package service;

import model.enums.LockMechanism;

public class SimulatorResult {
    private final LockMechanism mechanism;
    private final String flashItemId;
    private final int threadCount;
    private final int quantityPerThread;
    private final long successCount;
    private final long failCount;
    private final int limitedQty;
    private final int finalSoldQty;
    private final double throughput;
    private final double avgLatencyMs;
    private final double durationMs;
    private double vsBaselinePercent;
    private boolean targetPassed;

    public SimulatorResult(LockMechanism mechanism, String flashItemId, int threadCount,
                           int quantityPerThread, long successCount, long failCount,
                           int limitedQty, int finalSoldQty, double throughput,
                           double avgLatencyMs, double durationMs) {
        this.mechanism = mechanism;
        this.flashItemId = flashItemId;
        this.threadCount = threadCount;
        this.quantityPerThread = quantityPerThread;
        this.successCount = successCount;
        this.failCount = failCount;
        this.limitedQty = limitedQty;
        this.finalSoldQty = finalSoldQty;
        this.throughput = throughput;
        this.avgLatencyMs = avgLatencyMs;
        this.durationMs = durationMs;
    }

    public LockMechanism getMechanism() { return mechanism; }
    public String getFlashItemId() { return flashItemId; }
    public int getThreadCount() { return threadCount; }
    public int getQuantityPerThread() { return quantityPerThread; }
    public long getSuccessCount() { return successCount; }
    public long getFailCount() { return failCount; }
    public int getLimitedQty() { return limitedQty; }
    public int getFinalSoldQty() { return finalSoldQty; }
    public double getThroughput() { return throughput; }
    public double getAvgLatencyMs() { return avgLatencyMs; }
    public double getDurationMs() { return durationMs; }
    public double getVsBaselinePercent() { return vsBaselinePercent; }
    public boolean isTargetPassed() { return targetPassed; }

    /** So sánh TPS với NO_LOCK và đánh giá mục tiêu an toàn + giảm không quá 30%. */
    public void compareWithBaseline(double baselineThroughput) {
        if (mechanism == LockMechanism.NO_LOCK) {
            vsBaselinePercent = 0.0;
        } else if (baselineThroughput <= 0.0) {
            vsBaselinePercent = -100.0;
        } else {
            vsBaselinePercent = (throughput - baselineThroughput) / baselineThroughput * 100.0;
        }
        targetPassed = !hasRaceInconsistency() && vsBaselinePercent >= -30.0;
    }

    public long getRequestedQuantity() {
        return (long) threadCount * quantityPerThread;
    }

    public long getSuccessfulQuantity() {
        return successCount * quantityPerThread;
    }

    public long getLostUpdateQuantity() {
        return Math.max(0, getSuccessfulQuantity() - finalSoldQty);
    }

    public long getOversoldQuantity() {
        return Math.max(0, finalSoldQty - limitedQty);
    }

    public double getSafetyViolationRate() {
        long successfulQuantity = getSuccessfulQuantity();
        if (successfulQuantity == 0) return 0.0;
        return (getLostUpdateQuantity() + getOversoldQuantity()) * 100.0
                / successfulQuantity;
    }

    public boolean hasRaceInconsistency() {
        return getLostUpdateQuantity() > 0 || getOversoldQuantity() > 0;
    }
}
