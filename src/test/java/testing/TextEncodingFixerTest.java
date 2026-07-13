package testing;

import org.junit.jupiter.api.Test;
import util.TextEncodingFixer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TextEncodingFixerTest {
    @Test
    void repairsCp437ConsoleInputWithoutChangingCorrectText() {
        String correct = "Nồi cơm điện Sharp";
        String broken = new String(correct.getBytes(StandardCharsets.UTF_8), Charset.forName("IBM437"));

        assertEquals(correct, TextEncodingFixer.repairConsoleText(broken));
        assertEquals(correct, TextEncodingFixer.repairConsoleText(correct));
        assertEquals("123456", TextEncodingFixer.repairConsoleText("123456"));
    }
}
