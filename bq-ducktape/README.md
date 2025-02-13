![logo](.img/bq-icon-5.png)

# Duck Tape

[![Maven Central Version](https://img.shields.io/maven-central/v/io.github.bitquant-initiative/ducktape)](https://central.sonatype.com/artifact/io.github.bitquant-initiative/ducktape)

Duck Tape makes it simple to process market data by integrating DuckDB with TA4J.

![logo](.img/duckdb-48x48.png) [DuckDB](https://duckdb.org) is a fantastic analytics database engine that has no footprint.

![logo](.img/ta4j-48x48.png) [TA4J](https://github.com/ta4j/ta4j?tab=readme-ov-file#ta4j-------) is a library that includes 100+ technical indicators for analyzing 
market data.

Ducktape combines both tools so that you can apply market indicators to relational market data with just a single line!

And for perhaps first time ever, "Duck Tape" is used correctly. This is not "duct tape" it is DuckDB operating on ticker tape data!

# Usage

## First Steps

1. Create a DuckTape instance with an in-memory DuckDB database:

```java
DuckTape tape = DuckTape.createInMemory();
```

2. Load some market data from CSV:

```java
BarSeriesTable table = tape.importTable("btc", "./btc.csv");
```
Alternatively you can operate on an existing table that you loaded yourself.  Ducktape 
wants it to contain the following columns: `date`, `open`, `high`, `low`, `close`, `volume`.

3. Load the data into a TA4J BarSeries object:
   
```java
BarSeries barSeries = table.getBarSeries();
```

You can use this BarSeries like any other. The key difference is that it is immutable. You cannot
load new bars. That's OK...just modify the table and reload the bar series.

Note: `getBarSeries()` actually performs a SELECT statement and loads the data into the BarSeries object. This means that
if you are repeatedly accessing the BarSeries, you should avoid repeated calls to `getBarSeries()`.

## Adding Indicators

Ducktape makes it very easy to add indicators using a simple expression syntax.  Under the hood ducktape will:

1. Alter the table to add the column
2. Iterate through the data in the table and write the values to the new column

The following will write the 20-period SMA to a column named `sma_20`:

```java
table.addIndicator("sma(20)","sma_20");
```

Alternately you can provide the column name as part of the indicator expression, by appending "as column_name" as follows:

```
table.addIndicator("sma(20) as sma_20");
```

If you don't provide an explicit name, the name of the indicator "function" will be used. The following 
will create a column named "sma":
```
table.addIndicator("sma(20)");
```

The expressions used above are a concise declarative way of constructing an indicator or set of indicators. The
following programmatic approach is functionally identical:

```
table.addIndicator( new SMAIndicator(new ClosePriceIndicator(table.getBarSeries()), 20), "sma");
```

## Fetching Columns As Indicators

You can choose a column and use it as an indicator:

```java
// Load the "close" column as an indicator:
Indicator<Num> closeIndicator = table.getIndicator("close");

// which is functionally identical to:
Indicator<Num> closeIndicator == new ClosePriceIndicator(table.getBarSeries());
```

## Referencing Columns in Expressions

TA4J Indicators typically take either `BarSeries` or `Indicator` as inputs to their constructors.

When ducktape invokes the Indicator constructors to build the indicators, it will
automatically inject the BarSeries for the able being modified.

Indicator parameters may be explicit or implicit. The following expressions are functionally identical:

* `sma(20)`
* `sma(close, 20)

As a bit of syntactic sugar to keep the expressions simple, ducktape will automatically inject an Indicator 
for the `close` price if you don't specify one.  If you want to use another column, just specify the other column.

For example, the following will compute the SMA on the `high` price:

```
sma(high,20)
```
Note: If the constructor parameters on the Indicator call for more than one indicator, `close` with *NOT* be added 
implicity.  You must explicitly specify each column as input.

## Indicator Functions

In general, TA4J Indicators are mapped into short function names with a mechanical mapping.

Indicator Class Name -> Simple Name -> Remove "Indicator" -> Lower Underbar

`org.ta4j.core.indicators.volume.ChaikinMoneyFlowIndicator` -> `ChaikinMoneyFlowIndicator` -> `ChaikinMoneyFlow` -> `chaikin_money_flow`

A full reference can be found below.

## Access to DuckDB

Ducktape also provides a fluent API for interacting with DuckDB using JDBC. This can be accessed using 
the [Duck](https://github.com/bitquant-initiative/ducktape/blob/main/src/main/java/bq/duckdb/DuckDb.java) class.

It can be obtained from the DuckTape instance:

DuckDb db = tape.getDb();

Statements can be executed easily:

```
 db.sqlTemplate().execute("truncate table foo)");
```

Queries are simple and fluent. It is implemented with a thin wrapper around JDBC objects which remove checked SQLException, 
improve null handling with Optional and improve date/time handling.

The following one-liner returns the list of dates where BTC closes above 50000:
```
  List<LocalDate> dates = db.sqlTemplate().query(c->c.sql("select date from btc where close>{{price}}}",50000),rs->{
      return rs.getLocalDate("date").get();
    });
```

Note the use of `{{param}}` style bind params.  This is much easier than using JDBC `?` positional parameters.  The `{{}}` style parameters
can be positional (as above) or named (as follows, which is functionally identical:

```java
    List<LocalDate> dates = db.sqlTemplate().query(c->{
      c.sql("select date from btc where close>{{price}}}");
      c.bind("price",50000);
    }    
      ,rs->{
      return rs.getLocalDate("date").get();
    });
```

Updates operate similarly.

## Creation and Lifecycle

Much of the time using DuckTape, you will probably want to be using a transient or in-memory DB. 

The following will create an in-memory database:
```
DuckTape tape = DuckTape.createInMemory();
```

IMPORTANT: Each call to DuckTape.createInMemory will create a DIFFERENT database, with an entirely different set of tables and data.

This is consistent with how DuckDB itself works.  The following will create two connections to two different databases:

```
var c0 = DriverManager.getConnection("jdbc:duckdb:");
var c1 = DriverManager.getConnection("jdbc:duckdb:");

Also remember to call `DuckTape.dispose()` when you are done with the database. Resources will be freed and all data will be lost.

If you want to share a single in-memory database that can be shared for the duration of the process:

```
DuckTape shared = DuckTape.getSharedInMemory();

// This will used the *SAME* database:

DuckTape sameDb = DuckTape.getSharedInMemory();
```

There are a variety of other options for opening databases:

```
// open by specifying a file:
DuckTape.create(new File("./mydb.db"));
```

If you want to use an externally created connection:

```
Connection c = DriverManager.getConnection("jdbc:duckdb:mydb.db");
DuckTape.create(c);
```

### Connection Lifecycle

DuckDB JDBC behaivor is slightly different than with most databases.  Calling Connection.close() will close the databbase, which, if it is in-memory
will lose all the data, which can be confusing.

To deal with this, Ducktape wraps the connection and turns `close()` into a no-op.

NOTE: It seems like this ought to be an option on the DuckDBConnection itself.  If it is, we can remove the connection wrapper implementation.

## Cleanup
```java
// DuckTape.close() will close the underlying DuckDB Connection. 
tape.close();
```

Simple!

# Why DuckTape?

TA4J has a wonderful set of indicators, but it can be cumbersome to get data in and out of 
its object model. I found that I was writing too much loading, parsing and data-transformation code 
just to perform a simple indicator computation.

DuckDB is really fantastic and has excellent cross-platform support.

By using DuckDB as the backing store for TA4J's indicator code, most of the messy code disappears.

# Assumptions and Limitations

* TA4J DoubleNum is the only supported Num type. 
* DuckTape expects BarSeries and Bar usage to be immutable. Adding and deleteing bars
is not supported.
* Only daily (1-day) time periods are supported. (I may add support for 1-hour and other time periods,
  but I don't use them, so it is not a priority for me.

# Reference

## Table Structure

DuckTape expects to operate on tables with the following structure:

|    date    |   open    |   high    |    low    |   close   |     volume     |
|------------|----------:|----------:|----------:|----------:|---------------:|
| 2025-01-18 | 104107.0  | 104933.15 | 102233.45 | 104435.0  | 7835.2994471   |
| 2025-01-19 | 104435.01 | 106314.44 | 99518.0   | 101211.13 | 13312.63685598 |
| 2025-01-20 | 101217.78 | 109358.01 | 99416.27  | 102145.43 | 32342.18311338 |
| 2025-01-21 | 102145.42 | 107291.1  | 100051.0  | 106159.26 | 19411.23488978 |

Note: In the future, we may allow columns with alternate names or some of the columns to be missing.

## Indicator Functions

| name | TA4J Indicator |
|------|------|
| acceleration_deceleration | [AccelerationDecelerationIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/AccelerationDecelerationIndicator.java) |
| accumulation_distribution | [AccumulationDistributionIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/volume/AccumulationDistributionIndicator.java) |
| adx | [ADXIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/adx/ADXIndicator.java) |
| amount | [AmountIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/helpers/AmountIndicator.java) |
| aroon_down | [AroonDownIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/aroon/AroonDownIndicator.java) |
| aroon_oscillator | [AroonOscillatorIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/aroon/AroonOscillatorIndicator.java) |
| aroon_up | [AroonUpIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/aroon/AroonUpIndicator.java) |
| atr | [ATRIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/ATRIndicator.java) |
| awesome_oscillator | [AwesomeOscillatorIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/AwesomeOscillatorIndicator.java) |
| bearish_engulfing | [BearishEngulfingIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/candles/BearishEngulfingIndicator.java) |
| bearish_harami | [BearishHaramiIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/candles/BearishHaramiIndicator.java) |
| bollinger_bands_middle | [BollingerBandsMiddleIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/bollinger/BollingerBandsMiddleIndicator.java) |
| bullish_engulfing | [BullishEngulfingIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/candles/BullishEngulfingIndicator.java) |
| bullish_harami | [BullishHaramiIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/candles/BullishHaramiIndicator.java) |
| cci | [CCIIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/CCIIndicator.java) |
| chaikin_money_flow | [ChaikinMoneyFlowIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/volume/ChaikinMoneyFlowIndicator.java) |
| chaikin_oscillator | [ChaikinOscillatorIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/volume/ChaikinOscillatorIndicator.java) |
| chandelier_exit_long | [ChandelierExitLongIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/ChandelierExitLongIndicator.java) |
| chandelier_exit_short | [ChandelierExitShortIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/ChandelierExitShortIndicator.java) |
| chop | [ChopIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/ChopIndicator.java) |
| close_location_value | [CloseLocationValueIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/helpers/CloseLocationValueIndicator.java) |
| close_price | [ClosePriceIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/helpers/ClosePriceIndicator.java) |
| close_price_difference | [ClosePriceDifferenceIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/helpers/ClosePriceDifferenceIndicator.java) |
| close_price_ratio | [ClosePriceRatioIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/helpers/ClosePriceRatioIndicator.java) |
| cmo | [CMOIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/CMOIndicator.java) |
| coppock_curve | [CoppockCurveIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/CoppockCurveIndicator.java) |
| correlation_coefficient | [CorrelationCoefficientIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/statistics/CorrelationCoefficientIndicator.java) |
| covariance | [CovarianceIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/statistics/CovarianceIndicator.java) |
| cross | [CrossIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/helpers/CrossIndicator.java) |
| date_time | [DateTimeIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/helpers/DateTimeIndicator.java) |
| de_mark_pivot_point | [DeMarkPivotPointIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/pivotpoints/DeMarkPivotPointIndicator.java) |
| difference_percentage | [DifferencePercentageIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/helpers/DifferencePercentageIndicator.java) |
| distance_from_ma | [DistanceFromMAIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/DistanceFromMAIndicator.java) |
| doji | [DojiIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/candles/DojiIndicator.java) |
| donchian_channel_lower | [DonchianChannelLowerIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/donchian/DonchianChannelLowerIndicator.java) |
| donchian_channel_middle | [DonchianChannelMiddleIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/donchian/DonchianChannelMiddleIndicator.java) |
| donchian_channel_upper | [DonchianChannelUpperIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/donchian/DonchianChannelUpperIndicator.java) |
| double_ema | [DoubleEMAIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/DoubleEMAIndicator.java) |
| down_trend | [DownTrendIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/trend/DownTrendIndicator.java) |
| dpo | [DPOIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/DPOIndicator.java) |
| dx | [DXIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/adx/DXIndicator.java) |
| ema | [EMAIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/EMAIndicator.java) |
| fisher | [FisherIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/FisherIndicator.java) |
| gain | [GainIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/helpers/GainIndicator.java) |
| hammer | [HammerIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/candles/HammerIndicator.java) |
| hanging_man | [HangingManIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/candles/HangingManIndicator.java) |
| high_price | [HighPriceIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/helpers/HighPriceIndicator.java) |
| highest_value | [HighestValueIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/helpers/HighestValueIndicator.java) |
| hma | [HMAIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/HMAIndicator.java) |
| ichimoku_chikou_span | [IchimokuChikouSpanIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/ichimoku/IchimokuChikouSpanIndicator.java) |
| ichimoku_kijun_sen | [IchimokuKijunSenIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/ichimoku/IchimokuKijunSenIndicator.java) |
| ichimoku_line | [IchimokuLineIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/ichimoku/IchimokuLineIndicator.java) |
| ichimoku_senkou_span_a | [IchimokuSenkouSpanAIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/ichimoku/IchimokuSenkouSpanAIndicator.java) |
| ichimoku_senkou_span_b | [IchimokuSenkouSpanBIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/ichimoku/IchimokuSenkouSpanBIndicator.java) |
| ichimoku_tenkan_sen | [IchimokuTenkanSenIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/ichimoku/IchimokuTenkanSenIndicator.java) |
| iii | [IIIIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/volume/IIIIndicator.java) |
| intra_day_momentum_index | [IntraDayMomentumIndexIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/IntraDayMomentumIndexIndicator.java) |
| inverted_hammer | [InvertedHammerIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/candles/InvertedHammerIndicator.java) |
| kalman_filter | [KalmanFilterIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/KalmanFilterIndicator.java) |
| kama | [KAMAIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/KAMAIndicator.java) |
| keltner_channel_middle | [KeltnerChannelMiddleIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/keltner/KeltnerChannelMiddleIndicator.java) |
| kst | [KSTIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/KSTIndicator.java) |
| loss | [LossIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/helpers/LossIndicator.java) |
| low_price | [LowPriceIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/helpers/LowPriceIndicator.java) |
| lower_shadow | [LowerShadowIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/candles/LowerShadowIndicator.java) |
| lowest_value | [LowestValueIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/helpers/LowestValueIndicator.java) |
| lwma | [LWMAIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/LWMAIndicator.java) |
| macd | [MACDIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/MACDIndicator.java) |
| mass_index | [MassIndexIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/MassIndexIndicator.java) |
| mean_deviation | [MeanDeviationIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/statistics/MeanDeviationIndicator.java) |
| median_price | [MedianPriceIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/helpers/MedianPriceIndicator.java) |
| minus_di | [MinusDIIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/adx/MinusDIIndicator.java) |
| minus_dm | [MinusDMIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/adx/MinusDMIndicator.java) |
| mma | [MMAIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/MMAIndicator.java) |
| money_flow_index | [MoneyFlowIndexIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/volume/MoneyFlowIndexIndicator.java) |
| nvi | [NVIIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/volume/NVIIndicator.java) |
| on_balance_volume | [OnBalanceVolumeIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/volume/OnBalanceVolumeIndicator.java) |
| open_price | [OpenPriceIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/helpers/OpenPriceIndicator.java) |
| parabolic_sar | [ParabolicSarIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/ParabolicSarIndicator.java) |
| pearson_correlation | [PearsonCorrelationIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/statistics/PearsonCorrelationIndicator.java) |
| percent_b | [PercentBIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/bollinger/PercentBIndicator.java) |
| periodical_growth_rate | [PeriodicalGrowthRateIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/statistics/PeriodicalGrowthRateIndicator.java) |
| pivot_point | [PivotPointIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/pivotpoints/PivotPointIndicator.java) |
| plus_di | [PlusDIIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/adx/PlusDIIndicator.java) |
| plus_dm | [PlusDMIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/adx/PlusDMIndicator.java) |
| ppo | [PPOIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/PPOIndicator.java) |
| previous_value | [PreviousValueIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/helpers/PreviousValueIndicator.java) |
| pvi | [PVIIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/volume/PVIIndicator.java) |
| pvo | [PVOIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/PVOIndicator.java) |
| ravi | [RAVIIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/RAVIIndicator.java) |
| real_body | [RealBodyIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/candles/RealBodyIndicator.java) |
| recent_swing_high | [RecentSwingHighIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/RecentSwingHighIndicator.java) |
| recent_swing_low | [RecentSwingLowIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/RecentSwingLowIndicator.java) |
| relative_volume_standard_deviation | [RelativeVolumeStandardDeviationIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/volume/RelativeVolumeStandardDeviationIndicator.java) |
| roc | [ROCIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/ROCIndicator.java) |
| rocv | [ROCVIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/volume/ROCVIndicator.java) |
| rsi | [RSIIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/RSIIndicator.java) |
| running_total | [RunningTotalIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/helpers/RunningTotalIndicator.java) |
| rwihigh | [RWIHighIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/RWIHighIndicator.java) |
| rwilow | [RWILowIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/RWILowIndicator.java) |
| shooting_star | [ShootingStarIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/candles/ShootingStarIndicator.java) |
| sigma | [SigmaIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/statistics/SigmaIndicator.java) |
| simple_linear_regression | [SimpleLinearRegressionIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/statistics/SimpleLinearRegressionIndicator.java) |
| sma | [SMAIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/SMAIndicator.java) |
| squeeze_pro | [SqueezeProIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/SqueezeProIndicator.java) |
| standard_deviation | [StandardDeviationIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/statistics/StandardDeviationIndicator.java) |
| standard_error | [StandardErrorIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/statistics/StandardErrorIndicator.java) |
| stochastic_oscillator_d | [StochasticOscillatorDIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/StochasticOscillatorDIndicator.java) |
| stochastic_oscillator_k | [StochasticOscillatorKIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/StochasticOscillatorKIndicator.java) |
| stochastic_rsi | [StochasticRSIIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/StochasticRSIIndicator.java) |
| super_trend | [SuperTrendIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/supertrend/SuperTrendIndicator.java) |
| super_trend_lower_band | [SuperTrendLowerBandIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/supertrend/SuperTrendLowerBandIndicator.java) |
| super_trend_upper_band | [SuperTrendUpperBandIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/supertrend/SuperTrendUpperBandIndicator.java) |
| three_black_crows | [ThreeBlackCrowsIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/candles/ThreeBlackCrowsIndicator.java) |
| three_white_soldiers | [ThreeWhiteSoldiersIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/candles/ThreeWhiteSoldiersIndicator.java) |
| time_segmented_volume | [TimeSegmentedVolumeIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/volume/TimeSegmentedVolumeIndicator.java) |
| tr | [TRIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/helpers/TRIndicator.java) |
| trade_count | [TradeCountIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/helpers/TradeCountIndicator.java) |
| triple_ema | [TripleEMAIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/TripleEMAIndicator.java) |
| typical_price | [TypicalPriceIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/helpers/TypicalPriceIndicator.java) |
| ulcer_index | [UlcerIndexIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/UlcerIndexIndicator.java) |
| unstable | [UnstableIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/helpers/UnstableIndicator.java) |
| up_trend | [UpTrendIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/trend/UpTrendIndicator.java) |
| upper_shadow | [UpperShadowIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/candles/UpperShadowIndicator.java) |
| variance | [VarianceIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/statistics/VarianceIndicator.java) |
| volume | [VolumeIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/helpers/VolumeIndicator.java) |
| vwap | [VWAPIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/volume/VWAPIndicator.java) |
| williams_r | [WilliamsRIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/WilliamsRIndicator.java) |
| wma | [WMAIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/WMAIndicator.java) |
| zlema | [ZLEMAIndicator](https://github.com/ta4j/ta4j/tree/0.17/ta4j-core/src/main/java/org/ta4j/core/indicators/ZLEMAIndicator.java) |
