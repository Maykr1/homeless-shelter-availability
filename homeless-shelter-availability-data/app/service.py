from __future__ import annotations

import traceback
from collections.abc import Awaitable, Callable
from typing import Any

from app.adapters.google_places import GooglePlacesAdapter
from app.adapters.rhode_island import RhodeIslandDashboardAdapter
from app.adapters.utah import UtahDashboardAdapter
from app.config import Settings
from app.models import SourceShelterRecord
from app.repository import ShelterRepository


class ImportOrchestrator:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self.repository = ShelterRepository(settings)
        self.google_adapter = GooglePlacesAdapter(settings)
        self.utah_adapter = UtahDashboardAdapter(settings)
        self.ri_adapter = RhodeIslandDashboardAdapter(settings)

    def enqueue(self, import_type: str, source_system: str) -> str:
        return self.repository.create_import_run(import_type, source_system, metadata={"fixtureMode": self.settings.import_fixture_mode})

    async def run(self, run_id: str, import_type: str, source_system: str) -> None:
        self.repository.mark_running(run_id)
        try:
            records = await self._load_records(import_type, source_system)
            metadata = self._run_metadata(import_type, source_system)
            metadata["recordCount"] = len(records)
            summary = self.repository.merge_records(records)
            self.repository.mark_completed(
                run_id,
                summary,
                metadata=metadata,
            )
        except Exception as exc:
            metadata = self._run_metadata(import_type, source_system)
            metadata["traceback"] = traceback.format_exc()
            self.repository.mark_failed(
                run_id,
                str(exc),
                metadata=metadata,
            )

    def get_run(self, run_id: str):
        return self.repository.get_import_run(run_id)

    async def _load_records(self, import_type: str, source_system: str) -> list[SourceShelterRecord]:
        if import_type == "full-country":
            return await self.google_adapter.collect_records()

        loaders: dict[str, Callable[[], Awaitable[list[SourceShelterRecord]]]] = {
            "google": self.google_adapter.collect_records,
            "utah": self.utah_adapter.collect_records,
            "ri": self.ri_adapter.collect_records,
        }
        try:
            return await loaders[source_system]()
        except KeyError as exc:
            raise ValueError(f"Unsupported source system '{source_system}'.") from exc

    def _run_metadata(self, import_type: str, source_system: str) -> dict[str, Any]:
        metadata: dict[str, Any] = {
            "fixtureMode": self.settings.import_fixture_mode,
            "importType": import_type,
            "sourceSystem": source_system,
        }
        if source_system == "google":
            metadata.update(self.google_adapter.last_collection_metadata)
        return metadata
