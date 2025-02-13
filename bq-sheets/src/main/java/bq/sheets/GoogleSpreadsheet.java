package bq.sheets;

import com.google.api.services.sheets.v4.model.Spreadsheet;

public class GoogleSpreadsheet {

  Spreadsheet spreadsheet;

  public String getId() {
    return getSpreadsheet().getSpreadsheetId();
  }

  public Spreadsheet getSpreadsheet() {

    return spreadsheet;
  }

  public String toString() {
    return getSpreadsheet().getSpreadsheetUrl();
  }
}
