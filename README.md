# ProfitTrailer - Binance Auto Blacklist

Automatcally checks for new Binance listings and disables trading in ProfitTrailer.
All newly listed coins will be disabled in your PAIRS.properties file. If you are
using ProfitFeeder, each of your pairs.txt files will be updated.

## Configuration
Modify these settings in the blacklist.properties file:
* enabled = true (enable/disable this tool while it's running)
* market = BTC (the market your bot is currently in)
* days = 14 (automatically disable coins that have only been listed for less than 14 days)
* clear = true (if true, re-enable trading of a newly listed coin once it has been listed for at least the days defined above)
* interval = 30 (interval in minutes to check for new Binance listings)

## Usage
Place ProfitTrailer-blacklist.jar into your ProfitTrailer folder.

Change directory to your ProfitTrailer/Feeder folder and run the jar file:
java -jar ProfitTrailer-blacklist.jar

## If you found this tool useful
LTC: LQYXFHK1exVRP8FbFDh134hCddRXGmWZqn
