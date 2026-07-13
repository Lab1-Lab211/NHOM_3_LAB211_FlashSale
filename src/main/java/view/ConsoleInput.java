package view;

import java.io.Console;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;
import util.TextEncodingFixer;

public class ConsoleInput {
    private final Scanner scanner = new Scanner(new InputStreamReader(System.in, StandardCharsets.UTF_8));

    public String readLine(String prompt) {
        System.out.print(prompt);
        return readLine();
    }

    public String readLine() {
        Console console = System.console();
        if (console != null) {
            String line = console.readLine();
            return line == null ? "" : TextEncodingFixer.repairConsoleText(line);
        }
        return TextEncodingFixer.repairConsoleText(scanner.nextLine());
    }

    public String readPassword(String prompt) {
        Console console = System.console();
        if (console == null) {
            // IDE consoles often do not expose java.io.Console, so masking is not
            // available there. Run the application in a real terminal/run.bat.
            System.out.print(prompt);
            return scanner.nextLine();
        }

        char[] passwordChars = console.readPassword("%s", prompt);
        if (passwordChars == null) {
            return "";
        }

        try {
            return new String(passwordChars);
        } finally {
            Arrays.fill(passwordChars, '\0');
        }
    }

    public int readInt(String prompt) {
        String input = readLine(prompt).trim();
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
