from __future__ import annotations

import asyncio

from app.adapters.google_places import GooglePlacesAdapter
from app.config import Settings
from app.google_seeds import SearchSeed


def test_google_places_fixture_maps_to_source_records() -> None:
    settings = Settings(import_fixture_mode=True)
    adapter = GooglePlacesAdapter(settings)

    records = asyncio.run(adapter.collect_records())

    assert len(records) >= settings.google_minimum_record_count
    assert records[0].source_system == "google"
    assert records[0].external_id == "fixture-google-1"
    assert records[0].name == "New York Homeless Shelter"
    assert records[0].city == "New York"
    assert records[0].state == "NY"
    assert records[0].phone_number == "(201) 555-1001"
    assert records[0].available_beds is None
    assert records[0].total_beds is None
    assert records[2].category == "veteran"
    assert adapter.last_collection_metadata["recordCount"] == len(records)
    assert adapter.last_collection_metadata["minimumRecordCount"] == settings.google_minimum_record_count


def test_google_places_filter_keeps_related_shelter_locations() -> None:
    settings = Settings(google_queries=("drop-in center", "warming center", "homeless shelter"))
    adapter = GooglePlacesAdapter(settings)
    place = {
        "displayName": {"text": "Downtown Drop-In Center"},
        "formattedAddress": "100 Main Street, Indianapolis, IN 46204, USA",
    }
    details = {
        "displayName": {"text": "Emergency Warming Center"},
        "formattedAddress": "100 Main Street, Indianapolis, IN 46204, USA",
    }

    assert adapter._is_excluded("drop-in center", place, details) is False
    assert adapter._is_excluded("warming center", place, details) is False


def test_google_places_filter_excludes_animal_and_non_shelter_results() -> None:
    settings = Settings(google_queries=("homeless shelter",))
    adapter = GooglePlacesAdapter(settings)
    animal_place = {
        "displayName": {"text": "County Animal Shelter"},
        "formattedAddress": "10 Main Street, Indianapolis, IN 46204, USA",
    }
    pantry_place = {
        "displayName": {"text": "Downtown Food Pantry"},
        "formattedAddress": "11 Main Street, Indianapolis, IN 46204, USA",
    }

    assert adapter._is_excluded("homeless shelter", animal_place) is True
    assert adapter._is_excluded("homeless shelter", pantry_place) is True


def test_google_places_filter_does_not_exclude_rescue_mission() -> None:
    settings = Settings(google_queries=("homeless shelter",))
    adapter = GooglePlacesAdapter(settings)
    place = {
        "displayName": {"text": "City Rescue Mission"},
        "formattedAddress": "12 Main Street, Indianapolis, IN 46204, USA",
    }

    assert adapter._is_excluded("homeless shelter", place) is False


def test_google_places_to_record_keeps_unknown_beds() -> None:
    adapter = GooglePlacesAdapter(Settings())
    seed = SearchSeed(label="Indianapolis", state="IN", lat=39.7684, lng=-86.1581)
    place = {
        "id": "google-1",
        "displayName": {"text": "Harbor House"},
        "formattedAddress": "245 Meridian Street, Indianapolis, IN 46204, USA",
        "location": {"latitude": 39.7684, "longitude": -86.1581},
    }
    details = {
        "displayName": {"text": "Harbor House"},
        "formattedAddress": "245 Meridian Street, Indianapolis, IN 46204, USA",
        "location": {"latitude": 39.7684, "longitude": -86.1581},
        "nationalPhoneNumber": "(317) 555-0100",
        "websiteUri": "https://harbor.example.org",
        "regularOpeningHours": {"weekdayDescriptions": ["Monday: Open 24 hours"]},
    }

    record = adapter._to_record(seed, "homeless shelter", place, details)

    assert record is not None
    assert record.available_beds is None
    assert record.total_beds is None
