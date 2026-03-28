from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from typing import Any


@dataclass(slots=True)
class SourceShelterRecord:
    source_system: str
    external_id: str
    name: str
    address: str
    city: str
    state: str
    zip_code: str | None = None
    phone_number: str | None = None
    website: str | None = None
    latitude: float | None = None
    longitude: float | None = None
    hours: str | None = None
    category: str = "general"
    description: str | None = None
    services: list[str] = field(default_factory=list)
    eligibility: list[str] = field(default_factory=list)
    available_beds: int | None = None
    total_beds: int | None = None
    last_source_updated_at: datetime | None = None
    raw_payload: dict[str, Any] = field(default_factory=dict)


@dataclass(slots=True)
class ImportSummary:
    created_count: int = 0
    updated_count: int = 0
    skipped_count: int = 0
