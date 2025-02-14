package bq.sql;

import bq.util.BqException;
import bq.util.Plural;
import bq.util.S;
import com.google.common.collect.Lists;
import com.jakewharton.picnic.Cell;
import com.jakewharton.picnic.CellStyle;
import com.jakewharton.picnic.Row;
import com.jakewharton.picnic.Table;
import com.jakewharton.picnic.TableSection;
import com.jakewharton.picnic.TableStyle;
import com.jakewharton.picnic.TextAlignment;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ResultSetTextFormatter implements ResultSetProcessor<String> {

  int maxDisplayRows = 40;

  class ColumnValue {
    Object value;
    String text;
  }

  class ResultSetRow {
    long seq;
    List<ColumnValue> vals;
  }

  public String toString(ResultSet rs) {
    return process(Results.create(rs));
  }

  String trimText(String input) {
    if (input == null) {
      return null;
    }
    String s = input;
    if (s.length() > 100) {
      s = s.substring(0, 100) + "...";
    }
    return s;
  }

  public String process(Results rs) {

    try {
      List<String> columnNames = Lists.newArrayList();
      List<ResultSetRow> rows = Lists.newArrayList();

      var meta = rs.getResultSet().getMetaData();
      var columnCount = meta.getColumnCount();

      int count = 0;
      int hiddenRowCount = 0;
      int deleteRowIndex = maxDisplayRows / 2;
      while (rs.next()) {
        ResultSetRow r = new ResultSetRow();
        r.vals = new ArrayList<>();
        r.seq = count++;

        for (int i = 1; i <= columnCount; i++) {
          var v = new ColumnValue();
          v.value = rs.getResultSet().getObject(i);
          v.text = rs.getString(i).orElse("NULL");

          r.vals.add(v);
        }
        if (rows.size() >= maxDisplayRows) {
          rows.remove(deleteRowIndex);
          hiddenRowCount++;
        }
        rows.add(r);
      }

      // Thsi is the place to go back and re-align decimals

      // Now build the picnic table

      var tsb = new TableSection.Builder();
      CellStyle columnNameStyle =
          new CellStyle.Builder()
              .setPaddingLeft(2)
              .setPaddingRight(2)
              .setAlignment(TextAlignment.MiddleCenter)
              .setBorderLeft(true)
              .setBorderRight(true)
              .setBorderBottom(false)
              .setBorderTop(true)
              .build();
      CellStyle columnTypeStyle =
          new CellStyle.Builder()
              .setPaddingLeft(2)
              .setPaddingRight(2)
              .setAlignment(TextAlignment.MiddleCenter)
              .setBorderLeft(true)
              .setBorderRight(true)
              .setBorderBottom(true)
              .setBorderTop(false)
              .build();

      var header = new Row.Builder();
      for (int i = 1; i <= meta.getColumnCount(); i++) {
        var hc = new Cell.Builder(meta.getColumnName(i)).setStyle(columnNameStyle).build();
        header.addCell(hc);
      }
      tsb.addRow(header.build());
      header = new Row.Builder();

      for (int i = 1; i <= meta.getColumnCount(); i++) {
        var hc =
            new Cell.Builder(S.notNull(meta.getColumnTypeName(i)).orElse("").toLowerCase())
                .setStyle(columnTypeStyle)
                .build();
        header.addCell(hc);
      }
      tsb.addRow(header.build());

      for (ResultSetRow rsr : rows) {

        var rowBuilder = new Row.Builder();

        for (int col = 0; col < rsr.vals.size(); col++) {
          ColumnValue cellVal = rsr.vals.get(col);

          var style = getCellStyleForColumn(meta, col + 1);

          var cell = new Cell.Builder(cellVal.text).setStyle(style).build();
          rowBuilder.addCell(cell);
        }

        tsb.addRow(rowBuilder.build());
        if (rsr.seq == deleteRowIndex - 1) {

          for (int j = 0; j < Math.min(hiddenRowCount, 3); j++) {
            rowBuilder = new Row.Builder();
            for (int i = 1; i <= columnCount; i++) {
              rowBuilder.addCell(
                  new Cell.Builder("...").setStyle(getCellStyleForColumn(meta, i)).build());
            }
            tsb.addRow(rowBuilder.build());
          }
        }
      }

      var footerRow = new Row.Builder();
      String rowCountText = String.format("%s", Plural.toCount(count, "row"));

      if (hiddenRowCount > 0) {
        rowCountText = String.format("%s (%s shown)", rowCountText, maxDisplayRows);
      }
      var rowCountCell = new Cell.Builder(rowCountText).setColumnSpan(columnCount).build();
      footerRow.addCell(rowCountCell);
      tsb.addRow(footerRow.build());

      var tableBuilder = new Table.Builder();
      tableBuilder.setTableStyle(new TableStyle.Builder().setBorder(true).build());

      tableBuilder.setBody(tsb.build());
      tableBuilder.setCellStyle(
          new CellStyle.Builder().setBorder(true).setPaddingLeft(1).setPaddingRight(1).build());
      Table table = tableBuilder.build();

      return table.toString();

    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  static final CellStyle RIGHT_JUSTIFIED_VALUE =
      new CellStyle.Builder()
          .setPaddingLeft(2)
          .setPaddingRight(2)
          .setAlignment(TextAlignment.MiddleRight)
          .setBorderLeft(true)
          .setBorderRight(true)
          .setBorderBottom(false)
          .setBorderTop(false)
          .build();

  static final CellStyle LEFT_JUSTIFIED_VALUE =
      new CellStyle.Builder()
          .setPaddingLeft(2)
          .setPaddingRight(2)
          .setAlignment(TextAlignment.MiddleLeft)
          .setBorderLeft(true)
          .setBorderRight(true)
          .setBorderBottom(false)
          .setBorderTop(false)
          .build();

  CellStyle getCellStyleForColumn(ResultSetMetaData md, int c) {
    try {
      String typeName = md.getColumnTypeName(c);
      if (typeName == null) {
        return RIGHT_JUSTIFIED_VALUE;
      }
      if (typeName.toLowerCase().contains("char")) {
        return LEFT_JUSTIFIED_VALUE;
      }
      return RIGHT_JUSTIFIED_VALUE;
    } catch (SQLException e) {
      throw new BqException(e);
    }
  }
}
