from __future__ import annotations

from app.adapters.rhode_island import RhodeIslandDashboardAdapter
from app.adapters.utah import UtahDashboardAdapter
from app.config import Settings


def test_utah_dashboard_fixture_parses_live_bed_record() -> None:
    settings = Settings(import_fixture_mode=True)
    adapter = UtahDashboardAdapter(settings)
    html = (settings.fixture_dir / "utah_dashboard.html").read_text(encoding="utf-8")

    records = adapter.parse_fixture_html(html)

    assert len(records) == 1
    assert records[0].source_system == "utah"
    assert records[0].available_beds == 18
    assert records[0].total_beds == 60
    assert records[0].name == "Harbor House Indianapolis"


def test_rhode_island_dashboard_fixture_parses_veteran_record() -> None:
    settings = Settings(import_fixture_mode=True)
    adapter = RhodeIslandDashboardAdapter(settings)
    html = (settings.fixture_dir / "rhode_island_dashboard.html").read_text(encoding="utf-8")

    records = adapter.parse_fixture_html(html)

    assert len(records) == 1
    assert records[0].source_system == "ri"
    assert records[0].category == "veteran"
    assert records[0].available_beds == 4
    assert records[0].eligibility == ["Veterans"]
