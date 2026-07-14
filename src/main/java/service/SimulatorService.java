package service;

import model.Customer;
import model.FlashSaleItem;
import model.OrderTransaction;
import model.enums.LockMechanism;
import repository.FlashSaleItemRepository;
import repository.CustomerRepository;
import repository.OrderTransactionRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SimulatorService {
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    private final FlashSaleItemRepository flashSaleItemRepository;
    private final OrderTransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;

    public SimulatorService(FlashSaleItemRepository flashSaleItemRepository,
                            OrderTransactionRepository transactionRepository,
                            CustomerRepository customerRepository) {
        this.flashSaleItemRepository = flashSaleItemRepository;
        this.transactionRepository = transactionRepository;
        this.customerRepository = customerRepository;
    }

    public List<SimulatorResult> runAll(String flashItemId, int threadCount, int quantityPerThread) {
        validateInput(flashItemId, threadCount, quantityPerThread);
        List<Customer> selectedCustomers = selectCustomers(threadCount);
        transactionRepository.clearAll();

        List<SimulatorResult> results = new ArrayList<>();
        for (LockMechanism mechanism : LockMechanism.values()) {
            results.add(runSingle(flashItemId, quantityPerThread, mechanism, selectedCustomers));
        }
        double baselineThroughput = results.stream()
                .filter(r -> r.getMechanism() == LockMechanism.NO_LOCK)
                .mapToDouble(SimulatorResult::getThroughput)
                .findFirst()
                .orElse(0.0);
        for (SimulatorResult result : results) {
            result.compareWithBaseline(baselineThroughput);
        }
        appendSummary(results);
        return results;
    }

    public SimulatorResult runSingle(String flashItemId, int threadCount, int quantityPerThread,
                                     LockMechanism mechanism) {
        validateInput(flashItemId, threadCount, quantityPerThread);
        List<Customer> selectedCustomers = selectCustomers(threadCount);
        return runSingle(flashItemId, quantityPerThread, mechanism, selectedCustomers);
    }

    private SimulatorResult runSingle(String flashItemId, int quantityPerThread,
                                      LockMechanism mechanism,
                                      List<Customer> selectedCustomers) {
        int threadCount = selectedCustomers.size();
        if (mechanism == null) {
            mechanism = LockMechanism.NO_LOCK;
        }

        FlashSaleItem before = flashSaleItemRepository.findById(flashItemId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay flashItemId: " + flashItemId));
        int limitedQty = before.getLimitedQty();

        flashSaleItemRepository.resetAllSoldQty();

        CountDownLatch readyGate = new CountDownLatch(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<OrderTransaction> transactions = Collections.synchronizedList(new ArrayList<OrderTransaction>());

        for (int i = 1; i <= threadCount; i++) {
            final int index = i;
            final Customer customer = selectedCustomers.get(i - 1);
            final LockMechanism selectedMechanism = mechanism;
            executor.submit(() -> {
                readyGate.countDown();
                String orderId = "-";
                boolean success = false;
                String errorMessage = "";
                long start = 0L;
                long end = 0L;
                try {
                    startGate.await();
                    start = System.nanoTime();
                    sellByMechanism(flashItemId, quantityPerThread, selectedMechanism);
                    success = true;
                    orderId = String.format("SIM-%s-%05d", selectedMechanism.name(), index);
                } catch (Exception e) {
                    errorMessage = sanitizeError(e.getMessage());
                } finally {
                    end = System.nanoTime();
                    transactions.add(new OrderTransaction(
                            String.format("TXN-%s-%05d", selectedMechanism.name(), index),
                            String.format("REQ-SIM-%05d", index),
                            orderId,
                            customer.getCustomerId(),
                            flashItemId,
                            quantityPerThread,
                            selectedMechanism,
                            Thread.currentThread().getName(),
                            start,
                            end,
                            success,
                            errorMessage));
                    doneGate.countDown();
                }
            });
        }

        awaitGate(readyGate);
        long actualStart = System.nanoTime();
        startGate.countDown();
        awaitGate(doneGate);
        long actualEnd = System.nanoTime();

        executor.shutdown();
        awaitTermination(executor);
        transactionRepository.saveAll(transactions);

        FlashSaleItem after = flashSaleItemRepository.findById(flashItemId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay flashItemId sau simulation: " + flashItemId));
        long successCount = transactions.stream().filter(OrderTransaction::isSuccess).count();
        long failCount = transactions.size() - successCount;
        double durationMs = (actualEnd - actualStart) / 1_000_000.0;
        double durationSeconds = Math.max(0.001, (actualEnd - actualStart) / 1_000_000_000.0);
        double throughput = successCount / durationSeconds;
        double avgLatencyMs = transactions.stream()
                .mapToDouble(OrderTransaction::thoiGianXuLyMs)
                .average()
                .orElse(0.0);

        return new SimulatorResult(mechanism, flashItemId, threadCount, quantityPerThread,
                successCount, failCount, limitedQty, after.getSoldQty(), throughput,
                avgLatencyMs, durationMs);
    }

    private void sellByMechanism(String flashItemId, int quantity, LockMechanism mechanism) throws Exception {
        switch (mechanism) {
            case FILE_LOCK:
                flashSaleItemRepository.sellWithFileLock(flashItemId, quantity);
                break;
            case SYNCHRONIZED:
                flashSaleItemRepository.sellWithSynchronized(flashItemId, quantity);
                break;
            case OPTIMISTIC:
                flashSaleItemRepository.sellWithOptimisticLock(flashItemId, quantity);
                break;
            case NO_LOCK:
            default:
                flashSaleItemRepository.sellNoLock(flashItemId, quantity);
                break;
        }
    }

    private void validateInput(String flashItemId, int threadCount, int quantityPerThread) {
        if (flashItemId == null || flashItemId.trim().isEmpty()) {
            throw new IllegalArgumentException("flashItemId khong duoc trong");
        }
        if (threadCount <= 0) {
            throw new IllegalArgumentException("threadCount phai > 0");
        }
        if (quantityPerThread <= 0) {
            throw new IllegalArgumentException("quantityPerThread phai > 0");
        }
    }

    /** Số customer hiện có, cũng là số thread tối đa của một lần mô phỏng. */
    public int getAvailableCustomerCount() {
        return customerRepository.findAll().size();
    }

    private List<Customer> selectCustomers(int threadCount) {
        List<Customer> customers = new ArrayList<>(customerRepository.findAll());
        customers.sort(Comparator.comparing(Customer::getCustomerId,
                String.CASE_INSENSITIVE_ORDER));
        if (customers.isEmpty()) {
            throw new IllegalArgumentException("customers.csv khong co customer de chay simulator");
        }
        if (threadCount > customers.size()) {
            throw new IllegalArgumentException(String.format(
                    "So thread (%d) vuot qua so customer trong customers.csv (%d)",
                    threadCount, customers.size()));
        }
        return new ArrayList<>(customers.subList(0, threadCount));
    }

    private void awaitGate(CountDownLatch gate) {
        try {
            if (!gate.await(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Simulation timeout sau " + DEFAULT_TIMEOUT_SECONDS + " giay");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Simulation bi interrupt", e);
        }
    }

    private void awaitTermination(ExecutorService executor) {
        try {
            if (!executor.awaitTermination(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private String sanitizeError(String message) {
        if (message == null) {
            return "";
        }
        return message.replace(',', ';').replace('\n', ' ').replace('\r', ' ');
    }

    /** Lưu một dòng tổng hợp cho mỗi mechanism để dùng lại ở báo cáo T9. */
    private void appendSummary(List<SimulatorResult> results) {
        Path transactionPath = Paths.get(transactionRepository.getFilePath()).toAbsolutePath();
        Path parent = transactionPath.getParent();
        Path summaryPath = (parent == null ? Paths.get("data").toAbsolutePath() : parent)
                .resolve("simulation_results.csv");
        boolean writeHeader = !Files.exists(summaryPath);
        String runId = "RUN-" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"));

        try (BufferedWriter writer = Files.newBufferedWriter(summaryPath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            if (writeHeader) {
                writer.write("runId,timestamp,flashItemId,threadCount,quantityPerThread,mechanism,"
                        + "successCount,failCount,limitedQty,finalSoldQty,lostUpdateQty,oversoldQty,"
                        + "violationRate,elapsedMs,tps,avgLatencyMs,vsBaselinePercent,targetPassed");
                writer.newLine();
            }
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            for (SimulatorResult r : results) {
                writer.write(String.join(",",
                        runId, timestamp, r.getFlashItemId(), String.valueOf(r.getThreadCount()),
                        String.valueOf(r.getQuantityPerThread()), r.getMechanism().name(),
                        String.valueOf(r.getSuccessCount()), String.valueOf(r.getFailCount()),
                        String.valueOf(r.getLimitedQty()), String.valueOf(r.getFinalSoldQty()),
                        String.valueOf(r.getLostUpdateQuantity()), String.valueOf(r.getOversoldQuantity()),
                        String.format(java.util.Locale.US, "%.4f", r.getSafetyViolationRate()),
                        String.format(java.util.Locale.US, "%.4f", r.getDurationMs()),
                        String.format(java.util.Locale.US, "%.4f", r.getThroughput()),
                        String.format(java.util.Locale.US, "%.4f", r.getAvgLatencyMs()),
                        String.format(java.util.Locale.US, "%.4f", r.getVsBaselinePercent()),
                        String.valueOf(r.isTargetPassed())));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Khong the ghi simulation_results.csv", e);
        }
    }
}
