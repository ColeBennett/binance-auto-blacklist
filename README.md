# ProfitTrailer-NewBinanceListings

Automatcally checks for new Binance listings and disables trading in ProfitTrailer.
All newly listed coins will be disabled in your PAIRS.properties file.

## To-Do
* Interval configuration option for fetching updates
* PT-feeder support

## Configuration
Add these settings to your ProfitTrailer PAIRS.properties file:
* NBL_enabled = true (enable/disable this tool while it's running)
* NBL_days = 14
(automatically disable coins that have only been listed for less than 14 days)
* NBL_clear = true
(if true, re-enable trading of a newly listed coin once it has been listed for at least NBL_days)

## Usage
1. Place ProfitTrailer-NBL.jar into your ProfitTrailer folder.
2. Change directory to your ProfitTrailer folder.
3. Run the jar file: java -jar ProfitTrailer-NBL.jar
4. Leave running to continually fetch new updates from the Binance listing article.

## Latest Compiled Jar
https://github.com/bennettca/ProfitTrailer-new-binance-listings/releases
