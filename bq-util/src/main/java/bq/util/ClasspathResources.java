package bq.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Charsets;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class ClasspathResources {

  public static JsonNode asJsonNode(final String name) {
    try {
      return Json.readTree(asByteSource(name).read());
    } catch (IOException e) {
      throw new BqException(e);
    }
  }

  public static ByteSource asByteSource(final String name) {

    return new ByteSource() {

      @Override
      public InputStream openStream() throws IOException {

        String n = name;
        if (!name.startsWith("/")) {
          n = "/" + n;
        }
        InputStream is = getClass().getResourceAsStream(n);
        if (is == null) {
          throw new FileNotFoundException("resource not found: " + n);
        }
        return is;
      }
    };
  }

  public static CharSource asCharSource(final String name) {

    return asByteSource(name).asCharSource(Charsets.UTF_8);
  }
}
