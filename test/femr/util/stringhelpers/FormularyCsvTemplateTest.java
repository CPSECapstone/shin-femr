package femr.util.stringhelpers;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FormularyCsvTemplateTest {

    @Test
    public void containsTheImportHeadersAndEscapedExampleRows() {
        assertEquals(
                "Medication,Quantity\n"
                        + "Acetaminophen 500 mg tablet,100\n"
                        + "Amoxicillin 250 mg capsule,50\n"
                        + "\"Oral rehydration salts, WHO formula\",25\n",
                FormularyCsvTemplate.getCsv());
    }
}
