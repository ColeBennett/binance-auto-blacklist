# ProfitTrailer-NewBinanceListings

Automatcally checks for new Binance listings and disables trading in ProfitTrailer.
All newly listed coins will be added to your PAIRS.properties file.

## Configuration
Add these settings to your PAIRS.properties file:
* NBL_enabled = true (enable/disable this tool while it's running)
* NBL_days = 14
(automatically disable coins that have only been listed for less than 14 days)
* NBL_clear = true
(if true, re-enable trading of a newly listed coin once it has been listed for at least NBL_days)

## Usage
Place ProfitTrailer-NBL.jar into your ProfitTrailer folder.

Change directory to ProfitTrailer folder and run the jar file.
java -jar ProfitTrailer-NBL.jar

## Example console output
2018-01-26 22:05:59 INFO Set to query https://support.binance.com/hc/en-us/sections/115000106672-New-Listings every 15 minutes
2018-01-26 22:05:59 INFO Fetching data...
2018-01-26 22:06:11 INFO Loaded 23 newest listings (Took 11621 ms)
2018-01-26 22:06:39 INFO Fetching data...
2018-01-26 22:06:39 INFO New Binance listing: IOST (Released 2018-01-24T05:47:09)
