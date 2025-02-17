package bq.options.model.jm;

import com.google.common.base.Preconditions;

import bq.options.OptionModel;

public class BlackScholesModel extends OptionModel {


  @Override
  public Double calcIV() {
   
    checkIVCalcPrams();
        
    return BlackLike.bsImpliedVol(type(), optionPrice(), stockPrice(), strikePrice(), rate(), expiration(), volatility(),
        dividendYield());
  }

  @Override
  public Double calcOptionPrice() {
    checkPriceCalcParams();
    return BlackLike.priceBlackScholes(type(), stockPrice(), strikePrice(), expiration(), volatility(), rate(),
        dividendYield());

  }

  @Override
  public Double delta() {
    checkPriceCalcParams();
    return BlackLike.bsDelta(type(), stockPrice(), strikePrice(), volatility(), expiration(), rate(),
        dividendYield());

  }

  @Override
  public Double gamma() {
    
    checkPriceCalcParams();
    return BlackLike.bsGamma(stockPrice(), strikePrice(), volatility(), expiration(), rate(),
        dividendYield());

  }

  @Override
  public Double rho() {
    checkPriceCalcParams();
    return BlackLike.bsRho(type(),stockPrice(), strikePrice(), volatility(), expiration(), rate(),
        dividendYield());

  }

  @Override
  public Double theta() {
    checkPriceCalcParams();
    return BlackLike.bsTheta(type(), stockPrice(), strikePrice(), volatility(), expiration(), rate(),
        dividendYield());
  }

  @Override
  public Double vega() {
    checkPriceCalcParams();
    return BlackLike.bsVega(stockPrice(), strikePrice(), volatility(), expiration(), rate(),
        dividendYield());
  }

  @Override
  public OptionModel copy() {
    BlackScholesModel m = new BlackScholesModel();
    copyAttributes(m);
    
    return m;
    
  }


}
