package bq.ducktape.chart;

import bq.util.BqException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.StringWriter;

/**
 * Utilities to convert JSON to JavaScript objects suitable for inline code-generation
 * for plotly charts. This is not pretty.
 */
public class JSUtil {

  static class IndentationContext {
    int level = 0;

    String indent(String line) {
      try {
        StringWriter sw = new StringWriter();

        for (int i = 0; i < level; i++) {
          sw.append("    ");
        }
        sw.append(line);
        sw.close();
        return sw.toString();
      } catch (IOException e) {
        throw new BqException(e);
      }
    }

    public IndentationContext indent() {
      IndentationContext ctx = new IndentationContext();
      ctx.level = level + 1;
      return ctx;
    }
  }

  public static String toVariableDeclaration(String name, JsonNode n) {
    return toVariableDeclaration(name, n, new IndentationContext());
  }

  public static String toVariableDeclaration(String name, JsonNode n, IndentationContext indent) {

    String declaration = String.format("var %s = %s;", name, toObject(n));

    return indent.indent(declaration);
  }

  public static String toObject(JsonNode n) {
    return toObject(n, new IndentationContext());
  }

  public static String toObject(JsonNode n, IndentationContext indent) {

    // Would it be simpler and/or more reliable to emit JSON inside
    // of a javascript string, and call JSON.parse() ???

    if (n == null || n.isNull() || n.isMissingNode()) {
      return "null";
    }
    if (n.isNumber()) {
      return indent.indent(n.asText());
    }
    if (n.isTextual()) {
      return indent.indent(String.format("\"%s\"", n.asText().replace("\"", "\\\"")));
    }
    if (n.isBoolean()) {
      return indent.indent(n.asText());
    }
    if (n.isArray()) {
      StringBuffer sb = new StringBuffer();
      ArrayNode an = (ArrayNode) n;
      sb.append("[ ");
      an.forEach(
          t -> {
            sb.append(toObject(t));
            sb.append(", ");
          });
      sb.append("]");
      String s = sb.toString();
      if (s.endsWith(", ]")) {
        s = s.substring(0, s.length() - 3);
        s = s + " ]";
      }
      return indent.indent(s);
    }
    if (n.isObject()) {
      StringBuffer sb = new StringBuffer();
      ObjectNode on = (ObjectNode) n;
      sb.append("{");
      on.fieldNames()
          .forEachRemaining(
              field -> {
                sb.append("\n");
                sb.append(indent.indent().indent(field));
                sb.append(": ");
                sb.append(toObject(on.get(field), indent));
                sb.append(",");
              });
      sb.append("\n}");
      return sb.toString();
    }
    throw new IllegalArgumentException();
  }
}
