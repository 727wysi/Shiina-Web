package dev.osunolimits.modules;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

import com.google.gson.Gson;

import lombok.AllArgsConstructor;
import lombok.Data;

public class ShiinaAchievementsSorter {
    private Gson gson = new Gson();

    public ShiinaAchievementsSorter() throws IOException {
        if(new File("data/achievements.json").exists()) {
            String json = Files.readString(new File("data/achievements.json").toPath());
            
            AchievementConnection data = gson.fromJson(json, AchievementConnection.class);
           
            return;
        } else {
            System.out.println("Achievements file not found.");

        }

        ArrayList<AchievementConnection> connections = new ArrayList<>();

    }

    @Data
    @AllArgsConstructor
    public class AchievementConnection{
        public int achievementId;
        public int row;
        public String category;
    }
}
