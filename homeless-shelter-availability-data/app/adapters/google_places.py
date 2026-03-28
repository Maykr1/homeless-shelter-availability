from __future__ import annotations

import asyncio
import json
import re

import httpx

from app.census import SearchSeed, load_census_place_seeds
from app.config import Settings
from app.models import SourceShelterRecord

ANIMAL_EXCLUSION_PATTERN = re.compile(r"\b(animal|humane|pet|dog|cat|rescue)\b", re.IGNORECASE)


class GooglePlacesAdapter:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings

    async def collect_records(self) -> list[SourceShelterRecord]:
        if self.settings.import_fixture_mode:
            return self._fixture_records()

        if not self.settings.google_places_api_key:
            raise RuntimeError("GOOGLE_PLACES_API_KEY is required for live Google directory imports.")

        seeds = await load_census_place_seeds(self.settings)
        seen_place_ids: set[str] = set()
        records: list[SourceShelterRecord] = []

        async with httpx.AsyncClient(timeout=30.0) as client:
            for seed in seeds:
                for query in self.settings.google_queries:
                    next_page_token: str | None = None
                    while True:
                        search_payload = {
                            "textQuery": query,
                            "locationBias": {
                                "circle": {
                                    "center": {"latitude": seed.lat, "longitude": seed.lng},
                                    "radius": self.settings.google_location_radius_meters,
                                }
                            },
                        }
                        if next_page_token:
                            search_payload["pageToken"] = next_page_token

                        search_response = await client.post(
                            self.settings.google_search_url,
                            headers=self._search_headers(),
                            json=search_payload,
                        )
                        search_response.raise_for_status()
                        search_data = search_response.json()

                        for place in search_data.get("places", []):
                            place_id = place.get("id")
                            if not place_id or place_id in seen_place_ids or self._is_excluded(place):
                                continue

                            details = await self._place_details(client, place_id)
                            record = self._to_record(seed, query, place, details)
                            if record:
                                records.append(record)
                                seen_place_ids.add(place_id)

                        next_page_token = search_data.get("nextPageToken")
                        if not next_page_token:
                            break
                        await asyncio.sleep(2)

        return records

    def _fixture_records(self) -> list[SourceShelterRecord]:
        fixture = json.loads((self.settings.fixture_dir / "google_places_fixture.json").read_text(encoding="utf-8"))
        detail_lookup = fixture["detail_responses"]
        records: list[SourceShelterRecord] = []
        seed = SearchSeed(label="Fixture seed", state="IN", lat=39.7684, lng=-86.1581)

        for place in fixture["search_responses"]["places"]:
            details = detail_lookup[place["id"]]
            record = self._to_record(seed, "homeless shelters", place, details)
            if record:
                records.append(record)

        return records

    async def _place_details(self, client: httpx.AsyncClient, place_id: str) -> dict:
        response = await client.get(
            f"{self.settings.google_details_base_url}/{place_id}",
            headers=self._details_headers(),
        )
        response.raise_for_status()
        return response.json()

    def _search_headers(self) -> dict[str, str]:
        return {
            "X-Goog-Api-Key": self.settings.google_places_api_key,
            "X-Goog-FieldMask": ",".join(
                [
                    "places.id",
                    "places.displayName",
                    "places.formattedAddress",
                    "places.location",
                    "places.primaryType",
                    "places.types",
                    "nextPageToken",
                ]
            ),
            "Content-Type": "application/json",
        }

    def _details_headers(self) -> dict[str, str]:
        return {
            "X-Goog-Api-Key": self.settings.google_places_api_key,
            "X-Goog-FieldMask": ",".join(
                [
                    "id",
                    "displayName",
                    "formattedAddress",
                    "location",
                    "nationalPhoneNumber",
                    "websiteUri",
                    "regularOpeningHours.weekdayDescriptions",
                    "types",
                ]
            ),
        }

    def _is_excluded(self, place: dict) -> bool:
        name = place.get("displayName", {}).get("text", "")
        return bool(ANIMAL_EXCLUSION_PATTERN.search(name))

    def _to_record(
        self,
        seed: SearchSeed,
        query: str,
        place: dict,
        details: dict,
    ) -> SourceShelterRecord | None:
        display_name = details.get("displayName", {}).get("text") or place.get("displayName", {}).get("text")
        formatted_address = details.get("formattedAddress") or place.get("formattedAddress")
        if not display_name or not formatted_address:
            return None

        address, city, state, zip_code = self._split_address(formatted_address)
        if not city or not state:
            return None

        location = details.get("location") or place.get("location") or {}
        weekday_descriptions = details.get("regularOpeningHours", {}).get("weekdayDescriptions", [])
        veteran_focus = self._is_veteran_listing(display_name, formatted_address)

        return SourceShelterRecord(
            source_system="google",
            external_id=place["id"],
            name=display_name,
            address=address,
            city=city,
            state=state,
            zip_code=zip_code,
            phone_number=details.get("nationalPhoneNumber"),
            website=details.get("websiteUri"),
            latitude=location.get("latitude"),
            longitude=location.get("longitude"),
            hours="; ".join(weekday_descriptions) if weekday_descriptions else None,
            category="veteran" if veteran_focus else "general",
            description=f"Imported from Google Places query '{query}' near {seed.label}.",
            services=[],
            eligibility=["Veterans"] if veteran_focus else [],
            raw_payload={
                "seed": seed.label,
                "query": query,
                "search_result": place,
                "details": details,
            },
        )

    def _is_veteran_listing(self, display_name: str, formatted_address: str) -> bool:
        searchable = f"{display_name} {formatted_address}".lower()
        return "veteran" in searchable or "veterans" in searchable

    def _split_address(self, formatted_address: str) -> tuple[str, str | None, str | None, str | None]:
        parts = [part.strip() for part in formatted_address.split(",")]
        if len(parts) < 3:
            return formatted_address, None, None, None

        street = parts[0]
        city = parts[-3] if len(parts) >= 4 else parts[1]
        state_zip = parts[-2] if len(parts) >= 3 else ""
        match = re.match(r"([A-Z]{2})\s+(\d{5}(?:-\d{4})?)", state_zip)
        if not match:
            return street, city, None, None
        return street, city, match.group(1), match.group(2)
