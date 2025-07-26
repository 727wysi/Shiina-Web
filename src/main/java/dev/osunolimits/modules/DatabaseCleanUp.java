package dev.osunolimits.modules;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import dev.osunolimits.common.Database;
import dev.osunolimits.common.MySQL;

public class DatabaseCleanUp extends Thread {

    private final Logger logger = (Logger) LoggerFactory.getLogger("DatabaseCleanUp-Web");

    public DatabaseCleanUp() {
        setName("DatabaseCleanUp");
        setDaemon(true);
    }

    @Override
    public void run() {
        while (true) {
            int cleanedUp = 0;
            int trashed = 0;
            for (int i = 0; i < Database.runningConnections.size(); i++) {
                MySQL mysql = Database.runningConnections.get(i);
                if (mysql == null) {
                    Database.runningConnections.remove(i);
                    i--;
                    cleanedUp++;
                    continue;
                }else{
                    long curTime = System.currentTimeMillis();
                    if(mysql.connectionCreated + 600000 < curTime) {
                        try {
                            mysql.close();
                            Database.runningConnections.remove(i);
                            i--;
                            trashed++;
                            continue;
                        } catch (Exception e) {
                            logger.error("Failed to close MySQL connection", e);
                        }
                    }
                }
            }

            if (cleanedUp > 0 || trashed > 0) {
                logger.info("Cleaned up {} and trashed {} MySQL connections", cleanedUp, trashed);
            }

            try {
                Thread.sleep(600000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

    }

}
