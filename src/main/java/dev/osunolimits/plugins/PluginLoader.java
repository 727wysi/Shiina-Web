package dev.osunolimits.plugins;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;

public class PluginLoader {

    private static final String PLUGINS_DIR = "plugins/";
    private static Logger log = (Logger) LoggerFactory.getLogger("PluginLoader");

    // Map to store shared classloaders for plugins with dependencies
    private final Map<String, URLClassLoader> sharedClassLoaders = new HashMap<>();

    public void loadPlugins() {
        try {
            Path pluginsPath = Paths.get(PLUGINS_DIR);
            if (!Files.exists(pluginsPath)) {
                Files.createDirectories(pluginsPath);
                log.info("Created plugins directory: " + PLUGINS_DIR);
                return;
            }

            Map<String, PluginMetadata> pluginMetadataMap = new HashMap<>();
            Set<String> loadedPlugins = new HashSet<>();

            // Step 1: Read and parse all the plugin jars
            try (DirectoryStream<Path> jarFiles = Files.newDirectoryStream(pluginsPath, "*.jar")) {
                for (Path jarPath : jarFiles) {
                    PluginMetadata metadata = parsePluginMetadata(jarPath.toString());
                    if (metadata != null) {
                        pluginMetadataMap.put(metadata.name, metadata);
                    }
                }
            }

            // Step 2: Sort plugins based on dependencies
            List<PluginMetadata> sortedPlugins = sortPluginsByDependency(pluginMetadataMap);

            // Step 3: Load plugins one by one while respecting dependencies
            for (PluginMetadata metadata : sortedPlugins) {
                if (!loadedPlugins.contains(metadata.name)) {
                    loadPluginFromMetadata(metadata, loadedPlugins, pluginMetadataMap);
                }
            }
        } catch (Exception e) {
            log.error("Error loading plugins: ", e);
        }
    }

    private PluginMetadata parsePluginMetadata(String jarFilePath) {
        try (JarFile jarFile = new JarFile(jarFilePath)) {
            JarEntry pluginYmlEntry = jarFile.getJarEntry("plugin.yml");
            if (pluginYmlEntry == null) {
                log.error("No plugin.yml in " + jarFilePath);
                return null;
            }

            try (InputStream inputStream = jarFile.getInputStream(pluginYmlEntry)) {
                java.util.Properties properties = new java.util.Properties();
                properties.load(inputStream);

                String mainClass = properties.getProperty("main");
                String pluginName = properties.getProperty("name");
                String dependsOn = properties.getProperty("depends-on");

                if (mainClass == null || pluginName == null) {
                    log.error("plugin.yml missing required fields in " + jarFilePath);
                    return null;
                }

                List<String> dependencies = (dependsOn != null) ? List.of(dependsOn.split(",")) : List.of();
                return new PluginMetadata(pluginName.trim(), mainClass.trim(), dependencies, jarFilePath);
            }
        } catch (Exception e) {
            log.error("Error reading plugin metadata from " + jarFilePath, e);
            return null;
        }
    }

    private List<PluginMetadata> sortPluginsByDependency(Map<String, PluginMetadata> plugins) {
        List<PluginMetadata> sorted = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> stack = new HashSet<>();

        for (PluginMetadata plugin : plugins.values()) {
            if (!visited.contains(plugin.name)) {
                if (!topologicalSort(plugin, plugins, visited, stack, sorted)) {
                    log.error("Circular dependency detected involving " + plugin.name);
                    return List.of(); // Prevents loading in case of a cycle
                }
            }
        }

        return sorted;
    }

    private boolean topologicalSort(PluginMetadata plugin, Map<String, PluginMetadata> plugins,
            Set<String> visited, Set<String> stack, List<PluginMetadata> sorted) {
        if (stack.contains(plugin.name))
            return false; // Cycle detected
        if (visited.contains(plugin.name))
            return true;

        stack.add(plugin.name);
        for (String dep : plugin.dependencies) {
            PluginMetadata depPlugin = plugins.get(dep.trim());
            if (depPlugin != null && !topologicalSort(depPlugin, plugins, visited, stack, sorted)) {
                return false;
            }
        }

        stack.remove(plugin.name);
        visited.add(plugin.name);
        sorted.add(plugin);
        return true;
    }

    private void loadPluginFromMetadata(PluginMetadata metadata, Set<String> loadedPlugins, Map<String, PluginMetadata> pluginMetadataMap) {
        Logger logger = (Logger) LoggerFactory.getLogger("Plugin [" + metadata.name + "]");
        try {
            // Step 1: Load dependencies first
            for (String dependency : metadata.dependencies) {
                if (!loadedPlugins.contains(dependency)) {
                    PluginMetadata depMetadata = pluginMetadataMap.get(dependency);
                    if (depMetadata != null) {
                        loadPluginFromMetadata(depMetadata, loadedPlugins, pluginMetadataMap);
                    }
                }
            }

            // Step 2: Load the plugin itself
            loadAndEnablePlugin(metadata, pluginMetadataMap, logger);
            loadedPlugins.add(metadata.name);
            logger.info("Successfully loaded plugin: " + metadata.name);
        } catch (Exception e) {
            log.error("Error loading plugin [" + metadata.name + "]: ", e);
        }
    }

    private static class PluginMetadata {
        String name;
        String mainClass;
        List<String> dependencies;
        String jarFilePath;

        PluginMetadata(String name, String mainClass, List<String> dependencies, String jarFilePath) {
            this.name = name;
            this.mainClass = mainClass;
            this.dependencies = dependencies;
            this.jarFilePath = jarFilePath;
        }
    }

    private void loadAndEnablePlugin(PluginMetadata metadata, Map<String, PluginMetadata> pluginMetadataMap, Logger logger) {
        try {
            Set<String> jarPaths = new HashSet<>();
            collectDependencies(metadata, pluginMetadataMap, jarPaths);

            URL[] urls = new URL[jarPaths.size()];
            int i = 0;
            for (String path : jarPaths) {
                urls[i++] = Paths.get(path).toUri().toURL();
            }

            // Use a shared classloader if the plugin has dependencies
            URLClassLoader classLoader;
            if (!metadata.dependencies.isEmpty()) {
                classLoader = new URLClassLoader(urls, getSharedClassLoader(metadata.dependencies, pluginMetadataMap));
            } else {
                classLoader = new URLClassLoader(urls, PluginLoader.class.getClassLoader());
            }

            Class<?> clazz = Class.forName(metadata.mainClass, true, classLoader);

            if (!ShiinaPlugin.class.isAssignableFrom(clazz)) {
                logger.info(metadata.mainClass + " does not extend ShiinaPlugin");
                return;
            }

            ShiinaPlugin pluginInstance = (ShiinaPlugin) clazz.getDeclaredConstructor().newInstance();
            pluginInstance.onEnable(metadata.name, logger);
            logger.info("Loaded and enabled plugin: " + metadata.name);
        } catch (Exception e) {
            logger.error("Error loading and enabling plugin: ", e);
        }
    }

    private void collectDependencies(PluginMetadata metadata, Map<String, PluginMetadata> pluginMetadataMap, Set<String> jarPaths) {
        if (jarPaths.contains(metadata.jarFilePath)) {
            return;
        }
        jarPaths.add(metadata.jarFilePath);
        for (String dep : metadata.dependencies) {
            PluginMetadata depMetadata = pluginMetadataMap.get(dep.trim());
            if (depMetadata != null) {
                collectDependencies(depMetadata, pluginMetadataMap, jarPaths);
            }
        }
    }

    private URLClassLoader getSharedClassLoader(List<String> dependencies, Map<String, PluginMetadata> pluginMetadataMap) throws Exception {
        // Create a unique key for the set of dependencies
        String key = String.join(",", dependencies);

        // Return an existing shared classloader if it exists
        if (sharedClassLoaders.containsKey(key)) {
            return sharedClassLoaders.get(key);
        }

        // Create a new shared classloader for these dependencies
        Set<String> jarPaths = new HashSet<>();
        for (String dep : dependencies) {
            PluginMetadata depMetadata = pluginMetadataMap.get(dep.trim());
            if (depMetadata != null) {
                collectDependencies(depMetadata, pluginMetadataMap, jarPaths);
            }
        }

        URL[] urls = new URL[jarPaths.size()];
        int i = 0;
        for (String path : jarPaths) {
            urls[i++] = Paths.get(path).toUri().toURL();
        }

        URLClassLoader sharedClassLoader = new URLClassLoader(urls, PluginLoader.class.getClassLoader());
        sharedClassLoaders.put(key, sharedClassLoader);
        return sharedClassLoader;
    }
}