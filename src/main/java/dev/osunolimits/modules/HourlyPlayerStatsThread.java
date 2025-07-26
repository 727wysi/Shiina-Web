package dev.osunolimits.modules;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import dev.osunolimits.api.BanchoStats;
import dev.osunolimits.api.BanchoStats.PlayerCountResponse;
import dev.osunolimits.main.App;

public class HourlyPlayerStatsThread extends Thread {

    private static final Logger log = (Logger) LoggerFactory.getLogger("HourlyPlayerStatsStore");

    private static final String REDIS_KEY = "shiina:hourly:histo";
    private static final long ONE_DAY_SECONDS = 86400;

    private static HourlyPlayerStatsStore store;

    public HourlyPlayerStatsStore getStore() {
        return store;
    }

    public HourlyPlayerStatsThread() {
        setName("HourlyPlayerStatsThread");
        setDaemon(true);
        store = new HourlyPlayerStatsStore();
    }

    @Override
    public void run() {
        // Wait until the next hour boundary before starting collection
        waitUntilNextHour();

        while (!isInterrupted()) {
            // Check for interruption before making API call
            if (isInterrupted()) {
                log.info("HourlyPlayerStatsThread interrupted during loop, shutting down gracefully");
                break;
            }

            BanchoStats stats = new BanchoStats();
            PlayerCountResponse playerCount = stats.getPlayerCount();
            if (playerCount != null && !isInterrupted()) {
                int onlinePlayers = playerCount.getOnline();
                store.addHourlyValue(onlinePlayers);
                log.info("Added hourly player count: {}", onlinePlayers);
            } else if (isInterrupted()) {
                log.info("HourlyPlayerStatsThread interrupted after API call, shutting down gracefully");
                break;
            } else {
                log.warn("Failed to fetch player count from BanchoStats");
            }

            try {
                Thread.sleep(3600000); // Sleep for 1 hour
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        log.info("HourlyPlayerStatsThread stopped");
    }

    private void waitUntilNextHour() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextHour = now.truncatedTo(ChronoUnit.HOURS).plusHours(1);
        long millisecondsUntilNextHour = ChronoUnit.MILLIS.between(now, nextHour);

        log.info("Waiting {} ms until next hour boundary ({})", millisecondsUntilNextHour, nextHour);

        try {
            Thread.sleep(millisecondsUntilNextHour);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public class HourlyPlayerStatsStore {
        public void addHourlyValue(int value) {
            // Round timestamp to the hour boundary to ensure only one entry per hour
            long now = Instant.now().getEpochSecond();
            long hourBoundary = (now / 3600) * 3600; // Round down to hour boundary
            String entry = hourBoundary + ":" + value;

            // Add value with hour boundary timestamp as score
            App.jedisPool.zadd(REDIS_KEY, hourBoundary, entry);

            // Remove entries older than 24 hours (keep exactly 24 hours of data)
            long cutoffTime = hourBoundary - ONE_DAY_SECONDS;
            App.jedisPool.zremrangeByScore(REDIS_KEY, "-inf", String.valueOf(cutoffTime));

            // Additional safety: ensure we don't exceed 24 entries
            long count = App.jedisPool.zcard(REDIS_KEY);
            if (count > 24) {
                App.jedisPool.zremrangeByRank(REDIS_KEY, 0, count - 25); // Keep last 24
            }
            
            log.debug("Stored hourly value {} at timestamp {}. Total entries: {}", value, hourBoundary, App.jedisPool.zcard(REDIS_KEY));
        }

        public List<Map<String, Object>> getLast24Hours() {
            // Get all entries (up to 24 hours)
            List<String> entries = App.jedisPool.zrange(REDIS_KEY, 0, -1);

            return entries.stream().map(entry -> {
                String[] parts = entry.split(":");
                long timestampSec = Long.parseLong(parts[0]);
                int value = Integer.parseInt(parts[1]);

                Map<String, Object> map = new HashMap<>();
                map.put("time", timestampSec * 1000); // convert to milliseconds
                map.put("value", value);
                return map;
            }).collect(Collectors.toList());
        }

        public int getStoredHoursCount() {
            return (int) App.jedisPool.zcard(REDIS_KEY);
        }
    }

}