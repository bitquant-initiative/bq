package bq.options.model.jm;

import bq.options.OptionModel;
import bq.options.OptionType;

public class BjerksundStenslandModel extends OptionModel {

  @Override
  public Double calcIV() {
    checkIVCalcPrams();
    return BlackLike.priceBjerkStens(type(),stockPrice(),strikePrice(),volatility(),expiration(),rate(),dividendYield());
  }

  @Override
  public Double calcOptionPrice() {
    checkPriceCalcParams();
  

    return BlackLike.priceBjerkStens(type(),stockPrice(),strikePrice(),expiration(),volatility(),rate(),dividendYield());
  }

  @Override
  public Double delta() {
    checkPriceCalcParams();
    return BlackLike.bjDelta(type(),stockPrice(),strikePrice(),volatility(),expiration(),rate(),dividendYield());
  }

  @Override
  public Double gamma() {
    return Double.NaN; // need an implementation
  }

  @Override
  public Double rho() {
    checkPriceCalcParams();
    return Double.NaN; // need an implementation
  }

  @Override
  public Double theta() {
    checkPriceCalcParams();
    return BlackLike.bjTheta(type(),stockPrice(),strikePrice(),volatility(),expiration(),rate(),dividendYield());
  }

  @Override
  public Double vega() {
    checkPriceCalcParams();
    return BlackLike.bjVega(type(),stockPrice(),strikePrice(),volatility(),expiration(),rate(),dividendYield());
  }

  @Override
  public OptionModel copy() {
    BlackScholesModel m = new BlackScholesModel();
    copyAttributes(m);   
    return m;   
  }
}
