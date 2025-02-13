package bq.sheets;

import bq.util.BqException;
import bq.util.Dates;
import bq.util.S;
import bq.util.Zones;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarBuilder;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.NaN;
import org.ta4j.core.num.Num;

public class WorksheetData {

  GoogleSpreadsheet spreadsheet;

  public static WorksheetData from(GoogleSpreadsheet spreadsheet) {
    WorksheetData d = new WorksheetData();
    d.spreadsheet = spreadsheet;
    return d;
  }

  // =GOOGLEFINANCE("WGMI", "all", DATE(2022,1,1), TODAY(), "DAILY")
  public BarSeries getBarSeries(String tab) {

    List<Bar> bars = Lists.newArrayList();
    try {
      String range = "A1:H20000";
      if (S.isNotBlank(tab)) {
        range = tab + "!" + range;
      }
      ValueRange response =
          GoogleSheets.get()
              .getService()
              .spreadsheets()
              .values()
              .get(spreadsheet.getId(), range)
              .execute();

      List<List<Object>> values = response.getValues();

      List<Object> header = (List<Object>) values.get(0);

      Map<String, Integer> colMap = Maps.newHashMap();
      for (int i = 0; i < header.size(); i++) {
        String val = Objects.toString(header.get(i)).toLowerCase().trim();
        colMap.put(val, i);
      }

      values.stream()
          .skip(1)
          .forEach(
              it -> {
                Optional<Bar> bar = toBar(it, colMap);
                if (bar.isPresent()) {
                  bars.add(bar.get());
                }
              });

    } catch (IOException e) {
      throw new BqException(e);
    }

    return new BaseBarSeriesBuilder().withNumTypeOf(DoubleNum.ZERO).withBars(bars).build();
  }

  Optional<Num> toNum(List<Object> data, Map<String, Integer> colMap, String name) {
    Integer col = colMap.get(name);
    if (col == null) {
      return Optional.of(NaN.NaN);
    }
    try {
      String v = Objects.toString(data.get(col));
      Double d = Double.parseDouble(v);
      return Optional.of(DoubleNum.valueOf(d));

    } catch (Exception e) {
      return Optional.of(NaN.NaN);
    }
  }

  Optional<ZonedDateTime> parse(String s) {

    if (S.isBlank(s)) {
      return Optional.empty();
    }
    // Google Sheets + GoogleFinance loves this format
    // 1/14/2010 16:00:00

    try {
      LocalDateTime dt = LocalDateTime.parse(s, DateTimeFormatter.ofPattern("M/d/yyyy HH:mm:ss"));

      return Optional.of(dt.atZone(Zones.NYC));

    } catch (Exception ignore) {

    }

    return Dates.asZonedDateTime(s, Zones.NYC);
  }

  Optional<Bar> toBar(List<Object> data, Map<String, Integer> colMap) {

    Integer c = colMap.get("date");
    if (c == null) {
      return Optional.empty();
    }

    String dateString = Objects.toString(data.get(c));

    Optional<ZonedDateTime> dt = parse(dateString);

    if (dt.isEmpty()) {
      return Optional.empty();
    }
    BaseBarBuilder bbb = BaseBar.builder(DoubleNum.ZERO, String.class);

    bbb.timePeriod(Duration.ofDays(1));
    bbb.endTime(dt.get().plusDays(1).truncatedTo(ChronoUnit.DAYS));

    toNum(data, colMap, "open")
        .ifPresent(
            n -> {
              bbb.openPrice(n);
            });
    toNum(data, colMap, "high")
        .ifPresent(
            n -> {
              bbb.highPrice(n);
            });

    toNum(data, colMap, "low")
        .ifPresent(
            n -> {
              bbb.lowPrice(n);
            });
    toNum(data, colMap, "close")
        .ifPresent(
            n -> {
              bbb.closePrice(n);
            });
    toNum(data, colMap, "volume")
        .ifPresent(
            n -> {
              bbb.volume(n);
            });

    return Optional.of(bbb.build());
  }

  private void tabs() {
    /* Spreadsheet sp = service.spreadsheets().get(spreadsheetId).execute();
    List<Sheet> sheets = sp.getSheets();

    sheets.forEach(it -> {
      GridProperties px;

      System.out.println(it.getProperties().get("gridProperties").getClass());
    });*/
  }
}
