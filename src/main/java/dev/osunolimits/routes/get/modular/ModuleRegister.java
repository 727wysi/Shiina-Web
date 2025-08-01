package dev.osunolimits.routes.get.modular;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import ch.qos.logback.classic.Logger;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class ModuleRegister {

    private static final List<ModulePath> modules = new ArrayList<>();
    private static final Map<String, List<ShiinaModule>> loadedModules = new HashMap<>();
    private static final Logger log = (Logger) LoggerFactory.getLogger("ModuleRegister");

    public static void registerDefaultModule(String forPage, ShiinaModule module) {
        ModulePath modulePath = new ModulePath(forPage, module, true);
        modules.add(modulePath);
        log.info("Registered default module for " + forPage + ": " + module.moduleName());
    }

    public static void registerModule(String forPage, ShiinaModule module) {
        ModulePath modulePath = new ModulePath(forPage, module, false);
        modules.add(modulePath);
        log.info("Registered module for " + forPage + ": " + module.moduleName());
    }

    public static List<ShiinaModule> getModulesForPage(String page) {
        return loadedModules.get(page);
    }

    public static void reloadModuleConfigurations() {
        log.info("Reloading Module System...");
        Path modulesPath = Path.of("data/modules");
        if (!Files.exists(modulesPath)) {
            try {
                Files.createDirectories(modulesPath);
                log.info("Created directory: " + modulesPath);
            } catch (IOException e) {
                log.error("Failed to create modules directory: ", e);
            }
        }

        List<FoundModule> foundModules = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(modulesPath)) {
            paths.filter(path -> path.toString().endsWith(".json"))
                    .forEach(jsonPath -> {
                        try {
                            Path relativePath = modulesPath.relativize(jsonPath);
                            String filename = relativePath.getFileName().toString();

                            String page = filename.substring(0, filename.length() - 5);

                            log.info("Module Json found for page: " + page);

                            FoundModule foundModule = new FoundModule(page, Files.readString(jsonPath));
                            foundModules.add(foundModule);
                        } catch (Exception e) {
                            log.error("Error processing JSON file: " + jsonPath, e);
                        }
                    });
        } catch (IOException e) {
            log.error("Error walking through modules directory: ", e);
        }

        Set<String> foundPages = new HashSet<>();
        for(ModulePath modulePath : modules) {
            foundPages.add(modulePath.getPath());
        }

        Set<String> foundPagesJson = new HashSet<>();
        for(FoundModule foundModule : foundModules) {
            foundPagesJson.add(foundModule.getPage());
        }

        for(String page : foundPages) {
            if(!foundPagesJson.contains(page)) {
                List<String> toLoad = new ArrayList<>();
                for (ModulePath modulePath : modules) {
                    if (modulePath.getPath().equals(page)) {
                        if(!modulePath.isDefault()) {
                            continue;
                        }
                        toLoad.add(modulePath.getModule().moduleName());
                    }
                }

                Gson gson = new Gson();
                String json = gson.toJson(toLoad);
                try {
                    Files.writeString(Path.of("data/modules/" + page + ".json"), json);
                    log.info("Created JSON file for page: " + page);
                } catch (IOException e) {
                    log.error("Error writing JSON file for page: " + page, e);
                }
            }
        }

        for(FoundModule foundModule : foundModules) {
            List<String> toLoad = new Gson().fromJson(foundModule.getJson(), new TypeToken<List<String>>(){}.getType());
            List<ShiinaModule> loaded = new ArrayList<>();
            for(String moduleName : toLoad) {
                for(ModulePath modulePath : modules) {
                    if(modulePath.getModule().moduleName().equals(moduleName)) {
                        loaded.add(modulePath.getModule());
                    }
                }
            }
            log.info("Loaded modules for page: " + foundModule.getPage() + ": " + loaded);
            loadedModules.put(foundModule.getPage(), loaded);
        }

        


    }

    @AllArgsConstructor @Getter
    private static class FoundModule {
        private String page;
        private String json;
    }

}
