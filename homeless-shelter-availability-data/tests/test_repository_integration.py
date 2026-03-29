from __future__ import annotations

import asyncio
from pathlib import Path

import psycopg
from testcontainers.postgres import PostgresContainer

from app.adapters.google_places import GooglePlacesAdapter
from app.config import Settings
from app.models import SourceShelterRecord
from app.repository import ShelterRepository


def test_repository_merges_source_records_and_tracks_import_runs() -> None:
    migration_path = (
        Path(__file__).resolve().parents[2]
        / "homeless-shelter-availability-api"
        / "src"
        / "main"
        / "resources"
        / "db"
        / "migration"
        / "V1__hybrid_shelter_schema.sql"
    )

    with PostgresContainer("postgres:15") as postgres:
        settings = Settings(database_url=_psycopg_database_url(postgres.get_connection_url()))
        _apply_migration(settings.database_url, migration_path.read_text(encoding="utf-8"))

        repository = ShelterRepository(settings)
        run_id = repository.create_import_run("source-import", "google", {"fixtureMode": True})
        repository.mark_running(run_id)

        google_record = SourceShelterRecord(
            source_system="google",
            external_id="google-1",
            name="Harbor House Indianapolis",
            address="245 Meridian Street",
            city="Indianapolis",
            state="IN",
            zip_code="46204",
            phone_number="317-555-0100",
            website="https://harbor.example.org",
            latitude=39.7684,
            longitude=-86.1581,
            hours="Open 24 hours",
            category="general",
            description="Google directory seed",
            total_beds=60,
            services=["Emergency shelter"],
        )
        summary_one = repository.merge_records([google_record])
        repository.mark_completed(run_id, summary_one, {"recordCount": 1})

        ri_record = SourceShelterRecord(
            source_system="ri",
            external_id="ri-1",
            name="Harbor House Indianapolis",
            address="245 Meridian Street",
            city="Indianapolis",
            state="IN",
            zip_code="46204",
            latitude=39.7684,
            longitude=-86.1581,
            category="general",
            available_beds=9,
            total_beds=60,
            services=["Case management"],
        )
        summary_two = repository.merge_records([ri_record])

        with psycopg.connect(settings.database_url) as connection, connection.cursor() as cursor:
            cursor.execute("SELECT COUNT(*) FROM shelters")
            shelter_count = cursor.fetchone()[0]
            cursor.execute("SELECT COUNT(*) FROM shelter_sources")
            source_count = cursor.fetchone()[0]
            cursor.execute("SELECT available_beds, total_beds, source_system FROM shelters")
            available_beds, total_beds, source_system = cursor.fetchone()

        assert summary_one.created_count == 1
        assert summary_two.updated_count == 1
        assert shelter_count == 1
        assert source_count == 2
        assert available_beds == 9
        assert total_beds == 60
        assert source_system == "ri"
        assert repository.get_import_run(run_id)["status"] == "completed"


def test_repository_imports_google_fixture_batch_with_unknown_beds() -> None:
    migration_path = (
        Path(__file__).resolve().parents[2]
        / "homeless-shelter-availability-api"
        / "src"
        / "main"
        / "resources"
        / "db"
        / "migration"
        / "V1__hybrid_shelter_schema.sql"
    )

    with PostgresContainer("postgres:15") as postgres:
        settings = Settings(
            database_url=_psycopg_database_url(postgres.get_connection_url()),
            import_fixture_mode=True,
        )
        _apply_migration(settings.database_url, migration_path.read_text(encoding="utf-8"))

        repository = ShelterRepository(settings)
        records = asyncio.run(GooglePlacesAdapter(settings).collect_records())
        summary = repository.merge_records(records)

        with psycopg.connect(settings.database_url) as connection, connection.cursor() as cursor:
            cursor.execute("SELECT COUNT(*) FROM shelters")
            shelter_count = cursor.fetchone()[0]
            cursor.execute("SELECT COUNT(*) FROM shelter_sources WHERE source_system = 'google'")
            google_source_count = cursor.fetchone()[0]
            cursor.execute(
                "SELECT COUNT(*) FROM shelters WHERE source_system = 'google' AND available_beds IS NULL AND total_beds IS NULL"
            )
            unknown_google_count = cursor.fetchone()[0]

        assert len(records) >= settings.google_minimum_record_count
        assert summary.created_count == len(records)
        assert shelter_count == len(records)
        assert google_source_count == len(records)
        assert unknown_google_count == len(records)


def _apply_migration(database_url: str, sql: str) -> None:
    with psycopg.connect(database_url, autocommit=True) as connection, connection.cursor() as cursor:
        cursor.execute(sql)


def _psycopg_database_url(database_url: str) -> str:
    return database_url.replace("postgresql+psycopg2://", "postgresql://", 1)
