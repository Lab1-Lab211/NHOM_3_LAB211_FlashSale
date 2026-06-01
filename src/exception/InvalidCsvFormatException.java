package exception;

/**
 * Exception khi dòng CSV có định dạng không hợp lệ — thiếu cột, sai kiểu dữ liệu, v.v.
 * Ném trong quá trình fromCsvLine() khi parse thất bại.
 */
public class InvalidCsvFormatException extends FlashSaleException {

    private final String fileName;
    private final int lineNumber;
    private final String csvLine;

    public InvalidCsvFormatException(String fileName, int lineNumber, String csvLine, String reason) {
        super(String.format("CSV không hợp lệ tại %s, dòng %d: %s. Nội dung: '%s'",
                fileName, lineNumber, reason, csvLine));
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.csvLine = csvLine;
    }

    public InvalidCsvFormatException(String message) {
        super(message);
        this.fileName = "";
        this.lineNumber = -1;
        this.csvLine = "";
    }

    public String getFileName() { return fileName; }
    public int getLineNumber() { return lineNumber; }
    public String getCsvLine() { return csvLine; }
}
