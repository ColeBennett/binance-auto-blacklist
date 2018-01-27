package com.github.bennettca.newbinancelistings;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
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

    public static void main(String[] args) {
        NewBinanceListings app = new NewBinanceListings();
        app.start();
    }

    private final Map<String, LocalDateTime> cache;
    private ScheduledExecutorService scheduler;

    public NewBinanceListings() {
        cache = new LinkedHashMap<>();
    }

    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this, 0, 15, TimeUnit.MINUTES);
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        Document doc = null;
        if (!cache.isEmpty()) cache.clear();
        try {
            doc = Jsoup.connect(URL).get();
            Elements newListings = doc.select(".article-list-link");
            for (Element element : newListings) {
                String title = element.text();
                if (title.contains("Binance Lists")) {
                    String coin = title.replaceAll("(Binance Lists|\\s+)", "");
                    if (coin.matches("\\(A-Z+\\)")) {
                        Matcher m = Pattern.compile("\\(A-Z+\\)").matcher(coin);
                        while (m.find()) {
                            coin = m.group();
                            break;
                        }
                    }

                    String listingUrl = element.absUrl("href");
                    Document listingArticle = Jsoup.connect(listingUrl).get();

                    Element date = listingArticle.selectFirst(".meta-data time");
                    String released = date.attr("datetime");

                    LOGGER.info(String.format("\ntitle: %s\nreleased: %s", coin, released));
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to query " + URL, e);
        }
        if (!cache.isEmpty()) {
            modifyProfitTrailerSettings();
        }

        long elapsed = System.currentTimeMillis() - start;
        LOGGER.info(String.format("Queried %s.\nTook " + elapsed + " ms", URL));
    }

    private void modifyProfitTrailerSettings() {
        File pairsFile = new File("trading", "PAIRS.properties");

        Properties props = new Properties();
        try {
            props.load(new FileInputStream(pairsFile));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load " + pairsFile.getName(), e);
            return;
        }

        String market = props.getProperty("MARKET");
        if (market == null) {
            LOGGER.warning("No market set in " + pairsFile.getName());
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (Entry<String, LocalDateTime> entry : cache.entrySet()) {
            if (Duration.between(now, entry.getValue()).abs().toDays() <= 7) {
                String propsKey = entry.getKey() + market + "_trading_enabled";
                if (!props.containsKey(propsKey)) {
                    props.setProperty(propsKey, "false");
                }
            }
        }

        try {
            props.store(new FileOutputStream(pairsFile), null);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to save " + pairsFile.getName(), e);
        }
    }
}
