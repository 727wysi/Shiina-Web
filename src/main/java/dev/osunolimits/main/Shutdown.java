package dev.osunolimits.main;

import java.util.ArrayList;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import dev.osunolimits.common.Database;
import dev.osunolimits.common.MySQL;
import spark.Spark;

public class Shutdown extends Thread {

    public static final Logger log = (Logger) LoggerFactory.getLogger("Shutdown");

    @Override
    public void run() {
        log.info("Starting shutdown process...");
        long startTime = System.currentTimeMillis();

        if (App.rankCache != null) {
            log.info("Shutting down rank cache scheduler...");
            App.rankCache.shutdown();
        }

        if (App.multiDetection != null) {
            log.info("Shutting down multi-detection scheduler...");
            App.multiDetection.shutdown();
        }

        if(App.databaseCleanUp != null) {
            log.info("Shutting down database cleanup thread...");
            App.databaseCleanUp.interrupt();
        }

        if(App.hourlyPlayerStatsThread != null) {
            log.info("Shutting down hourly player stats thread...");
            App.hourlyPlayerStatsThread.interrupt();
        }

        log.info("Closing active MySQL connections (" + Database.runningConnections.size() + " connections)...");
        for (MySQL connection : new ArrayList<>(Database.runningConnections)) {
            try {
                connection.close();
            } catch (Exception e) {
                log.warn("Error closing MySQL connection: " + e.getMessage());
            }
        }

        if (Database.dataSource != null && !Database.dataSource.isClosed()) {
            log.info("Closing HikariCP data source...");
            Database.dataSource.close();
        }

        if (App.jedisPool != null) {
            log.info("Closing Redis connection pool...");
            App.jedisPool.close();
        }

        if (App.webServer != null) {
            log.info("Stopping web server...");
            Spark.awaitStop();
            App.webServer.shutdown();
            App.webServer = null;
        }

        // Force JVM to perform garbage collection to release any lingering resources
        System.gc();

        // Additional delay to ensure all resources are properly cleaned up
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("Shutdown completed successfully in " + (System.currentTimeMillis() - startTime) + " ms.");
    }
}
