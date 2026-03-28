from __future__ import annotations

import argparse
import asyncio
import json
from collections.abc import Sequence

from app.config import get_settings
from app.service import ImportOrchestrator


IMPORT_TARGETS: dict[str, list[tuple[str, str]]] = {
    "all": [
        ("full-country", "google"),
        ("source-import", "utah"),
        ("source-import", "ri"),
    ],
    "full-country": [("full-country", "google")],
    "google": [("source-import", "google")],
    "utah": [("source-import", "utah")],
    "ri": [("source-import", "ri")],
}


async def _run_target(target: str) -> int:
    orchestrator = ImportOrchestrator(get_settings())
    exit_code = 0

    for import_type, source_system in IMPORT_TARGETS[target]:
        run_id = orchestrator.enqueue(import_type, source_system)
        await orchestrator.run(run_id, import_type, source_system)
        run = orchestrator.get_run(run_id)
        print(json.dumps(run, indent=2, default=str))
        if run is None or run.get("status") != "completed":
            exit_code = 1

    return exit_code


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Run shelter imports and persist results into Postgres.")
    parser.add_argument(
        "target",
        choices=tuple(IMPORT_TARGETS),
        help="Import target to run. 'all' runs the full country import and both regional overlays.",
    )
    args = parser.parse_args(argv)
    return asyncio.run(_run_target(args.target))


if __name__ == "__main__":
    raise SystemExit(main())
