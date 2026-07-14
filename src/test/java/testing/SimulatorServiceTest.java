package testing;

import model.Customer;
import model.FlashSaleItem;
import model.enums.CustomerTier;
import model.enums.LockMechanism;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import repository.CustomerRepository;
import repository.FlashSaleItemRepository;
import repository.OrderTransactionRepository;
import service.SimulatorResult;
import service.SimulatorService;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** End-to-end acceptance tests cho milestone T8. */
public class SimulatorServiceTest {
    private static final String DIR = "data/test_simulator";
    private static final String ITEM_FILE = DIR + "/items.csv";
    private static final String CUSTOMER_FILE = DIR + "/customers.csv";
    private static final String TRANSACTION_FILE = DIR + "/transactions.csv";
    private static final String SUMMARY_FILE = DIR + "/simulation_results.csv";

    @AfterEach
    public void cleanup() {
        new File(ITEM_FILE).delete();
        new File(CUSTOMER_FILE).delete();
        new File(TRANSACTION_FILE).delete();
        new File(SUMMARY_FILE).delete();
        new File(DIR).delete();
    }

    @Test
    public void runAllProducesFourComparableResultsAndCsvLogs() throws Exception {
        SimulatorService service = createService();

        List<SimulatorResult> results = service.runAll("FSI-SIM", 8, 1);

        assertEquals(4, results.size());
        assertEquals(LockMechanism.NO_LOCK, results.get(0).getMechanism());
        for (SimulatorResult result : results) {
            assertEquals(8, result.getSuccessCount() + result.getFailCount());
            assertTrue(result.getThroughput() >= 0.0);
            assertTrue(result.getSafetyViolationRate() >= 0.0);
        }
        assertTrue(Files.exists(Paths.get(TRANSACTION_FILE)));
        OrderTransactionRepository transactionRepository =
                new OrderTransactionRepository(TRANSACTION_FILE);
        assertEquals(32, transactionRepository.count());
        assertEquals(4, transactionRepository.findByCustomer("CUS-SIM-001").size());
        assertEquals(4, transactionRepository.findByRequest("REQ-SIM-00001").size());
        assertTrue(transactionRepository.findByCustomer("CUS-SIM-001").stream()
                .allMatch(t -> "FSI-SIM".equals(t.getFlashItemId()) && t.getQuantity() == 1));
        assertTrue(Files.exists(Paths.get(SUMMARY_FILE)));
        assertEquals(5, Files.readAllLines(Paths.get(SUMMARY_FILE)).size(),
                "Summary phai co 1 header + 4 mechanism");
    }

    @Test
    public void rejectsInvalidInputAndMissingItem() {
        SimulatorService service = createService();
        assertThrows(IllegalArgumentException.class, () -> service.runAll("", 5, 1));
        assertThrows(IllegalArgumentException.class, () -> service.runAll("FSI-SIM", 0, 1));
        assertThrows(IllegalArgumentException.class, () -> service.runAll("FSI-SIM", 5, 0));
        assertThrows(IllegalArgumentException.class, () -> service.runAll("FSI-SIM", 9, 1));
        assertThrows(IllegalArgumentException.class, () -> service.runAll("MISSING", 5, 1));
    }

    @Test
    public void resultDetectsLostUpdateAndEvaluatesThirtyPercentTarget() {
        SimulatorResult safe = new SimulatorResult(LockMechanism.SYNCHRONIZED, "FSI", 10, 1,
                10, 0, 10, 10, 75.0, 1.0, 100.0);
        safe.compareWithBaseline(100.0);
        assertEquals(-25.0, safe.getVsBaselinePercent(), 0.001);
        assertTrue(safe.isTargetPassed());

        SimulatorResult race = new SimulatorResult(LockMechanism.NO_LOCK, "FSI", 10, 1,
                10, 0, 10, 7, 100.0, 1.0, 100.0);
        race.compareWithBaseline(100.0);
        assertEquals(3, race.getLostUpdateQuantity());
        assertTrue(race.hasRaceInconsistency());
    }

    private SimulatorService createService() {
        cleanup();
        FlashSaleItemRepository itemRepository = new FlashSaleItemRepository(ITEM_FILE);
        itemRepository.save(new FlashSaleItem(
                "FSI-SIM", "EVT-SIM", "PRD-SIM", 5, 0, 100000.0, 1));
        CustomerRepository customerRepository = new CustomerRepository(CUSTOMER_FILE);
        for (int i = 1; i <= 8; i++) {
            customerRepository.save(new Customer(
                    String.format("CUS-SIM-%03d", i), "Customer " + i,
                    "customer" + i + "@example.com", CustomerTier.REGULAR,
                    "2026-07-14"));
        }
        return new SimulatorService(itemRepository,
                new OrderTransactionRepository(TRANSACTION_FILE), customerRepository);
    }
}
