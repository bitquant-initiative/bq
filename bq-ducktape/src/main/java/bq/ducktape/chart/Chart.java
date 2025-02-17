package bq.ducktape.chart;

import bq.util.BqException;
import bq.util.Json;
import bq.util.RuntimeEnvironment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.flogger.FluentLogger;
import com.google.common.io.Closer;
import com.google.common.io.Files;
import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class Chart {

  String chartDiv = "chart1";
  ObjectNode layout = Json.createObjectNode();
  ObjectNode config = Json.createObjectNode();
  ArrayNode data = Json.createArrayNode();

  ObjectNode combinedConfig = Json.createObjectNode();
  static FluentLogger logger = FluentLogger.forEnclosingClass();

  static AtomicBoolean desktopDisabled = new AtomicBoolean(false);

  boolean scopedVariables = false;

  public String getDivVar() {
    return chartDiv.replace("-", "_");
  }

  private String toScopedVar(String name) {
    if (scopedVariables) {
      return String.format("%s_%s", getDivVar(), name);
    } else {
      return name;
    }
  }

  private String getLayoutVar() {
    return toScopedVar("layout");
  }

  private String getConfigVar() {
    return toScopedVar("config");
  }

  private String getDataVar() {
    return toScopedVar("data");
  }

  public Optional<ChartTrace> getTrace(String name) {

    for (JsonNode n : data) {
      if (n.path("name").asText().equals(name)) {

        ChartTrace t = new ChartTrace();
        t.traceJson = (ObjectNode) n;
        return Optional.of(t);
      }
    }

    return Optional.empty();
  }

  public static void disableBrowser() {
    if (desktopDisabled.get() == false) {
      logger.atInfo().log("disabling desktop capabilities (opening charts)");
      desktopDisabled.set(true);
    }
  }

  /**
   * Add/update a named trace. Using the Consumer is a little bit odd, but makes
   * the usage code a bit simpler and more fluent than constructing a bunch of
   * objects or json.
   *
   * @param name
   * @param cb
   * @return
   */
  public Chart trace(String name, Consumer<ChartTrace> cb) {

    ChartTrace t = getTrace(name).orElse(null);
    if (t == null) {
      t = new ChartTrace();
      t.chart = this;
      t.traceJson.put("name", name);
    }

    cb.accept(t);
    data.add(t.traceJson);

    return this;
  }

  String getNextYAxisName() {
    for (int i = 2; i < 10; i++) {
      String name = String.format("yaxis%s", i);
      if (!layout.has(name)) {
        return name;
      }
    }
    throw new IllegalStateException("too many axes");
  }

  String getCurrentYAxisRef() {
    return getCurrentYAxisName().replace("axis", "");
  }

  String getCurrentYAxisName() {
    if (!layout.has("yaxis2")) {
      return "yaxis";
    }
    for (int i = 3; i < 10; i++) {
      String name = String.format("yaxis%s", i);
      if (!layout.has(name)) {
        return String.format("yaxis%s", i - 1);
      }
    }
    return "yaxis";
  }

  private Chart() {

    config.put("displaylogo", false);
    config.put("responsive", true);

    ObjectNode xAxis = Json.createObjectNode();
    ObjectNode yAxis = Json.createObjectNode();
    layout.set("xaxis", xAxis);
    layout.set("yaxis", yAxis);

    // width(750);
    // height(400);

    xAxis.put("title", "");
    yAxis.put("title", "");

    combinedConfig.set("layout", layout);
    combinedConfig.set("data", data);
    combinedConfig.set("config", config);
  }

  public static Chart newChart() {
    return new Chart();
  }

  public Chart width(int width) {
    layout.put("width", width);
    return this;
  }

  public Chart height(int h) {
    layout.put("height", h);
    return this;
  }

  public Chart title(String title) {

    layout.put("title", title);

    return this;
  }

  public Chart targetDiv(String div) {
    this.chartDiv = div;
    return this;
  }

  public String toHtml() {
    StringWriter sw = new StringWriter();
    writeHtml(sw);
    return sw.toString();
  }

  public String toJavaScript() {
    StringWriter sw = new StringWriter();
    writeJavaScript(sw);
    return sw.toString();
  }

  public Chart scopedVariables(boolean b) {
    this.scopedVariables = b;
    return this;
  }

  public void writeJavaScript(Writer w) {
    PrintWriter pw = new PrintWriter(w);
    pw.println("var " + getDivVar() + " = document.getElementById('" + chartDiv + "');");

    pw.println(JSUtil.toVariableDeclaration(getLayoutVar(), layout));
    pw.println(JSUtil.toVariableDeclaration(getDataVar(), data));
    pw.println(JSUtil.toVariableDeclaration(getConfigVar(), config));

    pw.println(
        String.format("Plotly.newPlot(%s,%s,%s,%s);", getDivVar(), getDataVar(), getLayoutVar(), getConfigVar()));
    pw.println("");
    pw.flush();
  }

  public void writeHtml(Writer w) {

    PrintWriter pw = new PrintWriter(w);
    pw.println("<!DOCTYPE html>");
    pw.println("<html>");
    pw.println("<head>");
    pw.println("<meta charset=\"UTF-*\">");
    pw.println("<title>Title</title>");

    pw.println("<script src=\"https://cdn.plot.ly/plotly-latest.min.js\"></script>");
    pw.println("</head>");
    pw.println("<body>");

    pw.println("<div id='" + chartDiv + "'></div>");

    pw.println("<script>");

    pw.flush();
    writeJavaScript(w);
    pw.println("</script>");
    pw.println("</body></html>");

    pw.flush();
  }

  public void writeJavaScript(File f) {
    try (Closer closer = Closer.create()) {
      Writer w = Files.asCharSink(f, StandardCharsets.UTF_8).openStream();
      closer.register(w);
      writeJavaScript(w);
      w.flush();
    } catch (IOException e) {
      throw new BqException(e);
    }
  }

  public void view(File f) {
    try {
      if (desktopDisabled.get()) {
        logger.atWarning().log("opening Charts in browser has been disabled");
        return;
      }
      if (GraphicsEnvironment.isHeadless() || RuntimeEnvironment.get().isCIEnvironment()) {
        logger.atWarning().log("chart cannot be opened in headless environment");
        return;
      }

      logger.atInfo().log("opening %s", f);
      Desktop.getDesktop().open(f);
    } catch (IOException e) {
      throw new BqException(e);
    }
  }

  public void view() {

    try (Closer closer = Closer.create()) {
      Path p = java.nio.file.Files.createTempFile("chart_", ".html");

      Writer w = Files.asCharSink(p.toFile(), StandardCharsets.UTF_8).openStream();
      closer.register(w);
      writeHtml(w);
      w.close();

      view(p.toFile());

    } catch (IOException e) {
      throw new BqException(e);
    }
  }
}
