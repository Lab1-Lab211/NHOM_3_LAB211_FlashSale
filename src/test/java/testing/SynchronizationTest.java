package testing;

import model.FlashSaleItem;
import model.enums.LockMechanism;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import repository.FlashSaleItemRepository;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Concurrent acceptance tests cho milestone T7. */
public class SynchronizationTest {
    private static final String TEST_FILE = "data/test_synchronization_items.csv";

    @AfterEach
    public void cleanup() {
        new File(TEST_FILE).delete();
    }

    @Test
    public void synchronizedKeepsInventoryConsistentWithFiveThreads() throws Exception {
        assertSafeRun(LockMechanism.SYNCHRONIZED, 5, 5);
    }

    @Test
    public void fileLockKeepsInventoryConsistentWithFiveThreads() throws Exception {
        assertSafeRun(LockMechanism.FILE_LOCK, 5, 5);
    }

    @Test
    public void optimisticKeepsInventoryConsistentWithFiveThreads() throws Exception {
        assertSafeRun(LockMechanism.OPTIMISTIC, 5, 5);
    }

    @Test
    public void safeMechanismsNeverExceedLimitedQuantityUnderContention() throws Exception {
        for (LockMechanism mechanism : new LockMechanism[]{
                LockMechanism.FILE_LOCK, LockMechanism.SYNCHRONIZED, LockMechanism.OPTIMISTIC}) {
            RunOutcome outcome = runConcurrent(mechanism, 20, 7);
            assertTrue(outcome.finalSoldQty >= 0, mechanism + " tao ton kho am");
            assertTrue(outcome.finalSoldQty <= outcome.limitedQty, mechanism + " ban vuot kho");
            assertEquals(outcome.successCount, outcome.finalSoldQty,
                    mechanism + " co lost update: request bao thanh cong nhung kho khong tang dung");
        }
    }

    private void assertSafeRun(LockMechanism mechanism, int threads, int limitedQty) throws Exception {
        RunOutcome outcome = runConcurrent(mechanism, threads, limitedQty);
        assertFalse(outcome.timedOut, mechanism + " bi timeout");
        assertEquals(outcome.successCount, outcome.finalSoldQty);
        assertTrue(outcome.finalSoldQty <= outcome.limitedQty);
        assertTrue(outcome.finalVersion >= 1 + outcome.successCount,
                "Version phai tang sau moi lan commit thanh cong");
    }

    private RunOutcome runConcurrent(LockMechanism mechanism, int threads, int limitedQty)
            throws Exception {
        new File(TEST_FILE).delete();
        FlashSaleItemRepository repository = new FlashSaleItemRepository(TEST_FILE);
        repository.save(new FlashSaleItem(
                "FSI-TEST", "EVT-TEST", "PRD-TEST", limitedQty, 0, 100000.0, 1));

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger successes = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    sell(repository, mechanism);
                    successes.incrementAndGet();
                } catch (Exception ignored) {
                    // OutOfStock, retry exhaustion, or overlapping JVM file lock are valid failures.
                } finally {
                    done.countDown();
                }
            });
        }

        assertTrue(ready.await(5, TimeUnit.SECONDS), "Threads khong san sang kip");
        start.countDown();
        boolean completed = done.await(15, TimeUnit.SECONDS);
        executor.shutdownNow();

        FlashSaleItem after = repository.findById("FSI-TEST").orElseThrow(
                () -> new AssertionError("Khong tim thay item sau concurrent test"));
        return new RunOutcome(successes.get(), after.getSoldQty(), after.getLimitedQty(),
                after.getVersion(), !completed);
    }

    private void sell(FlashSaleItemRepository repository, LockMechanism mechanism) throws Exception {
        switch (mechanism) {
            case FILE_LOCK:
                repository.sellWithFileLock("FSI-TEST", 1);
                break;
            case SYNCHRONIZED:
                repository.sellWithSynchronized("FSI-TEST", 1);
                break;
            case OPTIMISTIC:
                repository.sellWithOptimisticLock("FSI-TEST", 1);
                break;
            default:
                repository.sellNoLock("FSI-TEST", 1);
        }
    }

    private static class RunOutcome {
        private final int successCount;
        private final int finalSoldQty;
        private final int limitedQty;
        private final int finalVersion;
        private final boolean timedOut;

        private RunOutcome(int successCount, int finalSoldQty, int limitedQty,
                           int finalVersion, boolean timedOut) {
            this.successCount = successCount;
            this.finalSoldQty = finalSoldQty;
            this.limitedQty = limitedQty;
            this.finalVersion = finalVersion;
            this.timedOut = timedOut;
        }
    }
}
