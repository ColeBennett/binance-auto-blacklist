# ProfitTrailer - Binance Auto Blacklist

Automatcally checks for new Binance listings and disables trading in ProfitTrailer.
All newly listed coins will be disabled in your PAIRS.properties file. If you are
using ProfitTrailer Feeder, your appsettings.json file will be updated.

# Configuration
Some settings in the blacklist.properties file:
* enabled = true (enable/disable this tool while it's running)
* market = BTC (the market your bot is currently in)
* days = 14 (automatically disable coins that have only been listed for less than 14 days)
* clear = true (if true, re-enable trading of a newly listed coin once it has been listed for at least the days defined above)
* som = false (if true, set a pair to sell-only mode instead of disabling trading)
* interval = 30 (interval in minutes to check for new Binance listings)

# Usage for PT
* Download the latest zip file from https://github.com/bennettca/binance-auto-blacklist/releases 
* Extract the three files and place them in your ProfitTrailer folder
* Modify the settings if you wish
* If you are on Windows run the ProfitTrailer-blacklist.bat file and keep it open. If you are not on Windows please run the jar file as you normally would run a jar file in your OS. 

# Usage for PTF
* MUST HAVE PTF V1.3.5.320 & UP! If you do not know what version please use the latest from here https://github.com/mehtadone/PTFeeder/releases
* Download the latest zip file from https://github.com/bennettca/binance-auto-blacklist/releases 
* Extract the three files and place them in your ProfitTrailer Feeder folder
* Modify the settings if you wish
* If you are on Windows run the ProfitTrailer-blacklist.bat file and keep it open. If you are not on Windows please run the jar file as you normally would run a jar file in your OS. 

# If you need any help please PM @Cole#4126 on Discord

# If you found the utility useful
LTC: LQYXFHK1exVRP8FbFDh134hCddRXGmWZqn
