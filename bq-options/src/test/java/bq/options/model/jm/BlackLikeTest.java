package bq.options.model.jm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bq.options.OptionModel;
import bq.options.OptionType;
import java.time.Duration;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

public class BlackLikeTest {
  @Test
  public void testBlackCall1() {
    // Result       /  Online calculator
    // ---------------------------------------------
    // 20.037       / https://www.mystockoptions.com/black-scholes.cfm
    // 20.2961      / https://www.erieri.com/blackscholes
    // 20.2961667   / (excel spreadsheet)
    // 20.2961      /
    // http://www.fintools.com/resources/online-calculators/options-calcs/options-calculator/

    double s = 1177.62d;
    double k = 1195.00d;
    double t = 0.084931506849315d; // date 12/19/2017, expiration 1/19/2018, 31 days
    double v = 0.20d;
    double r = 0.0135d;
    double q = 0.0d;
    double bsprice = BlackLike.priceBlackScholes(OptionType.CALL, s, k, t, v, r, q);
    System.out.println("testBlackCall1 bsprice=" + bsprice);

    OptionModel m =
        new BlackScholesModel()
            .type(OptionType.CALL)
            .stockPrice(s)
            .strikePrice(k)
            .expiration(t)
            .volatility(v)
            .rate(r)
            .dividendYield(q);

    assertEquals(20.29616303951127d, bsprice, 0.00000000000d);

    org.assertj.core.api.Assertions.assertThat(m.calcOptionPrice())
        .isCloseTo(bsprice, withinPercentage(.001));

    m.optionPrice(m.calcOptionPrice());
    org.assertj.core.api.Assertions.assertThat(m.calcIV()).isCloseTo(v, withinPercentage(.001d));

    System.out.println(m.calcIV());
    m.volatility(.3);
    System.out.println(m.calcIV());
  }

  @Test
  public void testBlackPut1() {
    // Result       /  Online calculator
    // ---------------------------------------------
    // n/a          / https://www.mystockoptions.com/black-scholes.cfm
    // 0.2708       / https://www.erieri.com/blackscholes
    // ?????        / (excel spreadsheet)
    // 0,2708       /
    // http://www.fintools.com/resources/online-calculators/options-calcs/options-calculator/

    double s = 214.76d;
    double k = 190.00d;
    double t = 0.084931506849315d; // date 12/19/2017, expiration 1/19/2018, 31 days
    double v = 0.25d;
    double r = 0.0135d;
    double q = 0.0d;
    double bsprice = BlackLike.priceBlackScholes(OptionType.PUT, s, k, t, v, r, q);
    System.out.println("testBlackPut1 bsprice=" + bsprice);
    assertEquals(0.2707906395245452d, bsprice, 0.00000000000d);

    OptionModel m =
        new BlackScholesModel()
            .type(OptionType.PUT)
            .stockPrice(s)
            .strikePrice(k)
            .volatility(v)
            .expiration(Duration.ofDays(31))
            .rate(r)
            .dividendYield(q);

    assertThat(m.calcOptionPrice()).isCloseTo(bsprice, withinPercentage(.001d));
  }

  @Test
  public void testBlackImpVol1() {
    double p = 20.29616;
    double s = 1177.62d;
    double k = 1195.00d;
    double t = 0.084931506849315d; // date 12/19/2017, expiration 1/19/2018, 31 days
    double r = 0.0135d;
    double q = 0.0d;
    double v = .5d;
    double bsiv = BlackLike.bsImpliedVol(OptionType.CALL, p, s, k, r, t, v, q);
    System.out.println("testBlackImpVol1 bsiv=" + bsiv);
    assertEquals(0.20d, bsiv, BlackLike.IV_PRECISION);

    var model =
        new BlackScholesModel()
            .type(OptionType.CALL)
            .optionPrice(p)
            .stockPrice(s)
            .strikePrice(k)
            .expiration(t)
            .volatility(v)
            .rate(r)
            .dividendYield(q);

    Assertions.assertThat(model.calcIV()).isCloseTo(0.20d, Offset.offset(BlackLike.IV_PRECISION));
  }

  @Test
  public void testBsCallGreeks() {

    // online calculator comparisons
    // http://www.cboe.com/framed/IVolframed.aspx?content=http%3a%2f%2fcboe.ivolatility.com%2fcalc%2findex.j%3fcontract%3dAE172F0B-BFE3-4A3D-B5A3-6085B2C4F088&sectionName=SEC_TRADING_TOOLS&title=CBOE%20-%20IVolatility%20Services
    // delta = 0.4198, gamma = 0.0057, vega = 1.3414, theta = -0.4505, rho = 0.4027
    // http://www.option-price.com/
    // delta = 0.42, gamma = 0.006, vega = 1.341, theta = -0.45, rho = 0.403
    // http://www.fintools.com/resources/online-calculators/options-calcs/options-calculator/
    // delta = 0.4197, gamma = 0.0057, vega = 1.3413, theta = -0.4502, rho = 0.4026

    double s = 1177.62d;
    double k = 1195.00d;
    double t = 0.084931506849315d; // date 12/19/2017, expiration 1/19/2018, 31 days
    double r = 0.0135d;
    double q = 0.0d;
    double v = 0.20d;
    OptionType type = OptionType.CALL;

    OptionModel model =
        new BlackScholesModel()
            .type(type)
            .stockPrice(s)
            .strikePrice(k)
            .expiration(t)
            .rate(r)
            .dividendYield(q)
            .volatility(v);

    double delta = BlackLike.bsDelta(type, s, k, v, t, r, q);
    double gamma = BlackLike.bsGamma(s, k, v, t, r, q);
    double vega = BlackLike.bsVega(s, k, v, t, r, q);
    double theta = BlackLike.bsTheta(type, s, k, v, t, r, q);
    double rho = BlackLike.bsRho(type, s, k, v, t, r, q);
    System.out.println(
        "testBsCallGreeks"
            + " delta="
            + delta
            + ", gamma="
            + gamma
            + ", vega="
            + vega
            + ", theta="
            + theta
            + ", rho="
            + rho);
    assertEquals(0.41974, delta, 0.0001d);
    assertEquals(0.00569, gamma, 0.0001d);
    assertEquals(1.34134, vega, 0.0001d);
    assertEquals(-0.45022, theta, 0.0001d);
    assertEquals(0.40257, rho, 0.0001d);

    Assertions.assertThat(model.delta()).isEqualTo(delta);
    Assertions.assertThat(model.gamma()).isEqualTo(gamma);
    Assertions.assertThat(model.vega()).isEqualTo(vega);
    Assertions.assertThat(model.theta()).isEqualTo(theta);
    Assertions.assertThat(model.rho()).isEqualTo(rho);
  }

  @Test
  public void testBsPutGreeks() {

    // online calculator comparisons
    // http://www.cboe.com/framed/IVolframed.aspx?content=http%3a%2f%2fcboe.ivolatility.com%2fcalc%2findex.j%3fcontract%3dAE172F0B-BFE3-4A3D-B5A3-6085B2C4F088&sectionName=SEC_TRADING_TOOLS&title=CBOE%20-%20IVolatility%20Services
    // delta = -0.0415, gamma = 0.0057, vega = 0.0556, theta = -0.0221, rho = -0.0078
    // http://www.fintools.com/resources/online-calculators/options-calcs/options-calculator/
    // delta = -0.0415, gamma = 0.0057, vega = 0.0556, theta = -0.0221, rho = -0.0078

    double s = 214.76d;
    double k = 190.00d;
    double t = 0.084931506849315d; // date 12/19/2017, expiration 1/19/2018, 31 days
    double r = 0.0135d;
    double q = 0.0d;
    double v = 0.25d;
    OptionType type = OptionType.PUT;
    double delta = BlackLike.bsDelta(type, s, k, v, t, r, q);
    double gamma = BlackLike.bsGamma(s, k, v, t, r, q);
    double vega = BlackLike.bsVega(s, k, v, t, r, q);
    double theta = BlackLike.bsTheta(type, s, k, v, t, r, q);
    double rho = BlackLike.bsRho(type, s, k, v, t, r, q);
    System.out.println(
        "testBsPutGreeks"
            + " delta="
            + delta
            + ", gamma="
            + gamma
            + ", vega="
            + vega
            + ", theta="
            + theta
            + ", rho="
            + rho);
    assertEquals(-0.04150, delta, 0.0001d);
    assertEquals(0.00567, gamma, 0.0001d);
    assertEquals(0.05557, vega, 0.0001d);
    assertEquals(-0.02206, theta, 0.0001d);
    assertEquals(-0.00780, rho, 0.0001d);

    OptionModel model =
        new BlackScholesModel()
            .type(type)
            .stockPrice(s)
            .strikePrice(k)
            .expiration(t)
            .rate(r)
            .dividendYield(q)
            .volatility(v);

    Assertions.assertThat(model.delta()).isEqualTo(delta);
    Assertions.assertThat(model.gamma()).isEqualTo(gamma);
    Assertions.assertThat(model.vega()).isEqualTo(vega);
    Assertions.assertThat(model.theta()).isEqualTo(theta);
    Assertions.assertThat(model.rho()).isEqualTo(rho);
  }

  @Test
  public void testBjerkStensCall1() {
    // Result       /  Online calculator
    // ---------------------------------------------
    // 19.0638      /
    // http://www.fintools.com/resources/online-calculators/options-calcs/options-calculator/
    // 20.422384    / http://janroman.dhis.org/calc/BjerksundStensland.php
    // 19.082612    / (excel spreadsheet)

    double s = 1177.62d;
    double k = 1195.00d;
    double t = 0.084931506849315d; // date 12/19/2017, expiration 1/19/2018, 31 days
    double v = 0.20d;
    double r = 0.0135d;
    double q = 0.03d;
    double bsprice = BlackLike.priceBjerkStens(OptionType.CALL, s, k, t, v, r, q);
    System.out.println("testBjerkStensCall1 bsprice=" + bsprice);
    assertEquals(19.082618995152643d, bsprice, 0.00000000000d);

    var model =
        new BjerksundStenslandModel()
            .type(OptionType.CALL)
            .stockPrice(s)
            .strikePrice(k)
            .expiration(t)
            .volatility(v)
            .rate(r)
            .dividendYield(q);
    Assertions.assertThat(model.calcOptionPrice()).isEqualTo(bsprice);
  }

  @Test
  public void testBjerkStensPut1() {
    // Result       /  Online calculator
    // ---------------------------------------------
    // 22.0534      /
    // http://www.fintools.com/resources/online-calculators/options-calcs/options-calculator/
    // 20.702770    / http://janroman.dhis.org/calc/BjerksundStensland.php
    // 22.0387792   / (excel spreadsheet)

    double s = 1177.62d;
    double k = 1165.00d;
    double t = 0.084931506849315d; // date 12/19/2017, expiration 1/19/2018, 31 days
    double v = 0.20d;
    double r = 0.0135d;
    double q = 0.03d;
    double bsprice = BlackLike.priceBjerkStens(OptionType.PUT, s, k, t, v, r, q);
    System.out.println("testBjerkStensPut1 bsprice=" + bsprice);
    assertEquals(22.03875264497185d, bsprice, 0.00000000000d);

    var model =
        new BjerksundStenslandModel()
            .type(OptionType.PUT)
            .stockPrice(s)
            .strikePrice(k)
            .expiration(t)
            .volatility(v)
            .rate(r)
            .dividendYield(q);
    Assertions.assertThat(model.calcOptionPrice()).isEqualTo(bsprice);
  }

  @Test
  public void testBjCallGreeks() {
    // ??? did not find an exact equivalent for testing,
    // but assumed to be pretty close to the bs Greeks

    double s = 1177.62d;
    double k = 1195.00d;
    double t = 0.084931506849315d; // date 12/19/2017, expiration 1/19/2018, 31 days
    double r = 0.0135d;
    double q = 0.0d;
    double v = 0.20d;
    OptionType type = OptionType.CALL;
    double delta = BlackLike.bsDelta(type, s, k, v, t, r, q);
    // double gamma = BlackLike.bsGamma(type, s, k, v, t, r, q);
    double vega = BlackLike.bjVega(type, s, k, v, t, r, q);
    double theta = BlackLike.bjTheta(type, s, k, v, t, r, q);
    // double rho = BlackLike.bsRho(type, s, k, v, t, r, q);
    System.out.println(
        "testBjCallGreeks"
            + " delta="
            + delta
            // + ", gamma=" + gamma
            + ", vega="
            + vega
            + ", theta="
            + theta
        // + ", rho=" + rho
        );

    OptionModel model =
        new BjerksundStenslandModel()
            .type(type)
            .stockPrice(s)
            .strikePrice(k)
            .volatility(v)
            .expiration(t)
            .rate(r)
            .dividendYield(q);
    OptionModel bs =
        new BjerksundStenslandModel()
            .type(type)
            .stockPrice(s)
            .strikePrice(k)
            .volatility(v)
            .expiration(t)
            .rate(r)
            .dividendYield(q);

    System.out.println(model);
    System.out.println(bs);
    Assertions.assertThat(model.delta()).isCloseTo(bs.delta(), withinPercentage(.01d));
    Assertions.assertThat(model.vega()).isCloseTo(bs.vega(), withinPercentage(.01d));
    Assertions.assertThat(model.theta()).isCloseTo(bs.theta(), withinPercentage(.01d));
  }

  @Test
  public void testIt() {
    OptionModel bs =
        new BjerksundStenslandModel()
            .type(OptionType.CALL)
            .stockPrice(23)
            .strikePrice(29)
            .volatility(.8)
            .expiration(Duration.ofDays(60))
            .rate(.045)
            .dividendYield(0d);

    System.out.println(bs.calcOptionPrice());
  }
}
