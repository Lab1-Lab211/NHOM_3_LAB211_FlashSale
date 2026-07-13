package util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/** Phuc hoi chuoi UTF-8 bi Windows console doc nham thanh CP437/CP1252. */
public final class TextEncodingFixer {
    private static final Charset CP437 = Charset.forName("IBM437");
    private static final Charset CP1252 = Charset.forName("windows-1252");

    private TextEncodingFixer() { }

    public static String repairConsoleText(String value) {
        if (value == null || value.isEmpty()) return value;

        if (looksLikeCp437Mojibake(value)) {
            String repaired = redecode(value, CP437);
            if (!repaired.contains("\uFFFD")) return repaired;
        }
        if (looksLikeCp1252Mojibake(value)) {
            String repaired = redecode(value, CP1252);
            if (!repaired.contains("\uFFFD")) return repaired;
        }
        return value;
    }

    private static String redecode(String value, Charset mistakenCharset) {
        return new String(value.getBytes(mistakenCharset), StandardCharsets.UTF_8);
    }

    private static boolean looksLikeCp437Mojibake(String value) {
        return value.matches(".*[ß╗║╞─æç├┤┐└┴┬│┼].*");
    }

    private static boolean looksLikeCp1252Mojibake(String value) {
        return value.contains("Ã") || value.contains("Â") || value.contains("Ä")
                || value.contains("Æ") || value.contains("á»") || value.contains("áº");
    }
}
