# ProfitTrailer - Binance Auto Blacklist

Automatcally checks for new Binance listings and enables sell-only mode for pairs in ProfitTrailer.
All newly listed coins will be set to sell-only mode in your PAIRS.properties file. If you are
using PT-Feeder, your SomOnlyPairs in appsettings.json will be updated.

## Configuration
Modify these settings in the blacklist.properties file:
* enabled = true (enable/disable this tool while it's running)
* interval = 5 (interval in minutes to check for new Binance listings)
* market = BTC (the market your bot is currently in)
* days = 14 (automatically disable coins that have only been listed for less than 14 days)
* clear = false (if true, disable sell-only mode for a newly listed coin once it has been listed for at least the days defined above)

## Usage for PT
* Download the latest zip file from https://github.com/bennettca/binance-auto-blacklist/releases
* Extract the three files and place them in your ProfitTrailer folder
* Modify the settings if you wish (restart to update the settings)
* If you are on Windows run the ProfitTrailer-blacklist.bat file and keep it open. If you are not on Windows please run the jar file as you normally would run a jar file in your OS.

## Usage for PT-Feeder
* Download the latest zip file from https://github.com/bennettca/binance-auto-blacklist/releases
* Extract the three files and place them in your ProfitTrailer Feeder folder
* Modify the settings if you wish (restart to update the settings)
* If you are on Windows run the ProfitTrailer-blacklist.bat file and keep it open. If you are not on Windows please run the jar file as you normally would run a jar file in your OS.

### Download the latest release
https://github.com/bennettca/binance-auto-blacklist/releases

### If you found this tool useful
LTC: LQYXFHK1exVRP8FbFDh134hCddRXGmWZqn
