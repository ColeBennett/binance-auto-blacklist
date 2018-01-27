# ProfitTrailer-NewBinanceListings

Automatcally checks for new Binance listings and disables trading in ProfitTrailer.
All newly listed coins less than 14 days old will be added to your PAIRS.properties file.

# Usage
Place ProfitTrailer-NewBinanceListings.jar in your ProfitTrailer folder.

Change directory to ProfitTrailer folder and run the jar file.
java -jar ProfitTrailer-NewBinanceListings.jar

# Example console output
2018-01-26 22:05:59 INFO Set to query https://support.binance.com/hc/en-us/sections/115000106672-New-Listings every 15 minutes
2018-01-26 22:05:59 INFO Fetching data...
2018-01-26 22:06:11 INFO Loaded 23 newest listings (Took 11621 ms)
2018-01-26 22:06:39 INFO Fetching data...
2018-01-26 22:06:39 INFO New Binance listing: IOST (Released 2018-01-24T05:47:09)