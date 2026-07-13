package testing;

import model.Product;
import model.enums.ProductCategory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import repository.ProductRepository;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProductSearchTest {
    private static final String FILE = "data/test_product_search.csv";

    @AfterEach
    void cleanup() {
        new File(FILE).delete();
    }

    @Test
    void searchSupportsVietnameseWithoutAccentsWhitespaceAndProductId() {
        new File(FILE).delete();
        ProductRepository repository = new ProductRepository(FILE);
        repository.save(new Product("PRD-00001", "Nồi cơm điện cao cấp",
                ProductCategory.GIA_DUNG, 1000000, 10, 1));

        assertEquals(1, repository.findByName("Nồi cơm điện").size());
        assertEquals(1, repository.findByName("noi   com dien").size());
        assertEquals(1, repository.findByName("prd-00001").size());

        String cp437Mojibake = new String("Nồi cơm điện".getBytes(StandardCharsets.UTF_8),
                Charset.forName("IBM437"));
        String cp1252Mojibake = new String("Nồi cơm điện".getBytes(StandardCharsets.UTF_8),
                Charset.forName("windows-1252"));
        assertEquals(1, repository.findByName(cp437Mojibake).size());
        assertEquals(1, repository.findByName(cp1252Mojibake).size());

        String brokenProductName = new String("Máy nước nóng".getBytes(StandardCharsets.UTF_8),
                Charset.forName("IBM437"));
        repository.save(new Product("PRD-00002", brokenProductName,
                ProductCategory.GIA_DUNG, 500000, 20, 1));
        assertEquals("Máy nước nóng", repository.findById("PRD-00002").get().getName());
    }
}
