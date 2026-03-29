from __future__ import annotations

import contextlib
import json
import mimetypes
import threading
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import unquote, urlparse

try:
    from playwright.sync_api import sync_playwright
except ImportError as exc:  # pragma: no cover
    raise SystemExit(
        "Playwright for Python is required to generate frontend LCOV. "
        "Install it with `python -m pip install -r "
        "homeless-shelter-availability-data/requirements.txt`."
    ) from exc


WEB_ROOT = Path(__file__).resolve().parents[1]
REPO_ROOT = WEB_ROOT.parent
DIST_ROOT = WEB_ROOT / "dist"
ASSETS_ROOT = DIST_ROOT / "assets"
COVERAGE_ROOT = WEB_ROOT / "coverage"
LCOV_PATH = COVERAGE_ROOT / "lcov.info"
JS_ASSET_PATHS = sorted(path for path in ASSETS_ROOT.rglob("*.js") if path.is_file())

FIXTURE_SHELTERS = [
    {
        "id": "harbor-house-indianapolis-indianapolis-in",
        "slug": "harbor-house-indianapolis-indianapolis-in",
        "name": "Harbor House Indianapolis",
        "address": "245 Meridian Street",
        "city": "Indianapolis",
        "state": "IN",
        "zip": "46204",
        "phone": "317-555-0100",
        "website": "https://harbor.example.org",
        "coordinates": {"lat": 39.7684, "lng": -86.1581},
        "hours": "Open 24 hours",
        "category": "general",
        "services": ["Emergency shelter"],
        "eligibility": ["Adults"],
        "availableBeds": 18,
        "bedsAvailable": 18,
        "totalBeds": 60,
        "availabilityStatus": "available",
        "lastUpdated": "2026-03-28T13:12:00Z",
        "description": "Regional live-bed override fixture.",
    },
    {
        "id": "valor-house-columbus-columbus-oh",
        "slug": "valor-house-columbus-columbus-oh",
        "name": "Valor House Columbus",
        "address": "111 Veteran Way",
        "city": "Columbus",
        "state": "OH",
        "zip": "43065",
        "phone": "614-555-0100",
        "website": "https://valor.example.org",
        "coordinates": {"lat": 40.0992, "lng": -83.1141},
        "hours": "Intake until 10 PM",
        "category": "veteran",
        "services": ["Case management"],
        "eligibility": ["Veterans"],
        "availableBeds": 4,
        "bedsAvailable": 4,
        "totalBeds": 25,
        "availabilityStatus": "limited",
        "lastUpdated": "2026-03-28T14:00:00Z",
        "description": "Regional veteran shelter fixture.",
    },
]

FIXTURE_DETAILS = {shelter["slug"]: shelter for shelter in FIXTURE_SHELTERS}


class CoverageRequestHandler(BaseHTTPRequestHandler):
    def do_GET(self) -> None:  # noqa: N802
        parsed = urlparse(self.path)
        route = parsed.path

        if route == "/config.js":
            payload = {
                "GOOGLE_MAPS_API_KEY": "",
                "API_BASE_URL": self.server.base_url,  # type: ignore[attr-defined]
            }
            body = "window.__APP_CONFIG__ = " + json.dumps(payload, indent=2) + ";\n"
            self._write_response(body.encode("utf-8"), "application/javascript; charset=utf-8")
            return

        if route == "/api/shelters":
            self._write_json(FIXTURE_SHELTERS)
            return

        if route.startswith("/api/shelters/"):
            slug = route.removeprefix("/api/shelters/")
            shelter = FIXTURE_DETAILS.get(slug)
            if shelter is None:
                self.send_error(HTTPStatus.NOT_FOUND, f"Shelter '{slug}' was not found.")
                return
            self._write_json(shelter)
            return

        if route.startswith("/assets/"):
            self._serve_file(DIST_ROOT / route.lstrip("/"))
            return

        if route in {"/", "/find-help", "/resources"} or route.startswith("/shelters/"):
            self._serve_file(DIST_ROOT / "index.html")
            return

        candidate = DIST_ROOT / unquote(route.lstrip("/"))
        if candidate.exists() and candidate.is_file():
            self._serve_file(candidate)
            return

        self.send_error(HTTPStatus.NOT_FOUND, "Not Found")

    def log_message(self, format: str, *args: object) -> None:  # noqa: A003
        return

    def _serve_file(self, path: Path) -> None:
        if not path.exists() or not path.is_file():
            self.send_error(HTTPStatus.NOT_FOUND, "File not found")
            return

        content_type = mimetypes.guess_type(path.name)[0] or "application/octet-stream"
        self._write_response(path.read_bytes(), content_type)

    def _write_json(self, payload: object) -> None:
        self._write_response(
            json.dumps(payload).encode("utf-8"),
            "application/json; charset=utf-8",
        )

    def _write_response(self, body: bytes, content_type: str) -> None:
        self.send_response(HTTPStatus.OK)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


@contextlib.contextmanager
def serve_dist() -> str:
    httpd = ThreadingHTTPServer(("127.0.0.1", 0), CoverageRequestHandler)
    httpd.base_url = f"http://127.0.0.1:{httpd.server_port}"  # type: ignore[attr-defined]
    thread = threading.Thread(target=httpd.serve_forever, daemon=True)
    thread.start()
    try:
        yield httpd.base_url  # type: ignore[attr-defined]
    finally:
        httpd.shutdown()
        httpd.server_close()
        thread.join(timeout=5)


def merge_ranges(ranges: list[tuple[int, int]]) -> list[tuple[int, int]]:
    if not ranges:
        return []

    merged: list[list[int]] = [list(sorted(ranges)[0])]
    for start, end in sorted(ranges)[1:]:
        current = merged[-1]
        if start <= current[1]:
            current[1] = max(current[1], end)
        else:
            merged.append([start, end])
    return [(start, end) for start, end in merged]


def covered_lines(source: str, ranges: list[tuple[int, int]]) -> tuple[int, set[int]]:
    merged = merge_ranges(ranges)
    lines = source.splitlines(keepends=True)
    covered: set[int] = set()
    offset = 0

    for line_number, line in enumerate(lines, start=1):
        line_start = offset
        line_end = offset + len(line)
        if any(start < line_end and end > line_start for start, end in merged):
            covered.add(line_number)
        offset = line_end

    return len(lines), covered


def uncovered_lines(total_lines: int, executed_lines: set[int]) -> list[int]:
    return [line_number for line_number in range(1, total_lines + 1) if line_number not in executed_lines]


def format_line_ranges(line_numbers: list[int]) -> str:
    if not line_numbers:
        return "none"

    ranges: list[str] = []
    start = previous = line_numbers[0]

    for current in line_numbers[1:]:
        if current == previous + 1:
            previous = current
            continue
        ranges.append(f"{start}-{previous}" if start != previous else str(start))
        start = previous = current

    ranges.append(f"{start}-{previous}" if start != previous else str(start))
    return ", ".join(ranges)


def write_lcov(file_coverages: list[dict[str, object]]) -> None:
    COVERAGE_ROOT.mkdir(parents=True, exist_ok=True)

    with LCOV_PATH.open("w", encoding="utf-8", newline="\n") as handle:
        for file_coverage in file_coverages:
            source_path = str(file_coverage["source_path"])
            total_lines = int(file_coverage["total_lines"])
            executed_lines = set(file_coverage["executed_lines"])

            handle.write("TN:\n")
            handle.write(f"SF:{source_path}\n")
            for line_number in range(1, total_lines + 1):
                hit_count = 1 if line_number in executed_lines else 0
                handle.write(f"DA:{line_number},{hit_count}\n")
            handle.write(f"LF:{total_lines}\n")
            handle.write(f"LH:{len(executed_lines)}\n")
            handle.write("end_of_record\n")


def main() -> None:
    if not JS_ASSET_PATHS:
        raise SystemExit(f"No JavaScript assets were found in {ASSETS_ROOT}.")

    with serve_dist() as base_url:
        with sync_playwright() as playwright:
            browser = playwright.chromium.launch(headless=True)
            page = browser.new_page()
            page.route("https://fonts.googleapis.com/*", lambda route: route.abort())
            page.route("https://fonts.gstatic.com/*", lambda route: route.abort())

            session = page.context.new_cdp_session(page)
            session.send("Profiler.enable")
            session.send("Debugger.enable")
            session.send("Profiler.startPreciseCoverage", {"callCount": False, "detailed": True})

            page.goto(f"{base_url}/", wait_until="domcontentloaded")
            page.wait_for_selector("text=Shelter listings currently loaded")
            page.goto(f"{base_url}/find-help", wait_until="domcontentloaded")
            page.wait_for_selector("text=2 shelters match your filters")
            page.goto(
                f"{base_url}/shelters/{FIXTURE_SHELTERS[0]['slug']}",
                wait_until="domcontentloaded",
            )
            page.wait_for_selector(f"text={FIXTURE_SHELTERS[0]['name']}")
            page.goto(f"{base_url}/resources", wait_until="domcontentloaded")
            page.wait_for_selector("text=Support information that complements the search flow.")

            report = session.send("Profiler.takePreciseCoverage")["result"]
            session.send("Profiler.stopPreciseCoverage")
            browser.close()

    report_by_url = {entry.get("url"): entry for entry in report}
    file_coverages: list[dict[str, object]] = []

    for asset_path in JS_ASSET_PATHS:
        source = asset_path.read_text(encoding="utf-8")
        asset_url = f"{base_url}/{asset_path.relative_to(DIST_ROOT).as_posix()}"
        asset_entry = report_by_url.get(asset_url)
        executed_ranges = []
        if asset_entry is not None:
            executed_ranges = [
                (coverage_range["startOffset"], coverage_range["endOffset"])
                for function in asset_entry["functions"]
                for coverage_range in function["ranges"]
                if coverage_range.get("count", 0) > 0
            ]

        total_lines, executed_lines = covered_lines(source, executed_ranges)
        file_coverages.append(
            {
                "source_path": asset_path.relative_to(REPO_ROOT).as_posix(),
                "total_lines": total_lines,
                "executed_lines": executed_lines,
            }
        )

    write_lcov(file_coverages)

    total_lines = sum(int(file_coverage["total_lines"]) for file_coverage in file_coverages)
    covered_line_count = sum(len(set(file_coverage["executed_lines"])) for file_coverage in file_coverages)
    line_pct = (covered_line_count / total_lines * 100) if total_lines else 0.0
    print(f"Frontend LCOV written to {LCOV_PATH}")
    print(f"Overall JS line coverage: {covered_line_count}/{total_lines} ({line_pct:.2f}%)")
    print("Per-file coverage:")
    for file_coverage in file_coverages:
        source_path = str(file_coverage["source_path"])
        file_total_lines = int(file_coverage["total_lines"])
        file_executed_lines = set(file_coverage["executed_lines"])
        file_covered_line_count = len(file_executed_lines)
        file_line_pct = (file_covered_line_count / file_total_lines * 100) if file_total_lines else 0.0
        file_uncovered_lines = uncovered_lines(file_total_lines, file_executed_lines)
        print(
            f"- {source_path}: {file_covered_line_count}/{file_total_lines} "
            f"({file_line_pct:.2f}%)"
        )
        print(f"  uncovered lines: {format_line_ranges(file_uncovered_lines)}")


if __name__ == "__main__":
    main()
