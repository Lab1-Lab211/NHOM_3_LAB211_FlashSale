package view;

import controller.SimulatorController;
import service.SimulatorResult;

import java.util.List;

public class SimulatorView {
    private final SimulatorController simulatorController;
    private final ConsoleInput input;

    public SimulatorView(SimulatorController simulatorController, ConsoleInput input) {
        this.simulatorController = simulatorController;
        this.input = input;
    }

    public List<SimulatorResult> runInteractive() {
        String flashItemId = input.readLine("Nhap flashItemId can simulate: ").trim();
        int threadCount = input.readInt("Nhap so thread (100-500 de dung yeu cau T8): ");
        int quantity = input.readInt("Nhap so luong moi thread mua: ");

        List<SimulatorResult> results = simulatorController.runAll(flashItemId, threadCount, quantity);
        showResults(results);
        return results;
    }

    public void showResults(List<SimulatorResult> results) {
        System.out.println();
        System.out.println("=== SIMULATOR RESULT ===");
        System.out.printf("%-14s %8s %8s %8s %10s %12s %12s %12s %10s%n",
                "Mechanism", "Threads", "OK", "Fail", "Sold", "LostUpdate",
                "TPS", "AvgMs", "Status");
        for (SimulatorResult result : results) {
            System.out.printf("%-14s %8d %8d %8d %4d/%-5d %12d %12.2f %12.2f %10s%n",
                    result.getMechanism().name(),
                    result.getThreadCount(),
                    result.getSuccessCount(),
                    result.getFailCount(),
                    result.getFinalSoldQty(),
                    result.getLimitedQty(),
                    result.getLostUpdateQuantity(),
                    result.getThroughput(),
                    result.getAvgLatencyMs(),
                    result.hasRaceInconsistency() ? "RACE" : "OK");
        }
        System.out.println("Da ghi log vao data/transactions.csv");
    }
}
