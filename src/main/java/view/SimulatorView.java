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
        int availableCustomers = simulatorController.getAvailableCustomerCount();
        System.out.println("So customer co the dung de test: " + availableCustomers);
        int threadCount = input.readInt(
                "Nhap so thread (1-" + availableCustomers + ", moi thread = 1 customer): ");
        int quantity = input.readInt("Nhap so luong moi thread mua: ");

        List<SimulatorResult> results = simulatorController.runAll(flashItemId, threadCount, quantity);
        showResults(results);
        return results;
    }

    public void showResults(List<SimulatorResult> results) {
        System.out.println();
        System.out.println("=== SIMULATOR RESULT ===");
        System.out.printf("%-14s %13s %8s %8s %8s %10s %10s %10s %9s %10s %12s %10s%n",
                "Mechanism", "TotalTime(ms)", "Threads", "OK", "Fail", "Sold", "LostUpdate", "Oversold",
                "TPS", "vs SYNC", "Violation%", "Muc tieu");
        for (SimulatorResult result : results) {
            String comparison = result.getMechanism() == SimulatorResult.BASELINE_MECHANISM
                    ? "Baseline" : String.format("%+.1f%%", result.getVsBaselinePercent());
            System.out.printf("%-14s %13.2f %8d %8d %8d %4d/%-5d %10d %10d %9.2f %10s %11.2f%% %10s%n",
                    result.getMechanism().name(),
                    result.getDurationMs(),
                    result.getThreadCount(),
                    result.getSuccessCount(),
                    result.getFailCount(),
                    result.getFinalSoldQty(),
                    result.getLimitedQty(),
                    result.getLostUpdateQuantity(),
                    result.getOversoldQuantity(),
                    result.getThroughput(),
                    comparison,
                    result.getSafetyViolationRate(),
                    result.isTargetPassed() ? "PASS" : "FAIL");
        }
        System.out.println("Baseline hieu nang: SYNCHRONIZED (co dong bo va bao toan du lieu).");
        System.out.println("NO_LOCK chi de minh hoa toc do tho; co race condition thi van FAIL.");
        System.out.println("Da ghi log vao data/transactions.csv");
        System.out.println("Moi transaction co customerId de truy vet khach hang.");
        System.out.println("Da ghi tong hop vao data/simulation_results.csv");
    }
}
