package service;

import model.FlashSaleItem;
import model.OrderTransaction;
import model.enums.LockMechanism;
import repository.FlashSaleItemRepository;
import repository.OrderTransactionRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SimulatorService {
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    private final FlashSaleItemRepository flashSaleItemRepository;
    private final OrderTransactionRepository transactionRepository;

    public SimulatorService(FlashSaleItemRepository flashSaleItemRepository,
                            OrderTransactionRepository transactionRepository) {
        this.flashSaleItemRepository = flashSaleItemRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<SimulatorResult> runAll(String flashItemId, int threadCount, int quantityPerThread) {
        validateInput(flashItemId, threadCount, quantityPerThread);
        transactionRepository.clearAll();

        List<SimulatorResult> results = new ArrayList<>();
        for (LockMechanism mechanism : LockMechanism.values()) {
            results.add(runSingle(flashItemId, threadCount, quantityPerThread, mechanism));
        }
        return results;
    }

    public SimulatorResult runSingle(String flashItemId, int threadCount, int quantityPerThread,
                                     LockMechanism mechanism) {
        validateInput(flashItemId, threadCount, quantityPerThread);
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

        long wallStart = System.nanoTime();
        for (int i = 1; i <= threadCount; i++) {
            final int index = i;
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
                            orderId,
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
}
