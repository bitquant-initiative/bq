package bq.util;

import com.google.common.base.Charsets;
import com.google.common.base.Suppliers;
import com.google.common.flogger.FluentLogger;
import com.google.common.io.Files;
import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.function.Supplier;

public class RuntimeEnvironment {

  static FluentLogger logger = FluentLogger.forEnclosingClass();
  public static final RuntimeEnvironment instance = new RuntimeEnvironment();

  Supplier<Boolean> kubeSupplier =
      Suppliers.memoize(
          () -> {
            if (!isRunningInContainer()) {
              return false;
            }
            if (System.getenv("KUBERNETES_PORT") != null
                && System.getenv("KUBERNETES_SERVICE_HOST") != null) {
              return true;
            }
            return false;
          });
  Supplier<Boolean> containerSupplier =
      Suppliers.memoize(
          () -> {
            try {

              String s =
                  S.notNull(Files.asCharSource(new File("/proc/1/cgroup"), Charsets.UTF_8).read())
                      .orElse("")
                      .strip();

              logger.atInfo().log("/proc/1/cgroup: <<%s>>", s);
              if (s.equalsIgnoreCase("0::/")) {
                logger.atFine().log("inside container!");
                return true;
              }
            } catch (FileNotFoundException e) {
              // ignore
            } catch (IOException | RuntimeException e) {
              // ignore
              logger.atFine().withCause(e).log("exception (can be ignored)");
            }

            try {
              if (new File("/.dockerenv").exists()) {
                logger.atFine().log("found /.dockerenv");
                return true;
              }
            } catch (Exception e) {
              logger.atFine().withCause(e).log("exception (can be ignored)");
            }

            return false;
          });

  Supplier<Boolean> unitTestEnvironment =
      Suppliers.memoize(
          () -> {
            try {

              Class.forName("org.junit.jupiter.api.Test");
              return true;
            } catch (ClassNotFoundException e) {
              // ignore
            } catch (Exception e) {
              logger.atFine().withCause(e).log("exception (can be ignored)");
            }
            return false;
          });

  private RuntimeEnvironment() {}

  public static RuntimeEnvironment get() {
    return instance;
  }

  public boolean isSourceEnvironment() {
    File f = new File("./src/main/java/bq");
    return f.exists();
  }

  public boolean isRunningInFly() {
    return S.isNotBlank(System.getenv("FLY_APP_NAME"));
  }

  public boolean isRunningInKubernetes() {
    return kubeSupplier.get();
  }

  public boolean isRunningInContainer() {
    return containerSupplier.get();
  }

  public boolean isUnitTestEnvironment() {
    return unitTestEnvironment.get();
  }

  private String getOsProperty() {
    return S.notBlank(System.getProperty("os.name")).orElse("").toLowerCase().trim();
  }

  public boolean isLinux() {
    return getOsProperty().contains("linux");
  }

  public boolean isMac() {
    return getOsProperty().contains("mac") || getOsProperty().contains("os x");
  }

  public boolean isCIEnvironment() {
    if (System.getenv("GITHUB_WORKFLOW") != null) {
      return true;
    } else if (System.getenv("CI") != null) {
      return true;
    }

    return false;
  }

  public boolean isRunningInLambda() {
    return lambdaSupplier.get();
  }

  Supplier<Boolean> lambdaSupplier =
      Suppliers.memoize(
          () -> {
            return System.getenv("LAMBDA_TASK_ROOT") != null;
          });

  Supplier<Boolean> desktopSupported =
      Suppliers.memoize(
          () -> {
            try {
              return Desktop.isDesktopSupported();
            } catch (Exception e) {
              e.printStackTrace();
            }
            return false;
          });

  public boolean isDesktopSupported() {
    return desktopSupported.get();
  }
}
