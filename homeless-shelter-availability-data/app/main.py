from __future__ import annotations

import asyncio
from typing import Any

from fastapi import FastAPI, HTTPException

from app.config import get_settings
from app.service import ImportOrchestrator

settings = get_settings()
orchestrator = ImportOrchestrator(settings)
background_tasks: set[asyncio.Task] = set()

app = FastAPI(title="Shelter Import Worker", version="1.0.0")


def _schedule_background_run(run_id: str, import_type: str, source_system: str) -> None:
    task = asyncio.create_task(orchestrator.run(run_id, import_type, source_system))
    background_tasks.add(task)
    task.add_done_callback(background_tasks.discard)


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/imports/full-country")
async def import_full_country() -> dict[str, Any]:
    run_id = orchestrator.enqueue("full-country", "google")
    _schedule_background_run(run_id, "full-country", "google")
    return {"id": run_id, "status": "queued", "importType": "full-country", "sourceSystem": "google"}


@app.post("/imports/sources/{source_system}")
async def import_source(source_system: str) -> dict[str, Any]:
    if source_system not in {"google", "utah", "ri"}:
        raise HTTPException(status_code=404, detail=f"Unsupported source '{source_system}'.")

    run_id = orchestrator.enqueue("source-import", source_system)
    _schedule_background_run(run_id, "source-import", source_system)
    return {"id": run_id, "status": "queued", "importType": "source-import", "sourceSystem": source_system}


@app.get("/imports/{run_id}")
async def get_import_run(run_id: str) -> dict[str, Any]:
    run = orchestrator.get_run(run_id)
    if run is None:
        raise HTTPException(status_code=404, detail=f"Import run '{run_id}' was not found.")
    return run
