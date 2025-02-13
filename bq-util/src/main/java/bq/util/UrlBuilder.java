package bq.util;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class UrlBuilder {

  String protocol = "https";
  String host = "localhost";
  int port = -1;

  Map<String, String> queryParameters = Maps.newHashMap();

  List<String> paths = Lists.newArrayList();

  private UrlBuilder() {}

  public static UrlBuilder create(URL url) {
    Preconditions.checkNotNull(url, "url argumentvcannot be null");
    return create(url.toExternalForm());
  }

  public static UrlBuilder create(URI uri) {
    Preconditions.checkNotNull(uri, "uri argument cannot be null");
    try {
      return create(uri.toURL().toExternalForm());
    } catch (MalformedURLException e) {
      throw new BqException(e);
    }
  }

  public static UrlBuilder create(String urlString) {
    try {
      URL u = URI.create(urlString).toURL();
      UrlBuilder b =
          create().host(u.getHost()).port(u.getPort()).protocol(u.getProtocol()).path(u.getPath());

      if (u.getQuery() != null) {
        Splitter.on("&")
            .omitEmptyStrings()
            .splitToList(u.getQuery())
            .forEach(
                pair -> {
                  List<String> kvlist = Splitter.on('=').omitEmptyStrings().splitToList(pair);
                  if (kvlist.size() == 2) {
                    String k = URLDecoder.decode(kvlist.get(0), Charsets.UTF_8);
                    String v = URLDecoder.decode(kvlist.get(1), Charsets.UTF_8);
                    b.queryParam(k, v);
                  }
                });
      }
      return b;
    } catch (MalformedURLException e) {
      throw new BqException(e);
    }
  }

  public static UrlBuilder create() {
    return new UrlBuilder();
  }

  public List<String> splitPath(String path) {
    return Splitter.on("/").omitEmptyStrings().splitToList(path);
  }

  public UrlBuilder path(String path) {

    List<String> parts =
        splitPath(path).stream().map(s -> URLEncoder.encode(s, Charsets.UTF_8)).toList();
    this.paths.addAll(parts);

    return this;
  }

  public UrlBuilder queryParam(String key, long val) {
    return queryParam(key, Long.toString(val));
  }

  public UrlBuilder queryParam(String key, String val) {
    queryParameters.put(key, val);
    return this;
  }

  public UrlBuilder port(int port) {
    this.port = port;
    return this;
  }

  public UrlBuilder removeQueryParam(String name) {
    if (name != null) {
      this.queryParameters.remove(name);
    }
    return this;
  }

  public UrlBuilder clearQueryParams() {
    this.queryParameters.clear();
    return this;
  }

  public UrlBuilder host(String host) {
    this.host = host;
    return this;
  }

  public String build() {

    StringBuilder sb = new StringBuilder();
    sb.append(protocol);
    sb.append("://");
    sb.append(host);

    String portPart = "";

    if ("https".equalsIgnoreCase(protocol)) {
      if (port == 443 || port < 1) {
        portPart = "";
      } else {
        portPart = String.format(":%s", port);
      }
    } else if ("http".equalsIgnoreCase(protocol)) {
      if (port == 80 || port < 1) {
        portPart = "";
      } else {
        portPart = String.format(":%s", port);
      }
    } else {
      portPart = String.format(":%s", port);
    }

    sb.append(portPart);

    if (!paths.isEmpty()) {
      sb.append("/");
      sb.append(Joiner.on("/").join(paths));
    }

    if (!queryParameters.isEmpty()) {
      StringBuilder qs = new StringBuilder();
      AtomicInteger count = new AtomicInteger();

      queryParameters.forEach(
          (k, v) -> {
            if (count.get() == 0) {
              qs.append("?");
            } else {
              qs.append("&");
            }
            qs.append(
                String.format(
                    "%s=%s",
                    URLEncoder.encode(k, Charsets.UTF_8),
                    URLEncoder.encode(S.notBlank(v).orElse(""), Charsets.UTF_8)));
            count.incrementAndGet();
          });
      sb.append(qs.toString());
    }

    return sb.toString();
  }

  public UrlBuilder truncatePath() {
    paths.clear();
    return this;
  }

  public UrlBuilder https() {
    return protocol("https");
  }

  public UrlBuilder http() {
    return protocol("http");
  }

  public UrlBuilder protocol(String protocol) {
    this.protocol = protocol.toLowerCase();
    return this;
  }

  public String toString() {
    return build();
  }
}
