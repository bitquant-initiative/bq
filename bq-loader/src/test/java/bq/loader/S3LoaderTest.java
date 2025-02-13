package bq.loader;

import bq.loader.polygon.PolygonDataProvider;
import bq.util.Symbol;
import bq.util.ta4j.Bars;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;

public class S3LoaderTest extends LoaderTest {

  @Test
  public void testKey() {
    Assertions.assertThat(S3Loader.s3Key(Symbol.parse("X:BTC"))).isEqualTo("crypto/1d/BTC.csv");
    Assertions.assertThat(S3Loader.s3Key(Symbol.parse("I:SPX"))).isEqualTo("indices/1d/SPX.csv");
    Assertions.assertThat(S3Loader.s3Key(Symbol.parse("S:MSTR"))).isEqualTo("stocks/1d/MSTR.csv");
    Assertions.assertThat(S3Loader.s3Key(Symbol.parse("Q:BTC.MVRVZ")))
        .isEqualTo("indicators/1d/BTC.MVRVZ.csv");
  }

  @Test
  public void testRead() {

    var s3 = new S3Loader(getDb()).symbol("S:IREN");

    var barSeries = s3.loadAll();

    var polygon = new PolygonDataProvider(getDb()).symbol("S:IREN");

    BarSeries bs = polygon.loadAll();

    System.out.println(barSeries);
    System.out.println(bs);

    System.out.println(Bars.difference(bs, barSeries));
  }
}
