from __future__ import annotations

import json
import re
from datetime import datetime

from playwright.async_api import async_playwright

from app.config import Settings
from app.models import SourceShelterRecord


class UtahDashboardAdapter:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings

    async def collect_records(self) -> list[SourceShelterRecord]:
        if self.settings.import_fixture_mode:
            html = (self.settings.fixture_dir / "utah_dashboard.html").read_text(encoding="utf-8")
            return self.parse_fixture_html(html)

        async with async_playwright() as playwright:
            browser = await playwright.chromium.launch(headless=True)
            page = await browser.new_page()
            await page.goto(self.settings.utah_dashboard_url, wait_until="networkidle")
            html = await page.content()
            await browser.close()
        return self.parse_fixture_html(html)

    def parse_fixture_html(self, html: str) -> list[SourceShelterRecord]:
        match = re.search(
            r'<script[^>]+id="utah-bed-data"[^>]*>(?P<data>.*?)</script>',
            html,
            re.DOTALL | re.IGNORECASE,
        )
        if not match:
            return []

        rows = json.loads(match.group("data"))
        return [
            SourceShelterRecord(
                source_system="utah",
                external_id=row["external_id"],
                name=row["name"],
                address=row["address"],
                city=row["city"],
                state=row["state"],
                zip_code=row.get("zip_code"),
                phone_number=row.get("phone_number"),
                website=row.get("website"),
                latitude=row.get("latitude"),
                longitude=row.get("longitude"),
                hours=row.get("hours"),
                category=row.get("category", "general"),
                description=row.get("description"),
                services=row.get("services", []),
                eligibility=row.get("eligibility", []),
                available_beds=row.get("available_beds"),
                total_beds=row.get("total_beds"),
                last_source_updated_at=_parse_datetime(row.get("last_source_updated_at")),
                raw_payload={"fixture_row": row},
            )
            for row in rows
        ]


def _parse_datetime(value: str | None) -> datetime | None:
    if not value:
        return None
    return datetime.fromisoformat(value.replace("Z", "+00:00"))
