package bq.ducktape;

import com.google.common.base.Joiner;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.ta4j.core.Indicator;

public class DocGenTest {

  @Test
  public void genTable() {

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    pw.println("| name | TA4J Indicator |");
    pw.println("|------|------|");

    Map<String, Class<? extends Indicator<?>>> map =
        IndicatorRegistry.getRegistry().getAllIndicators();

    IndicatorRegistry.getRegistry().getAvailableIndicators().keySet().stream()
        .sorted()
        .forEach(
            key -> {
              Class<? extends Indicator<?>> clazz = map.get(key);
              String shortClassName = clazz.getSimpleName();
              String shortName = key;
              String indicatorGitHubUrl =
                  "https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java";

              indicatorGitHubUrl =
                  String.format(
                      "%s/%s.java", indicatorGitHubUrl, clazz.getName().replace(".", "/"));

              String classCell = String.format("[%s](%s)", shortClassName, indicatorGitHubUrl);

              List<String> cells = Lists.newArrayList();
              cells.add(shortName);
              cells.add(classCell);

              String line = Joiner.on(" | ").join(cells);

              pw.println(String.format("| %s |", line));
            });

    pw.close();
    System.out.println(sw.toString());
  }
}
