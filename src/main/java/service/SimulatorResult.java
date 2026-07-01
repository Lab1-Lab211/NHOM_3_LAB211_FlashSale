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

    public long getSuccessfulQuantity() {
        return successCount * quantityPerThread;
    }

    public long getLostUpdateQuantity() {
        return Math.max(0, getSuccessfulQuantity() - finalSoldQty);
    }

    public boolean hasRaceInconsistency() {
        return getLostUpdateQuantity() > 0 || finalSoldQty > limitedQty;
    }
}
