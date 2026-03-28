from __future__ import annotations

import asyncio

from app.adapters.google_places import GooglePlacesAdapter
from app.config import Settings


def test_google_places_fixture_maps_to_source_records() -> None:
    adapter = GooglePlacesAdapter(Settings(import_fixture_mode=True))

    records = asyncio.run(adapter.collect_records())

    assert len(records) == 2
    assert records[0].source_system == "google"
    assert records[0].external_id == "fixture-google-1"
    assert records[0].name == "Harbor House Indianapolis"
    assert records[0].city == "Indianapolis"
    assert records[0].state == "IN"
    assert records[0].phone_number == "(317) 555-0100"
    assert records[1].category == "veteran"
