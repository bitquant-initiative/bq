package bq.ducktape.chart;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class YAxis {
  ObjectNode json;

  ObjectNode config() {
    return json;
  }

  public YAxis overlaying(String ref) {
    config().put("overlaying", ref);
    return this;
  }

  public YAxis side(String side) {
    config().put("side", side);
    return this;
  }

  public YAxis logScale() {

    config().put("type", "log");
    return this;
  }

  public YAxis linearScale() {
    config().put("type", "linear");
    return this;
  }
}
