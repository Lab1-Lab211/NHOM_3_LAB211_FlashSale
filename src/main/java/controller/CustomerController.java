package controller;

import model.Customer;
import model.enums.CustomerTier;
import service.CustomerService;

import java.util.Optional;

public class CustomerController {
    private final CustomerService customerService;
    private Customer currentCustomer;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    public Customer register(String name, String email, CustomerTier tier) {
        currentCustomer = customerService.register(name, email, tier);
        return currentCustomer;
    }

    public Optional<Customer> login(String email) {
        Optional<Customer> customer = customerService.login(email);
        if (customer.isPresent()) {
            currentCustomer = customer.get();
        }
        return customer;
    }

    public void logout() {
        currentCustomer = null;
    }

    public Customer getCurrentCustomer() {
        return currentCustomer;
    }

    public boolean isLoggedIn() {
        return currentCustomer != null;
    }
}
