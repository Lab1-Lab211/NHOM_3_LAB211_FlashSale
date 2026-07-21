import csv
import json
import statistics
from pathlib import Path


ROOT = Path(__file__).resolve().parent
RESULTS = ROOT / "results"
MECHANISMS = ["NO_LOCK", "FILE_LOCK", "SYNCHRONIZED", "OPTIMISTIC"]
NUMERIC_FIELDS = [
    "threads", "quantityPerThread", "limitedQty", "successCount", "failCount",
    "finalSoldQty", "lostUpdateQty", "explicitOversoldQty", "logicalNegativeQty",
    "negativeStockRatePct", "consistencyViolationRatePct", "totalTimeMs",
    "throughputTps", "avgLatencyMs", "vsSynchronizedPct",
]


def load_rows():
    rows = []
    for path in sorted(RESULTS.glob("run-*/experiment_results.csv")):
        with path.open(encoding="utf-8-sig", newline="") as stream:
            for row in csv.DictReader(stream):
                row["repeat"] = int(row["repeat"])
                for field in NUMERIC_FIELDS:
                    row[field] = float(row[field])
                row["targetPassed"] = row["targetPassed"].lower() == "true"
                rows.append(row)
    return rows


def mean(values):
    return statistics.fmean(values)


def sd(values):
    return statistics.stdev(values) if len(values) > 1 else 0.0


def f4(value):
    return f"{value:.4f}"


rows = load_rows()
if len(rows) != 12:
    raise SystemExit(f"Expected 12 rows, found {len(rows)}")

raw_path = ROOT / "raw_results_1000_threads_4_mechanisms_3_repeats.csv"
raw_fields = list(rows[0].keys())
with raw_path.open("w", encoding="utf-8-sig", newline="") as stream:
    writer = csv.DictWriter(stream, fieldnames=raw_fields)
    writer.writeheader()
    writer.writerows(rows)

by_mechanism = {
    mechanism: [row for row in rows if row["mechanism"] == mechanism]
    for mechanism in MECHANISMS
}
sync_mean_tps = mean([r["throughputTps"] for r in by_mechanism["SYNCHRONIZED"]])

summary = []
for mechanism in MECHANISMS:
    group = by_mechanism[mechanism]
    mean_tps = mean([r["throughputTps"] for r in group])
    mean_negative = mean([r["negativeStockRatePct"] for r in group])
    mean_consistency = mean([r["consistencyViolationRatePct"] for r in group])
    mean_success = mean([r["successCount"] for r in group])
    mean_sold = mean([r["finalSoldQty"] for r in group])
    limited = mean([r["limitedQty"] for r in group])
    vs_sync = (mean_tps - sync_mean_tps) * 100.0 / sync_mean_tps
    all_repeats_safe = all(
        r["negativeStockRatePct"] == 0.0
        and r["consistencyViolationRatePct"] == 0.0
        for r in group
    )
    rq_pass = all_repeats_safe and vs_sync >= -30.0
    summary.append({
        "mechanism": mechanism,
        "repeats": 3,
        "meanSuccessCount": f4(mean_success),
        "sdSuccessCount": f4(sd([r["successCount"] for r in group])),
        "meanFinalSoldQty": f4(mean_sold),
        "inventoryUtilizationPct": f4(mean_sold * 100.0 / limited),
        "meanTotalTimeMs": f4(mean([r["totalTimeMs"] for r in group])),
        "sdTotalTimeMs": f4(sd([r["totalTimeMs"] for r in group])),
        "meanThroughputTps": f4(mean_tps),
        "sdThroughputTps": f4(sd([r["throughputTps"] for r in group])),
        "meanAvgLatencyMs": f4(mean([r["avgLatencyMs"] for r in group])),
        "meanNegativeStockRatePct": f4(mean_negative),
        "meanConsistencyViolationRatePct": f4(mean_consistency),
        "vsSynchronizedMeanPct": f4(vs_sync),
        "allRepeatsSafe": str(all_repeats_safe).lower(),
        "rqPass": str(rq_pass).lower(),
    })

summary_path = ROOT / "aggregated_results.csv"
with summary_path.open("w", encoding="utf-8-sig", newline="") as stream:
    writer = csv.DictWriter(stream, fieldnames=list(summary[0].keys()))
    writer.writeheader()
    writer.writerows(summary)

analysis = {
    "experiment": {
        "itemId": "FSI-00167",
        "threads": 1000,
        "quantityPerThread": 1,
        "limitedQty": 96,
        "mechanisms": 4,
        "repeats": 3,
        "measuredRuns": 12,
        "baseline": "SYNCHRONIZED",
        "threshold": "0% negative stock AND 0% consistency violation AND mean TPS decline <= 30%",
    },
    "summary": summary,
    "rqPassingMechanisms": [row["mechanism"] for row in summary if row["rqPass"] == "true"],
    "recommendedMechanism": "SYNCHRONIZED",
    "recommendationRationale": (
        "SYNCHRONIZED is the safest default for this single-JVM CSV application: it used all 96 "
        "available units with zero inconsistency. OPTIMISTIC also passed but averaged about 20% lower "
        "throughput under extreme contention. FILE_LOCK passed the narrow two-metric threshold, but "
        "used only about 61% of inventory because many same-JVM lock attempts failed immediately."
    ),
}
(ROOT / "analysis_summary.json").write_text(
    json.dumps(analysis, ensure_ascii=False, indent=2), encoding="utf-8"
)

print(json.dumps(analysis, ensure_ascii=False, indent=2))
