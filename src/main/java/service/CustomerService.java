package service;

import model.Customer;
import model.enums.CustomerTier;
import repository.CustomerRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import util.PasswordHasher;

public class CustomerService {
    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer register(String name, String email, CustomerTier tier) {
        String customerId = nextCustomerId();
        return register(customerId, name, email, PasswordHasher.DEFAULT_PASSWORD, tier);
    }

    public Customer register(String name, String email, String password) {
        String customerId = nextCustomerId();
        return register(customerId, name, email, password, CustomerTier.REGULAR);
    }

    public Customer register(String name, String email) {
        return register(name, email, CustomerTier.REGULAR);
    }

    public Customer register(String customerId, String name, String email, CustomerTier tier) {
        return register(customerId, name, email, PasswordHasher.DEFAULT_PASSWORD, tier);
    }

    public Customer register(String customerId, String name, String email,
                             String password, CustomerTier tier) {
        validateRegisterInput(customerId, name, email, password, tier);

        if (customerRepository.findById(customerId).isPresent()) {
            throw new IllegalArgumentException("Customer ID da ton tai: " + customerId);
        }
        if (customerRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email da ton tai: " + email);
        }

        PasswordHasher.PasswordData passwordData = PasswordHasher.hash(password);
        Customer customer = new Customer(
                customerId,
                name.trim(),
                email.trim(),
                passwordData.getHash(),
                passwordData.getSalt(),
                tier,
                LocalDate.now().toString());
        customerRepository.save(customer);
        return customer;
    }

    public Optional<Customer> login(String email) {
        return login(email, PasswordHasher.DEFAULT_PASSWORD);
    }

    public Optional<Customer> login(String email, String password) {
        if (email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }
        Optional<Customer> customer = customerRepository.findByEmail(email.trim());
        if (!customer.isPresent()) {
            return Optional.empty();
        }
        Customer found = customer.get();
        return PasswordHasher.verify(password, found.getPasswordHash(), found.getPasswordSalt())
                ? customer : Optional.empty();
    }

    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    private void validateRegisterInput(String customerId, String name, String email,
                                       String password, CustomerTier tier) {
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID khong duoc trong");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Ten customer khong duoc trong");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email khong duoc trong");
        }
        if (password == null || password.trim().length() < 6) {
            throw new IllegalArgumentException("Mat khau phai co it nhat 6 ky tu");
        }
        if (tier == null) {
            throw new IllegalArgumentException("Tier khong duoc null");
        }
    }

    private String nextCustomerId() {
        int max = 0;
        for (Customer customer : customerRepository.findAll()) {
            max = Math.max(max, extractNumber(customer.getCustomerId(), "CUS-"));
        }
        return String.format("CUS-%05d", max + 1);
    }

    private int extractNumber(String id, String prefix) {
        if (id == null || !id.startsWith(prefix)) {
            return 0;
        }
        try {
            return Integer.parseInt(id.substring(prefix.length()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
