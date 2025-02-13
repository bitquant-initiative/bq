package bq.ducktape.chart;

import bq.duckdb.DuckDb;
import bq.ducktape.BarSeriesTable;
import bq.sql.StatementBuilder;
import bq.sql.mapper.Mappers;
import bq.util.Json;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import java.util.function.Consumer;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;

/**
 * Trace is a thin wrapper around the underlying JSON for a data series.
 */
public class ChartTrace {

  ObjectNode traceJson = Json.createObjectNode();
  ObjectNode traceLineJson;
  ObjectNode traceMarkerJson;

  Chart chart;

  boolean newAxisCreated = false;

  ChartTrace() {
    traceJson.put("xaxis", "x");
    traceJson.put("yaxis", "y");
    traceJson.put("mode", "line");
    traceLineJson = Json.createObjectNode();
    traceJson.set("line", traceLineJson);

    traceMarkerJson = Json.createObjectNode();
    traceMarkerJson.put("symbol", "circle"); // circle, square, diamond
    traceMarkerJson.put("size", 1);
    traceMarkerJson.put("color", "red");

    traceJson.set("marker", traceMarkerJson);

    // 'solid': A solid line (default)
    // 'dash': A dashed line
    // 'dot': A dotted line
    // 'dashdot': A line with alternating dashes and dots
    // 'longdash': A line with long dashes
    // 'longdashdot': A line with long dashes and dots

    traceLineJson.put("dash", "solid");
    traceLineJson.put("width", 1);
    traceLineJson.put("color", "blue");
  }

  public ChartTrace lineWidth(double w) {
    traceLineJson.put("width", w);
    return this;
  }

  public ChartTrace lineColor(String color) {
    traceLineJson.put("color", color);
    return this;
  }

  public ChartTrace lineStyle(String s) {
    traceLineJson.put("dash", s);
    return this;
  }

  public ChartTrace yAxis(Consumer<YAxis> layoutAccessConfig) {

    YAxis axis = new YAxis();
    axis.json = (ObjectNode) chart.layout.path("yaxis");
    for (int i = 10; i >= 2; i--) {
      String name = String.format("yaxis%s", i);
      if (chart.layout.has(name)) {
        axis = new YAxis();
        axis.json = (ObjectNode) chart.layout.get(name);
        i = -1;
      }
    }

    layoutAccessConfig.accept(axis);

    return this;
  }

  public ChartTrace newYAxis(Consumer<YAxis> layoutAxisConfig) {

    Preconditions.checkState(
        newAxisCreated == false, "newYAxis() can only be called once per trace");
    this.newAxisCreated = true;
    ObjectNode layoutAxisJson = Json.createObjectNode();
    layoutAxisJson.put("side", "right");
    layoutAxisJson.put("overlaying", chart.getCurrentYAxisRef());

    YAxis yaxis = null;
    String newAxisName = null;
    for (int i = 2; yaxis == null && i < 10; i++) {
      String name = String.format("yaxis%s", i);
      if (!chart.layout.has(name)) {
        yaxis = new YAxis();
        yaxis.json = (ObjectNode) chart.layout.get(name);
        newAxisName = name;
      }
    }
    Preconditions.checkState(newAxisName != null);
    Preconditions.checkState(yaxis != null);

    String newAxisRef = newAxisName.replace("axis", ""); // y, y2, y3, etc
    chart.layout.set(newAxisName, layoutAxisJson);
    traceJson.put("yaxis", newAxisRef);

    YAxis axis = new YAxis();
    axis.json = layoutAxisJson;
    layoutAxisConfig.accept(axis);
    return this;
  }

  public ObjectNode config() {
    return traceJson;
  }

  public ChartTrace logScale() {
    yAxis(
        y -> {
          y.logScale();
        });
    return this;
  }

  public ChartTrace linearScale() {
    yAxis(
        y -> {
          y.linearScale();
        });
    return this;
  }

  ChartTrace xAxisData(ArrayNode x) {
    traceJson.set("x", x);
    return this;
  }

  ChartTrace yAxisData(ArrayNode y) {
    traceJson.set("y", y);
    return this;
  }

  public ChartTrace addData(BarSeriesTable t, String column) {
    ArrayNode d = Json.createArrayNode();
    ArrayNode p = Json.createArrayNode();

    BarSeries bs = t.getBarSeries();

    Indicator<?> indicator = t.getIndicator(column);

    for (int i = bs.getBeginIndex(); i < bs.getEndIndex(); i++) {
      Bar b = bs.getBar(i);
      String dt = b.getBeginTime().toLocalDate().toString();
      Num num = (Num) indicator.getValue(i);
      if (num != null) {
        d.add(dt);
        p.add(num.doubleValue());
      }
    }
    return addData(d, p);
  }

  public ChartTrace addData(ArrayNode x, ArrayNode y) {
    xAxisData(x);
    yAxisData(y);
    traceJson.put("mode", "lines");
    traceJson.put("type", "scatter");
    return this;
  }

  public ChartTrace addDateSeries(DuckDb db, Consumer<StatementBuilder> b) {
    return addData(db.template().query(b, Mappers.dateNumberPointMapper()).toList());
  }

  public ChartTrace addData(Iterable<bq.util.DateNumberPoint> pairs) {
    Preconditions.checkNotNull(pairs != null, "pairs");
    ArrayNode d = Json.createArrayNode();
    ArrayNode p = Json.createArrayNode();

    pairs.forEach(
        it -> {
          if (it.getDate() != null && it.getDouble().isPresent()) {
            d.add(it.getDate().toString());
            p.add(it.getDouble().get());
          }
        });

    return addData(d, p);
  }

  public ChartTrace addData(Indicator<Num> indicator) {
    ArrayNode d = Json.createArrayNode();
    ArrayNode p = Json.createArrayNode();
    xAxisData(d);
    yAxisData(p);

    BarSeries bs = indicator.getBarSeries();

    for (int i = bs.getBeginIndex(); i <= bs.getEndIndex(); i++) {
      Bar b = bs.getBar(i);
      String dt = b.getBeginTime().toLocalDate().toString();
      Num num = indicator.getValue(i);

      if (num != null && !num.isNaN()) d.add(dt);
      p.add(num.doubleValue());
    }

    traceJson.put("mode", "lines");
    traceJson.put("type", "scatter");
    return this;
  }

  public ChartTrace addData(BarSeries bs) {
    return addData(new ClosePriceIndicator(bs));
  }
}
