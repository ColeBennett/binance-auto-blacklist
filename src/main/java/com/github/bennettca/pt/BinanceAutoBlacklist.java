package com.github.bennettca.pt;

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
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BinanceAutoBlacklist implements Runnable {

    public static void main(String[] args) {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$s %5$s%6$s%n");
        new BinanceAutoBlacklist();
    }

    private static final Logger LOGGER = Logger.getLogger(BinanceAutoBlacklist.class.getName());
    private static final String URL = "https://support.binance.com/hc/en-us/sections/115000106672-New-Listings";
    private static final Pattern SYMBOL = Pattern.compile("\\((.*?)\\)");

    private final File settingsFile = new File("blacklist.properties");
    private final File pairsFile = new File("trading", "PAIRS.properties");
    private final PropertiesConfiguration config;
    private final Map<String, LocalDateTime> cache;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> task;
    private int currentInterval;

    /* Settings for blacklist.properties */
    private int interval;
    private String market;
    private int days;
    private boolean enabled, clear;

    public BinanceAutoBlacklist() {
        cache = new HashMap<>();
        scheduler = Executors.newSingleThreadScheduledExecutor();

        config = new PropertiesConfiguration();
        PropertiesConfigurationLayout layout = new PropertiesConfigurationLayout();
        config.setLayout(layout);
        if (!settingsFile.exists()) {
            config.setProperty("enabled", enabled = true);
            config.setProperty("interval", interval = 30);
            config.setProperty("market", market = "BTC");
            config.setProperty("days", days = 14);
            config.setProperty("clear", clear = true);
            try (FileWriter fw = new FileWriter(settingsFile, false)) {
                config.write(fw);
            } catch (ConfigurationException | IOException e) {
                LOGGER.log(Level.WARNING, "Failed to save " + settingsFile.getName(), e);
            }
            LOGGER.info("Created " + settingsFile.getPath());
            currentInterval = interval;
            start();
        } else {
            updateSettings();
        }
        new FileWatcher(this, settingsFile);
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        boolean first = cache.isEmpty();
        int newListingsFound = 0;
        if (enabled) {
            LOGGER.info("Fetching data...");
            try {
                Document doc = Jsoup.connect(URL).get();
                Elements newListings = doc.select(".article-list-link");
                for (Element element : newListings) {
                    String title = element.text();
                    if (title.contains("Binance Lists")) {
                        String coin = title.replace("Binance Lists ", "").trim();
                        Matcher matcher = SYMBOL.matcher(coin);
                        if (matcher.find()) {
                            coin = matcher.group().replaceAll("(\\(|\\))", "");
                        } else if (coin.length() > 5) {
                            continue;
                        }
                        if (cache.containsKey(coin)) continue;

                        // Not cached, so load the release date from the linked article.
                        Document listingArticle = Jsoup.connect(element.absUrl("href")).get();
                        Element dateElement = listingArticle.selectFirst(".meta-data time");
                        LocalDateTime date = LocalDateTime.parse(dateElement.attr("datetime"),
                                DateTimeFormatter.ISO_DATE_TIME);

                        if (!first && !cache.containsKey(coin)) {
                            LOGGER.info("New Binance listing: " + coin + " (Released "
                                    + date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + ")");
                        }
                        cache.put(coin, date);
                        newListingsFound++;
                    }
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to query " + URL, e);
            }
            if (newListingsFound == 0) {
                LOGGER.info("No new listings found");
            } else {
                LOGGER.info("Done");
            }
        }

        if (newListingsFound > 0) {
            long elapsed = System.currentTimeMillis() - start;
            LOGGER.info(String.format("Loaded " + newListingsFound + " newest listings (Took " + elapsed + " ms)", URL));
        }
        if (!cache.isEmpty()) {
            modifyProfitTrailerSettings();
        }
    }

    public void start() {
        if (task != null) {
            task.cancel(true);
        }
        LOGGER.info("Set to query " + URL + " every " + currentInterval + " minutes");
        task = scheduler.scheduleAtFixedRate(this, 0, currentInterval, TimeUnit.MINUTES);
    }

    PropertiesConfiguration updateSettings() {
        loadPropertiesFile(settingsFile, config);
        LOGGER.info("Loaded settings");
        if (config.containsKey("enabled")) {
            enabled = config.getBoolean("enabled");
        }
        if (config.containsKey("market")) {
            market = config.getString("market");
        }
        if (config.containsKey("days")) {
            days = config.getInt("days");
        }
        if (config.containsKey("clear")) {
            clear = config.getBoolean("clear");
        }
        boolean start = false;
        if (config.containsKey("interval")) {
            interval = config.getInt("interval");
            if (interval != currentInterval) {
                currentInterval = interval;
                start = true;
            }
        }
        if (task == null) {
            currentInterval = interval;
            start = true;
        }
        LOGGER.info(" enabled  = " + enabled);
        LOGGER.info(" interval = " + currentInterval + " minutes");
        LOGGER.info(" market   = " + market);
        LOGGER.info(" days     = " + days);
        LOGGER.info(" clear    = " + clear);
        if (start) start();
        return config;
    }

    private void modifyProfitTrailerSettings() {
        if (!enabled) {
            LOGGER.info("Currently disabled, set 'enabled = true' in blacklist.properties to re-enable");
            return;
        }
        if (cache.isEmpty()) return;


        // Load ProfitTrailer/ProfitFeeder pair files.
        Map<File, PropertiesConfiguration> files = new HashMap<>();
        Set<File> modified = new HashSet<>();
        if (pairsFile.exists()) {
            files.put(pairsFile, loadPropertiesFile(pairsFile));
        }

        File feederBaseDir = new File("config");
        if (feederBaseDir.isDirectory()) {
            File[] dirs = feederBaseDir.listFiles();
            if (dirs != null) {
                for (File dir : dirs) {
                    if (!dir.isDirectory()) continue;
                    File feederFile = new File(dir, "pairs.txt");
                    if (feederFile.exists()) {
                        files.put(feederFile, loadPropertiesFile(feederFile));
                    }
                }
            }
        }

        LocalDateTime now = LocalDateTime.now();
        for (Entry<String, LocalDateTime> entry : cache.entrySet()) {
            String propsKey = entry.getKey() + market + "_trading_enabled";
            long age = Duration.between(now, entry.getValue()).abs().toDays();
            if (age <= days) {
                for (Entry<File, PropertiesConfiguration> ent : files.entrySet()) {
                    if (checkPair(ent.getValue(), propsKey)) {
                        LOGGER.info("Disabled trading for " + entry.getKey()
                                + " (Listed " + age + " days ago) in " + ent.getKey().getPath());
                    }
                    ent.getValue().setProperty(propsKey, "false");
                    modified.add(ent.getKey());
                }
                continue;
            }
            for (Entry<File, PropertiesConfiguration> ent : files.entrySet()) {
                if (clear && ent.getValue().containsKey(propsKey)) {
                    if (ent.getValue().containsKey(propsKey)) {
                        if (checkPair(config, propsKey)) {
                            LOGGER.info("Enabled trading for " + entry.getKey()
                                    + " (Listed " + age + " days ago) in " + ent.getKey().getPath());
                        }
                        ent.getValue().clearProperty(propsKey);
                        modified.add(ent.getKey());
                    }
                }
            }
        }

        for (Entry<File, PropertiesConfiguration> entry : files.entrySet()) {
            if (!modified.contains(entry.getKey())) continue;
            try (FileWriter fw = new FileWriter(entry.getKey(), false)) {
                entry.getValue().write(fw);
            } catch (ConfigurationException | IOException e) {
                LOGGER.log(Level.WARNING, "Failed to save " + entry.getKey().getName(), e);
            }
        }
    }

    private boolean checkPair(PropertiesConfiguration config, String propsKey) {
        return !config.containsKey(propsKey) || (config.containsKey(propsKey) && config.getBoolean(propsKey));
    }

    private PropertiesConfiguration loadPropertiesFile(File file) {
        return loadPropertiesFile(file, new PropertiesConfiguration());
    }

    private PropertiesConfiguration loadPropertiesFile(File file, PropertiesConfiguration config) {
        PropertiesConfigurationLayout layout = new PropertiesConfigurationLayout();
        try (InputStreamReader is = new InputStreamReader(new FileInputStream(file))) {
            layout.load(config, is);
        } catch (ConfigurationException | FileNotFoundException e) {
            LOGGER.log(Level.WARNING, "Failed to load " + file.getName(), e);
        } catch (IOException e) {
            e.printStackTrace();
        }
        config.setLayout(layout);
        return config;
    }
}
