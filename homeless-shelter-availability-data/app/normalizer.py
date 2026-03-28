from __future__ import annotations

import math
import re
import unicodedata
from datetime import datetime, timezone


def normalize_text(value: str | None) -> str:
    if not value:
        return ""
    normalized = unicodedata.normalize("NFD", value)
    normalized = "".join(char for char in normalized if unicodedata.category(char) != "Mn")
    normalized = re.sub(r"[^a-zA-Z0-9]+", " ", normalized)
    return normalized.strip().lower()


def slugify(*parts: str) -> str:
    text = "-".join(filter(None, (normalize_text(part).replace(" ", "-") for part in parts)))
    text = re.sub(r"-{2,}", "-", text).strip("-")
    return text or "shelter"


def distance_miles(lat1: float | None, lng1: float | None, lat2: float | None, lng2: float | None) -> float | None:
    if None in {lat1, lng1, lat2, lng2}:
        return None

    radius_miles = 3958.7613
    lat_distance = math.radians(lat2 - lat1)
    lng_distance = math.radians(lng2 - lng1)
    a = (
        math.sin(lat_distance / 2) ** 2
        + math.cos(math.radians(lat1))
        * math.cos(math.radians(lat2))
        * math.sin(lng_distance / 2) ** 2
    )
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    return radius_miles * c


def now_utc() -> datetime:
    return datetime.now(timezone.utc)
