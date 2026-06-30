package testing;

import model.Customer;
import model.enums.CustomerTier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repository.CustomerRepository;
import service.CustomerService;
import controller.CustomerController;

import java.io.File;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class CustomerServiceTest {
    private static final String TEST_CSV_PATH = "data/test_customers_service.csv";
    private CustomerRepository customerRepository;
    private CustomerService customerService;
    private CustomerController customerController;

    @BeforeEach
    public void setUp() {
        // Xóa file test cũ nếu có
        deleteFile(TEST_CSV_PATH);
        customerRepository = new CustomerRepository(TEST_CSV_PATH);
        customerService = new CustomerService(customerRepository);
        customerController = new CustomerController(customerService);
    }

    @AfterEach
    public void tearDown() {
        deleteFile(TEST_CSV_PATH);
    }

    private void deleteFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    public void testRegisterNewCustomerSavesSuccessfully() {
        Customer customer = customerService.register("CUS-99999", "Nguyen Van A", "a.nguyen@email.com", CustomerTier.REGULAR);
        
        assertNotNull(customer);
        assertEquals("CUS-99999", customer.getCustomerId());
        assertEquals("Nguyen Van A", customer.getName());
        assertEquals("a.nguyen@email.com", customer.getEmail());
        assertEquals(CustomerTier.REGULAR, customer.getTier());

        // Kiểm tra xem đã ghi vào file/repository chưa
        Optional<Customer> saved = customerRepository.findById("CUS-99999");
        assertTrue(saved.isPresent());
        assertEquals("a.nguyen@email.com", saved.get().getEmail());
    }

    @Test
    public void testRegisterDuplicateIdThrowsException() {
        customerService.register("CUS-00001", "Nguyen Van A", "a.nguyen@email.com", CustomerTier.REGULAR);

        // Đăng ký tiếp một người có cùng ID nhưng khác email
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            customerService.register("CUS-00001", "Nguyen Van B", "b.nguyen@email.com", CustomerTier.VIP);
        });

        assertTrue(exception.getMessage().contains("Customer ID da ton tai"));
    }

    @Test
    public void testRegisterDuplicateEmailThrowsException() {
        customerService.register("CUS-00001", "Nguyen Van A", "a.nguyen@email.com", CustomerTier.REGULAR);

        // Đăng ký tiếp một người có cùng email nhưng khác ID
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            customerService.register("CUS-00002", "Nguyen Van B", "a.nguyen@email.com", CustomerTier.VIP);
        });

        assertTrue(exception.getMessage().contains("Email da ton tai"));
    }

    @Test
    public void testLoginExistingEmailReturnsCustomer() {
        customerService.register("CUS-00001", "Nguyen Van A", "a.nguyen@email.com", CustomerTier.REGULAR);

        Optional<Customer> loggedIn = customerService.login("a.nguyen@email.com");
        assertTrue(loggedIn.isPresent());
        assertEquals("Nguyen Van A", loggedIn.get().getName());
    }

    @Test
    public void testLoginNonExistentEmailReturnsEmpty() {
        Optional<Customer> loggedIn = customerService.login("nonexistent@email.com");
        assertFalse(loggedIn.isPresent());
    }

    @Test
    public void testControllerStateLoginLogout() {
        assertFalse(customerController.isLoggedIn());
        assertNull(customerController.getCurrentCustomer());

        // Đăng ký & Tự động set currentCustomer
        Customer c = customerController.register("Nguyen Van A", "a.nguyen@email.com", CustomerTier.VIP);
        assertNotNull(c);
        assertTrue(customerController.isLoggedIn());
        assertEquals(c, customerController.getCurrentCustomer());

        // Logout
        customerController.logout();
        assertFalse(customerController.isLoggedIn());
        assertNull(customerController.getCurrentCustomer());

        // Login lại qua Controller
        Optional<Customer> loggedIn = customerController.login("a.nguyen@email.com");
        assertTrue(loggedIn.isPresent());
        assertTrue(customerController.isLoggedIn());
        assertEquals("Nguyen Van A", customerController.getCurrentCustomer().getName());
    }
}
