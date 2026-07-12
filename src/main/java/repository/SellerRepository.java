package repository;

import model.Seller;

import java.util.Optional;

public class SellerRepository extends CsvRepository<Seller> {
    public SellerRepository(String filePath) {
        super(filePath, Seller::new);
    }

    public Optional<Seller> findByEmail(String email) {
        return findBy(s -> s.getEmail().equalsIgnoreCase(email)).stream().findFirst();
    }
}
