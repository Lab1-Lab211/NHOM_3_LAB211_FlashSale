package view;

import java.io.Console;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

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
            return line == null ? "" : line;
        }
        return scanner.nextLine();
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
