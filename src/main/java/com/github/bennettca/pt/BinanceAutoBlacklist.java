package com.github.bennettca.pt;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.PropertiesConfigurationLayout;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.lang.reflect.Type;
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
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping()
            .setLenient().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>(){}.getType();

    private final File settingsFile = new File("blacklist.properties");
    private final File pairsFile = new File("trading", "PAIRS.properties");
    private final File feederFile = new File("config", "appsettings.json");
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
            LOGGER.info("Most recent:");

            LocalDateTime now = LocalDateTime.now();
            Iterator<Entry<String, LocalDateTime>> itr = cache.entrySet().iterator();

            List<String> sorted = new ArrayList<String>(cache.keySet());
            Iterator<String> li = sorted.iterator();

            for (int i = 0; i < 5; i++) {
                    Entry<String, LocalDateTime> entry = itr.next();
                    long age = Duration.between(now, entry.getValue()).abs().toDays();
                    LOGGER.info("  " + entry.getKey() + " (listed " + age + " days ago)");

            }
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
        LOGGER.info("  enabled  = " + enabled);
        LOGGER.info("  interval = " + currentInterval + " minute"
                + (currentInterval != 1 ? "s" : ""));
        LOGGER.info("  market   = " + market);
        LOGGER.info("  days     = " + days);
        LOGGER.info("  clear    = " + clear);
        if (start) start();
        return config;
    }

    @SuppressWarnings("unchecked")
    private void modifyProfitTrailerSettings() {
        if (!enabled) {
            LOGGER.info("Currently disabled, set 'enabled = true' in blacklist.properties to re-enable");
            return;
        }
        if (cache.isEmpty()) return;
        boolean modifiedPairs = false, modifiedFeeder = false;

        // Load PT PAIRS.properties
        PropertiesConfiguration props = null;
        if (pairsFile.exists()) {
            props = loadPropertiesFile(pairsFile);
        }

        // Load PT-Feeder appsettings.json
        Map<String, Object> appsettings = null;
        Map<String, String> general = null;
        List<String> somPairs = null;
        if (feederFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(feederFile))) {
                appsettings = GSON.fromJson(br, MAP_TYPE);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to load " + feederFile.getPath());
            }
            if (appsettings != null) {
                general = (Map<String, String>) appsettings.get("General");
                if (general != null) {
                    String somData = general.get("SomOnlyPairs");
                    if (somData != null) {
                        somPairs = Arrays.asList(StringUtils.split(general.get("SomOnlyPairs"), ','));
                    } else {
                        LOGGER.warning("SomOnlyPairs not found in " + feederFile.getPath());
                    }
                } else {
                    LOGGER.warning("Could not find General settings in " + feederFile.getPath());
                }
            }
        }

        LocalDateTime now = LocalDateTime.now();
        for (Entry<String, LocalDateTime> entry : cache.entrySet()) {
            String propsKey = entry.getKey() + market + "_sell_only_mode";
            long age = Duration.between(now, entry.getValue()).abs().toDays();
            if (age <= days) {
                if (props != null) {
                    if (checkPair(props, propsKey)) {
                        LOGGER.info("Enabled sell-only mode for " + entry.getKey()
                                + " (Listed " + age + " days ago) in " + pairsFile.getPath());
                    }
                    props.setProperty(propsKey, "true");
                    modifiedPairs = true;
                }
                if (somPairs != null && !somPairs.contains(entry.getKey())) {
                    somPairs.add(entry.getKey());
                    modifiedFeeder = true;
                    LOGGER.info("Enabled sell-only mode for " + entry.getKey()
                            + " (Listed " + age + " days ago) in " + feederFile.getPath());
                }
                continue;
            }
            if (clear) {
                if (props != null && props.containsKey(propsKey)) {
                    if (checkPair(config, propsKey)) {
                        LOGGER.info("Disabled sell-only mode for " + entry.getKey()
                                + " (Listed " + age + " days ago) in " + pairsFile.getPath());
                    }
                    props.clearProperty(propsKey);
                    modifiedPairs = true;
                }
                if (somPairs != null) {
                    somPairs.remove(entry.getKey());
                    modifiedFeeder = true;
                    LOGGER.info("Disabled sell-only mode for " + entry.getKey()
                            + " (Listed " + age + " days ago) in " + feederFile.getPath());
                }
            }
        }

        LOGGER.info("done ");
        if (modifiedPairs) {
            LOGGER.info("saving pairs");
            try (FileWriter fw = new FileWriter(pairsFile, false)) {
                props.write(fw);
            } catch (ConfigurationException | IOException e) {
                LOGGER.log(Level.WARNING, "Failed to save " + pairsFile.getPath(), e);
            }
            LOGGER.info("Saved pairs");
        }

        LOGGER.info("som: " + somPairs);
        if (modifiedFeeder && somPairs != null) {
            general.put("SomOnlyPairs", StringUtils.join(somPairs, ','));
//            try (BufferedWriter bw = new BufferedWriter(new FileWriter(feederFile, false))) {
//                GSON.toJson(appsettings, bw);
//            } catch (IOException e) {
//                LOGGER.log(Level.WARNING, "Failed to save " + feederFile.getPath(), e);
//            }
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
            LOGGER.log(Level.WARNING, "Failed to load " + file.getPath(), e);
        } catch (IOException e) {
            e.printStackTrace();
        }
        config.setLayout(layout);
        return config;
    }
}
