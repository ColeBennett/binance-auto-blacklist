package com.github.bennettca.newbinancelistings;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.PropertiesConfigurationLayout;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewBinanceListings implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(NewBinanceListings.class.getName());
    private static final String URL = "https://support.binance.com/hc/en-us/sections/115000106672-New-Listings";
    private static final Pattern SYMBOL = Pattern.compile("\\((.*?)\\)");

    public static void main(String[] args) {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$s %5$s%6$s%n");
        NewBinanceListings app = new NewBinanceListings();
        app.start();
    }

    private final Map<String, LocalDateTime> cache;
    private ScheduledExecutorService scheduler;

    public NewBinanceListings() {
        cache = new HashMap<>();
    }

    public void start() {
        LOGGER.info("Set to query " + URL + " every 15 minutes");
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this, 0, 40, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        boolean first = cache.isEmpty();
        LOGGER.info("Fetching data...");
        try {
            Document doc = Jsoup.connect(URL).get();
            Elements newListings = doc.select(".article-list-link");
            for (Element element : newListings) {
                String title = element.text();
                if (title.contains("Binance Lists")) {
                    String coin = title.replace("Binance Lists ", "").trim();

                    if (coin.contains("IOS") && first) continue;

                    Matcher matcher = SYMBOL.matcher(coin);
                    if (matcher.find()) {
                        coin = matcher.group().replaceAll("(\\(|\\))", "");
                    } else if (coin.length() > 5) {
                        continue;
                    }
                    if (cache.containsKey(coin)) {
                        continue;
                    }

                    Document listingArticle = Jsoup.connect(element.absUrl("href")).get();
                    Element dateElement = listingArticle.selectFirst(".meta-data time");
                    LocalDateTime date = LocalDateTime.parse(dateElement.attr("datetime"),
                            DateTimeFormatter.ISO_DATE_TIME);

                    if (!first && !cache.containsKey(coin)) {
                        LOGGER.info("New Binance listing: " + coin + " (Released "
                                + date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + ")");
                    }
                    cache.put(coin, date);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to query " + URL, e);
        }

        if (!cache.isEmpty()) {
            modifyProfitTrailerSettings();
        }

        long elapsed = System.currentTimeMillis() - start;
        if (first) {
            LOGGER.info(String.format("Loaded " + cache.size() + " newest listings (Took " + elapsed + " ms)", URL));
        }
    }

    private void modifyProfitTrailerSettings() {
        File pairsFile = new File("trading", "PAIRS.properties");
        PropertiesConfiguration config = new PropertiesConfiguration();
        PropertiesConfigurationLayout layout = new PropertiesConfigurationLayout();

        try {
            layout.load(config, new InputStreamReader(new FileInputStream(pairsFile)));
        } catch (ConfigurationException | FileNotFoundException e) {
            LOGGER.log(Level.WARNING, "Failed to load " + pairsFile.getName(), e);
        }
        config.setLayout(layout);

        if (!config.containsKey("MARKET")) {
            LOGGER.warning("No market set in " + pairsFile.getName());
            return;
        }
        String market = (String) config.getProperty("MARKET");

        LocalDateTime now = LocalDateTime.now();
        for (Entry<String, LocalDateTime> entry : cache.entrySet()) {
            if (Duration.between(now, entry.getValue()).abs().toDays() <= 14) {
                String propsKey = entry.getKey() + market + "_trading_enabled";
                if (!config.containsKey(propsKey)) {
                    config.setProperty(propsKey, "false");
                }
            }
        }

        try {
            config.write(new FileWriter(pairsFile, false));
        } catch (ConfigurationException | IOException e) {
            LOGGER.log(Level.WARNING, "Failed to save " + pairsFile.getName(), e);
        }
    }
}
