package bq.sheets;

import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SheetTest {

  @BeforeEach
  public void checkAccess() {

    try {
      GoogleSheets.get();

    } catch (Exception e) {
      Assumptions.assumeTrue(false);
    }
  }

  @Test
  public void testIt() throws IOException, GeneralSecurityException {

    // https://support.google.com/docs/answer/3093281?hl=en

    GoogleSpreadsheet spreadsheet = GoogleSheets.get().getSheet("stocks");

    ValueRange body =
        new ValueRange()
            .setValues(
                Arrays.asList(
                    Arrays.asList("A", "B"),
                    Arrays.asList("1", ""),
                    Arrays.asList("2", "10"),
                    Arrays.asList("3 ", "=sum(A2:A6)"),
                    Arrays.asList("4", "20"),
                    Arrays.asList("5", "5")));

    UpdateValuesResponse result =
        GoogleSheets.get()
            .getService()
            .spreadsheets()
            .values()
            .update(spreadsheet.getId(), "A1", body)
            .setValueInputOption("USER_ENTERED")
            .execute();
    System.out.println(result);
    /*
    BarSeries bs = WorksheetData.from(spreadsheet).getBarSeries("WGMI");

    bs.getBarData().forEach(it->{
      System.out.println(it);
    });
    */

  }
}
