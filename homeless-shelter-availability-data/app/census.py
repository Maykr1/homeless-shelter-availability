from __future__ import annotations

import csv
import io
import zipfile
from dataclasses import dataclass

import httpx

from app.config import Settings


@dataclass(frozen=True)
class SearchSeed:
    label: str
    state: str
    lat: float
    lng: float


async def load_census_place_seeds(settings: Settings) -> list[SearchSeed]:
    async with httpx.AsyncClient(timeout=60.0) as client:
        response = await client.get(settings.census_gazetteer_url)
        response.raise_for_status()

    with zipfile.ZipFile(io.BytesIO(response.content)) as archive:
        member_name = next(name for name in archive.namelist() if name.endswith(".txt"))
        with archive.open(member_name) as handle:
            content = io.TextIOWrapper(handle, encoding="utf-8")
            reader = csv.DictReader(content, delimiter="\t")
            seeds: list[SearchSeed] = []
            for row in reader:
                try:
                    seeds.append(
                        SearchSeed(
                            label=row.get("NAME", "unknown"),
                            state=row.get("USPS", ""),
                            lat=float(row["INTPTLAT"]),
                            lng=float(row["INTPTLONG"]),
                        )
                    )
                except (KeyError, TypeError, ValueError):
                    continue

    if settings.google_full_country_max_seeds:
        return seeds[: settings.google_full_country_max_seeds]
    return seeds
