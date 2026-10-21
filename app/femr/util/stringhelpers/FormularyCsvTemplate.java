package femr.util.stringhelpers;

/**
 * Provides the starter CSV used by the formulary import workflow.  Keep the
 * header names in sync with InventoryService#importCSV.
 */
public final class FormularyCsvTemplate {

    private static final String[] HEADERS = {"Medication", "Quantity"};
    private static final String[][] EXAMPLE_ROWS = {
            {"Acetaminophen 500 mg tablet", "100"},
            {"Amoxicillin 250 mg capsule", "50"},
            {"Oral rehydration salts, WHO formula", "25"}
    };

    private FormularyCsvTemplate() {
    }

    public static String getCsv() {
        StringBuilder csv = new StringBuilder();
        appendRow(csv, HEADERS);
        for (String[] row : EXAMPLE_ROWS) {
            appendRow(csv, row);
        }
        return csv.toString();
    }

    private static void appendRow(StringBuilder csv, String[] values) {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                csv.append(',');
            }
            csv.append(escape(values[index]));
        }
        csv.append('\n');
    }

    private static String escape(String value) {
        if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}
