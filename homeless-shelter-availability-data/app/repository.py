from __future__ import annotations

import uuid
from datetime import datetime
from typing import Any

import psycopg
from psycopg.rows import dict_row
from psycopg.types.json import Json

from app.config import Settings
from app.models import ImportSummary, SourceShelterRecord
from app.normalizer import distance_miles, normalize_text, now_utc, slugify

SOURCE_PRIORITY = {"google": 10, "utah": 100, "ri": 100}


class ShelterRepository:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings

    def create_import_run(self, import_type: str, source_system: str, metadata: dict[str, Any] | None = None) -> str:
        run_id = str(uuid.uuid4())
        with self._connect() as connection, connection.cursor() as cursor:
            cursor.execute(
                """
                INSERT INTO import_runs (id, import_type, source_system, status, metadata)
                VALUES (%s, %s, %s, 'queued', %s)
                """,
                (run_id, import_type, source_system, Json(metadata or {})),
            )
        return run_id

    def mark_running(self, run_id: str) -> None:
        with self._connect() as connection, connection.cursor() as cursor:
            cursor.execute(
                """
                UPDATE import_runs
                SET status = 'running', started_at = NOW()
                WHERE id = %s
                """,
                (run_id,),
            )

    def mark_completed(self, run_id: str, summary: ImportSummary, metadata: dict[str, Any] | None = None) -> None:
        with self._connect() as connection, connection.cursor() as cursor:
            cursor.execute(
                """
                UPDATE import_runs
                SET status = 'completed',
                    completed_at = NOW(),
                    created_count = %s,
                    updated_count = %s,
                    skipped_count = %s,
                    metadata = %s
                WHERE id = %s
                """,
                (
                    summary.created_count,
                    summary.updated_count,
                    summary.skipped_count,
                    Json(metadata or {}),
                    run_id,
                ),
            )

    def mark_failed(self, run_id: str, error_text: str, metadata: dict[str, Any] | None = None) -> None:
        with self._connect() as connection, connection.cursor() as cursor:
            cursor.execute(
                """
                UPDATE import_runs
                SET status = 'failed',
                    completed_at = NOW(),
                    error_text = %s,
                    metadata = %s
                WHERE id = %s
                """,
                (error_text, Json(metadata or {}), run_id),
            )

    def get_import_run(self, run_id: str) -> dict[str, Any] | None:
        with self._connect() as connection, connection.cursor(row_factory=dict_row) as cursor:
            cursor.execute("SELECT * FROM import_runs WHERE id = %s", (run_id,))
            row = cursor.fetchone()
        if not row:
            return None
        return self._serialize_row(row)

    def merge_records(self, records: list[SourceShelterRecord]) -> ImportSummary:
        summary = ImportSummary()
        with self._connect() as connection, connection.cursor(row_factory=dict_row) as cursor:
            for record in records:
                outcome = self._merge_record(cursor, record)
                if outcome == "created":
                    summary.created_count += 1
                elif outcome == "updated":
                    summary.updated_count += 1
                else:
                    summary.skipped_count += 1
        return summary

    def _merge_record(self, cursor: psycopg.Cursor, record: SourceShelterRecord) -> str:
        current = self._find_match(cursor, record)
        if current is None:
            shelter_id = self._insert_shelter(cursor, record)
            self._upsert_source_record(cursor, shelter_id, record)
            return "created"

        updated_payload = self._merged_shelter_values(current, record)
        if updated_payload:
            assignments = ", ".join(f"{column} = %s" for column in updated_payload)
            cursor.execute(
                f"UPDATE shelters SET {assignments} WHERE id = %s",
                (*updated_payload.values(), current["id"]),
            )
            outcome = "updated"
        else:
            outcome = "skipped"

        self._upsert_source_record(cursor, current["id"], record)
        return outcome

    def _find_match(self, cursor: psycopg.Cursor, record: SourceShelterRecord) -> dict[str, Any] | None:
        cursor.execute(
            """
            SELECT shelters.*
            FROM shelter_sources
            JOIN shelters ON shelters.id = shelter_sources.shelter_id
            WHERE shelter_sources.source_system = %s
              AND shelter_sources.external_id = %s
            """,
            (record.source_system, record.external_id),
        )
        existing = cursor.fetchone()
        if existing:
            return existing

        normalized_address = normalize_text(record.address)
        normalized_name = normalize_text(record.name)
        cursor.execute(
            """
            SELECT *
            FROM shelters
            WHERE normalized_address = %s
              AND state = %s
              AND city = %s
            ORDER BY id
            LIMIT 5
            """,
            (normalized_address, record.state, record.city),
        )
        exact_address = cursor.fetchone()
        if exact_address:
            return exact_address

        cursor.execute(
            """
            SELECT *
            FROM shelters
            WHERE normalized_name = %s
              AND state = %s
              AND city = %s
            ORDER BY id
            LIMIT 5
            """,
            (normalized_name, record.state, record.city),
        )
        candidates = cursor.fetchall()
        for candidate in candidates:
            candidate_distance = distance_miles(
                candidate.get("latitude"),
                candidate.get("longitude"),
                record.latitude,
                record.longitude,
            )
            if candidate_distance is not None and candidate_distance <= 0.5:
                return candidate

        return None

    def _insert_shelter(self, cursor: psycopg.Cursor, record: SourceShelterRecord) -> int:
        slug = self._next_slug(cursor, slugify(record.name, record.city, record.state))
        cursor.execute(
            """
            INSERT INTO shelters (
                slug, name, address, city, state, zip_code, phone_number, website,
                latitude, longitude, hours, category, description, services, eligibility,
                available_beds, total_beds, last_source_updated_at, source_system,
                source_external_id, normalized_name, normalized_address
            )
            VALUES (
                %s, %s, %s, %s, %s, %s, %s, %s,
                %s, %s, %s, %s, %s, %s, %s,
                %s, %s, %s, %s,
                %s, %s, %s
            )
            RETURNING id
            """,
            (
                slug,
                record.name,
                record.address,
                record.city,
                record.state,
                record.zip_code,
                record.phone_number,
                record.website,
                record.latitude,
                record.longitude,
                record.hours,
                record.category,
                record.description,
                Json(record.services),
                Json(record.eligibility),
                record.available_beds,
                record.total_beds,
                record.last_source_updated_at,
                record.source_system,
                record.external_id,
                normalize_text(record.name),
                normalize_text(record.address),
            ),
        )
        return int(cursor.fetchone()["id"])

    def _merged_shelter_values(self, current: dict[str, Any], record: SourceShelterRecord) -> dict[str, Any]:
        incoming_priority = SOURCE_PRIORITY.get(record.source_system, 0)
        current_priority = SOURCE_PRIORITY.get(current.get("source_system"), 0)
        can_override = incoming_priority >= current_priority

        merged = {
            "name": _choose_scalar(current.get("name"), record.name, can_override),
            "address": _choose_scalar(current.get("address"), record.address, can_override),
            "city": _choose_scalar(current.get("city"), record.city, can_override),
            "state": _choose_scalar(current.get("state"), record.state, can_override),
            "zip_code": _choose_scalar(current.get("zip_code"), record.zip_code, can_override),
            "phone_number": _choose_scalar(current.get("phone_number"), record.phone_number, can_override),
            "website": _choose_scalar(current.get("website"), record.website, can_override),
            "latitude": _choose_scalar(current.get("latitude"), record.latitude, can_override),
            "longitude": _choose_scalar(current.get("longitude"), record.longitude, can_override),
            "hours": _choose_scalar(current.get("hours"), record.hours, can_override),
            "category": _choose_scalar(current.get("category"), record.category, can_override),
            "description": _choose_scalar(current.get("description"), record.description, can_override),
            "services": _choose_list(current.get("services"), record.services, can_override),
            "eligibility": _choose_list(current.get("eligibility"), record.eligibility, can_override),
            "available_beds": current.get("available_beds"),
            "total_beds": current.get("total_beds"),
            "last_source_updated_at": current.get("last_source_updated_at"),
            "source_system": current.get("source_system"),
            "source_external_id": current.get("source_external_id"),
        }

        if record.available_beds is not None and (current.get("available_beds") is None or can_override):
            merged["available_beds"] = record.available_beds
            merged["source_system"] = record.source_system
            merged["source_external_id"] = record.external_id
            merged["last_source_updated_at"] = record.last_source_updated_at or now_utc()

        if record.total_beds is not None and (current.get("total_beds") is None or can_override):
            merged["total_beds"] = record.total_beds

        merged["normalized_name"] = normalize_text(merged["name"])
        merged["normalized_address"] = normalize_text(merged["address"])

        changed = {}
        for column, value in merged.items():
            comparable_value = value
            current_value = current.get(column)
            if column in {"services", "eligibility"}:
                comparable_value = list(value or [])
                current_value = list(current_value or [])
            if comparable_value != current_value:
                changed[column] = Json(comparable_value) if column in {"services", "eligibility"} else comparable_value
        return changed

    def _upsert_source_record(self, cursor: psycopg.Cursor, shelter_id: int, record: SourceShelterRecord) -> None:
        cursor.execute(
            """
            INSERT INTO shelter_sources (
                shelter_id, source_system, external_id, name, address, city, state, zip_code,
                phone_number, website, latitude, longitude, hours, category, description,
                services, eligibility, available_beds, total_beds, normalized_name,
                normalized_address, last_source_updated_at, raw_payload
            )
            VALUES (
                %s, %s, %s, %s, %s, %s, %s, %s,
                %s, %s, %s, %s, %s, %s, %s,
                %s, %s, %s, %s, %s,
                %s, %s, %s
            )
            ON CONFLICT (source_system, external_id)
            DO UPDATE SET
                shelter_id = EXCLUDED.shelter_id,
                name = EXCLUDED.name,
                address = EXCLUDED.address,
                city = EXCLUDED.city,
                state = EXCLUDED.state,
                zip_code = EXCLUDED.zip_code,
                phone_number = EXCLUDED.phone_number,
                website = EXCLUDED.website,
                latitude = EXCLUDED.latitude,
                longitude = EXCLUDED.longitude,
                hours = EXCLUDED.hours,
                category = EXCLUDED.category,
                description = EXCLUDED.description,
                services = EXCLUDED.services,
                eligibility = EXCLUDED.eligibility,
                available_beds = EXCLUDED.available_beds,
                total_beds = EXCLUDED.total_beds,
                normalized_name = EXCLUDED.normalized_name,
                normalized_address = EXCLUDED.normalized_address,
                last_source_updated_at = EXCLUDED.last_source_updated_at,
                last_seen_at = NOW(),
                raw_payload = EXCLUDED.raw_payload
            """,
            (
                shelter_id,
                record.source_system,
                record.external_id,
                record.name,
                record.address,
                record.city,
                record.state,
                record.zip_code,
                record.phone_number,
                record.website,
                record.latitude,
                record.longitude,
                record.hours,
                record.category,
                record.description,
                Json(record.services),
                Json(record.eligibility),
                record.available_beds,
                record.total_beds,
                normalize_text(record.name),
                normalize_text(record.address),
                record.last_source_updated_at,
                Json(record.raw_payload),
            ),
        )

    def _next_slug(self, cursor: psycopg.Cursor, base_slug: str) -> str:
        slug = base_slug
        suffix = 2
        while True:
            cursor.execute("SELECT 1 FROM shelters WHERE slug = %s", (slug,))
            if cursor.fetchone() is None:
                return slug
            slug = f"{base_slug}-{suffix}"
            suffix += 1

    def _connect(self) -> psycopg.Connection:
        return psycopg.connect(self.settings.database_url, row_factory=dict_row)

    def _serialize_row(self, row: dict[str, Any]) -> dict[str, Any]:
        serialized = {}
        for key, value in row.items():
            serialized[key] = value.isoformat() if isinstance(value, datetime) else value
        return serialized


def _choose_scalar(current: Any, incoming: Any, can_override: bool) -> Any:
    if incoming in {None, ""}:
        return current
    if current in {None, ""} or can_override:
        return incoming
    return current


def _choose_list(current: list[str] | None, incoming: list[str] | None, can_override: bool) -> list[str]:
    incoming = incoming or []
    current = current or []
    if not incoming:
        return current
    if not current or can_override:
        return incoming
    return current
