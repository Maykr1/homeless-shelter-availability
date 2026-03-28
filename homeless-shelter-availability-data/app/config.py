from __future__ import annotations

import os
from dataclasses import dataclass, field
from pathlib import Path


def _env_bool(name: str, default: bool = False) -> bool:
    value = os.getenv(name)
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "on"}


def _env_int(name: str, default: int | None = None) -> int | None:
    value = os.getenv(name)
    if value is None or value == "":
        return default
    return int(value)


@dataclass(frozen=True)
class Settings:
    database_url: str = os.getenv(
        "DATABASE_URL",
        "postgresql://postgres:ZkY6tG5lp@db:5432/HSADB",
    )
    google_places_api_key: str = os.getenv("GOOGLE_PLACES_API_KEY") or os.getenv("GOOGLE_MAPS_API_KEY", "")
    import_fixture_mode: bool = _env_bool("IMPORT_FIXTURE_MODE", False)
    google_full_country_max_seeds: int | None = _env_int("GOOGLE_FULL_COUNTRY_MAX_SEEDS", 250)
    google_location_radius_meters: int = _env_int("GOOGLE_LOCATION_RADIUS_METERS", 20_000) or 20_000
    google_queries: tuple[str, ...] = ("homeless shelters", "family shelters")
    google_search_url: str = "https://places.googleapis.com/v1/places:searchText"
    google_details_base_url: str = "https://places.googleapis.com/v1/places"
    census_gazetteer_url: str = os.getenv(
        "CENSUS_GAZETTEER_URL",
        "https://www2.census.gov/geo/docs/maps-data/data/gazetteer/2025_Gazetteer/2025_Gaz_place_national.zip",
    )
    utah_dashboard_url: str = "https://jobs.utah.gov/homelessness/hbeds.html"
    rhode_island_dashboard_url: str = "https://housing.ri.gov/data-reports/emergency-shelter-data-dashboard"
    fixture_dir: Path = field(default_factory=lambda: Path(__file__).resolve().parent / "fixtures")


def get_settings() -> Settings:
    return Settings()
