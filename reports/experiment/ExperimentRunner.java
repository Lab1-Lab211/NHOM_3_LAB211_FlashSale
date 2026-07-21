import model.enums.LockMechanism;
import repository.CustomerRepository;
import repository.FlashSaleItemRepository;
import repository.OrderTransactionRepository;
import service.SimulatorResult;
import service.SimulatorService;

import java.io.BufferedWriter;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Reproducible, isolated runner for the 1000-thread concurrency experiment.
 * It copies production-shaped CSV inputs into a per-repeat directory so the
 * application's real data files are never mutated by the benchmark.
 */
public final class ExperimentRunner {
    private static final String ITEM_ID = "FSI-00167";
    private static final int THREADS = 1000;
    private static final int QUANTITY_PER_THREAD = 1;

    private ExperimentRunner() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: ExperimentRunner <repeatNo> <outputRoot>");
        }

        int repeat = Integer.parseInt(args[0]);
        Path projectRoot = Paths.get("").toAbsolutePath().normalize();
        Path outputRoot = Paths.get(args[1]).toAbsolutePath().normalize();
        Path runDir = outputRoot.resolve(String.format("run-%02d", repeat));
        Files.createDirectories(runDir);

        Path itemFile = runDir.resolve("flash_items.csv");
        Path customerFile = runDir.resolve("customers.csv");
        Path transactionFile = runDir.resolve("transactions.csv");
        Files.copy(projectRoot.resolve("data/flash_items.csv"), itemFile,
                StandardCopyOption.REPLACE_EXISTING);
        Files.copy(projectRoot.resolve("data/customers.csv"), customerFile,
                StandardCopyOption.REPLACE_EXISTING);
        Files.deleteIfExists(transactionFile);
        Files.deleteIfExists(runDir.resolve("simulation_results.csv"));

        SimulatorService simulator = new SimulatorService(
                new FlashSaleItemRepository(itemFile.toString()),
                new OrderTransactionRepository(transactionFile.toString()),
                new CustomerRepository(customerFile.toString()));

        long wallStart = System.nanoTime();
        List<SimulatorResult> results;
        try (PrintStream diagnosticLog = new PrintStream(
                Files.newOutputStream(runDir.resolve("diagnostics.log"),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING),
                true, StandardCharsets.UTF_8.name())) {
            PrintStream originalError = System.err;
            System.setErr(diagnosticLog);
            try {
                results = simulator.runAll(ITEM_ID, THREADS, QUANTITY_PER_THREAD);
            } finally {
                System.setErr(originalError);
            }
        }
        double wallSeconds = (System.nanoTime() - wallStart) / 1_000_000_000.0;

        Path resultFile = runDir.resolve("experiment_results.csv");
        try (BufferedWriter writer = Files.newBufferedWriter(resultFile, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write("repeat,timestamp,itemId,mechanism,threads,quantityPerThread,limitedQty,"
                    + "successCount,failCount,finalSoldQty,lostUpdateQty,explicitOversoldQty,"
                    + "logicalNegativeQty,negativeStockRatePct,consistencyViolationRatePct,"
                    + "totalTimeMs,throughputTps,avgLatencyMs,vsSynchronizedPct,targetPassed");
            writer.newLine();
            for (SimulatorResult result : results) {
                long successfulQty = result.getSuccessfulQuantity();
                long logicalNegativeQty = Math.max(0L, successfulQty - result.getLimitedQty());
                double negativeStockRate = successfulQty == 0L ? 0.0
                        : logicalNegativeQty * 100.0 / successfulQty;
                boolean targetPassed = negativeStockRate == 0.0
                        && !result.hasRaceInconsistency()
                        && result.getVsBaselinePercent() >= -30.0;

                writer.write(String.join(",",
                        Integer.toString(repeat),
                        LocalDateTime.now().toString(),
                        result.getFlashItemId(),
                        result.getMechanism().name(),
                        Integer.toString(result.getThreadCount()),
                        Integer.toString(result.getQuantityPerThread()),
                        Integer.toString(result.getLimitedQty()),
                        Long.toString(result.getSuccessCount()),
                        Long.toString(result.getFailCount()),
                        Integer.toString(result.getFinalSoldQty()),
                        Long.toString(result.getLostUpdateQuantity()),
                        Long.toString(result.getOversoldQuantity()),
                        Long.toString(logicalNegativeQty),
                        f4(negativeStockRate),
                        f4(result.getSafetyViolationRate()),
                        f4(result.getDurationMs()),
                        f4(result.getThroughput()),
                        f4(result.getAvgLatencyMs()),
                        f4(result.getVsBaselinePercent()),
                        Boolean.toString(targetPassed)));
                writer.newLine();

                System.out.printf(Locale.US,
                        "REPEAT=%d MECHANISM=%s OK=%d FAIL=%d SOLD=%d LOST=%d "
                                + "NEGATIVE_RATE=%.4f VIOLATION=%.4f TIME_MS=%.4f TPS=%.4f VS_SYNC=%.4f PASS=%s%n",
                        repeat, result.getMechanism(), result.getSuccessCount(),
                        result.getFailCount(), result.getFinalSoldQty(),
                        result.getLostUpdateQuantity(), negativeStockRate,
                        result.getSafetyViolationRate(), result.getDurationMs(),
                        result.getThroughput(), result.getVsBaselinePercent(), targetPassed);
            }
        }

        System.out.printf(Locale.US, "REPEAT=%d WALL_SECONDS=%.3f RESULT=%s%n",
                repeat, wallSeconds, resultFile);
    }

    private static String f4(double value) {
        return String.format(Locale.US, "%.4f", value);
    }
}
