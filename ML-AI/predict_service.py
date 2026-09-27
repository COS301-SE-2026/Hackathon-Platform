from __future__ import annotations
import os
import joblib
import pandas as pd
from fastapi import FastAPI
from pydantic import BaseModel

MODEL_PATH = os.getenv("AI_MODEL_PATH", "ai_usage_model.joblib")

bundle = joblib.load(MODEL_PATH)

model = bundle["model"]
feature_names = bundle["features"]
threshold = float(bundle["threshold"])
model_version = bundle["modelVersion"]

app = FastAPI(title="AI Assistance Detector", version="1.0")

class TelemetryFeaturesRequest(BaseModel):
    typedCharacters: int
    typingEdits: int
    typingBatchCount: int
    averageTypingIntervalMs: float
    pastedCharacters: int
    pasteEvents: int
    largePasteCount: int
    largestPasteCharacters: int
    largestPasteLines: int
    pasteFraction: float
    deletedCharacters: int
    deletionEdits: int
    reworkRatio: float
    focusLostCount: int
    focusGainedCount: int
    tabHiddenCount: int
    tabVisibleCount: int
    totalTabAwaySeconds: int
    maxTabAwaySeconds: int
    pasteAfterTabReturnCount: int
    largePasteAfterTabReturnCount: int
    runCount: int
    successfulRuns: int
    codeErrorRuns: int
    requestErrorRuns: int
    submitCount: int
    fileOpenedCount: int
    activeDurationSeconds: int

CAMEL_TO_MODEL = {
    "typedCharacters": "typed_characters",
    "typingEdits": "typing_edits",
    "typingBatchCount": "typing_batch_count",
    "averageTypingIntervalMs": "average_typing_interval_ms",
    "pastedCharacters": "pasted_characters",
    "pasteEvents": "paste_events",
    "largePasteCount": "large_paste_count",
    "largestPasteCharacters": "largest_paste_characters",
    "largestPasteLines": "largest_paste_lines",
    "pasteFraction": "paste_fraction",
    "deletedCharacters": "deleted_characters",
    "deletionEdits": "deletion_edits",
    "reworkRatio": "rework_ratio",
    "focusLostCount": "focus_lost_count",
    "focusGainedCount": "focus_gained_count",
    "tabHiddenCount": "tab_hidden_count",
    "tabVisibleCount": "tab_visible_count",
    "totalTabAwaySeconds": "total_tab_away_seconds",
    "maxTabAwaySeconds": "max_tab_away_seconds",
    "pasteAfterTabReturnCount": "paste_after_tab_return_count",
    "largePasteAfterTabReturnCount": "large_paste_after_tab_return_count",
    "runCount": "run_count",
    "successfulRuns": "successful_runs",
    "codeErrorRuns": "code_error_runs",
    "requestErrorRuns": "request_error_runs",
    "submitCount": "submit_count",
    "fileOpenedCount": "file_opened_count",
    "activeDurationSeconds": "active_duration_seconds",
}

@app.get("/health")
def health():
    return {
        "status": "UP",
        "modelVersion": model_version
    }

@app.post("/predict")
def predict(request: TelemetryFeaturesRequest):
    incoming = request.model_dump()
    model_values = {
        CAMEL_TO_MODEL[key]: value
        for key, value in incoming.items()
    }

    missing = [
        name
        for name in feature_names
        if name not in model_values
    ]

    if missing:
        raise ValueError(f"Missing model features: {missing}")

    row = pd.DataFrame(
        [
            {
                feature: model_values[feature]
                for feature in feature_names
            }
        ]
    )

    probability = float(model.predict_proba(row)[0][1])

    return {
        "modelVersion": model_version,
        "aiAssistanceLikelihood": probability,
        "aiAssistanceLikelihoodPercent" : round(probability * 100, 1),
        "reviewThreshold": threshold,
        "flaggedForReview": probability >= threshold
    }