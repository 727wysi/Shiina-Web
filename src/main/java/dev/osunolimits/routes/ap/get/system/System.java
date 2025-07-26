package dev.osunolimits.routes.ap.get.system;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import dev.osunolimits.common.Database;
import dev.osunolimits.modules.Shiina;
import dev.osunolimits.modules.ShiinaRoute;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import dev.osunolimits.routes.api.get.GetBmThumbnail;
import dev.osunolimits.utils.osu.PermissionHelper;
import spark.Request;
import spark.Response;

public class System extends Shiina {

    @Override
    public Object handle(Request req, Response res) throws Exception {
        ShiinaRequest shiina = new ShiinaRoute().handle(req, res);
        shiina.data.put("actNav", 10);

        if (!shiina.loggedIn) {
            return redirect(res, shiina, "/");
        }

        if (!PermissionHelper.hasPrivileges(shiina.user.priv, PermissionHelper.Privileges.ADMINISTRATOR)) {
            return redirect(res, shiina, "/");
        }

        // System and application statistics
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        Runtime runtime = Runtime.getRuntime();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        DecimalFormat df = new DecimalFormat("#.##");

        // System statistics
        shiina.data.put("os_name", osBean.getName());
        shiina.data.put("os_version", osBean.getVersion());
        shiina.data.put("os_arch", osBean.getArch());
        shiina.data.put("available_processors", osBean.getAvailableProcessors());
        
        double loadAvg = osBean.getSystemLoadAverage();
        shiina.data.put("system_load_average", loadAvg >= 0 ? df.format(loadAvg) : "N/A");
        shiina.data.put("system_load_percentage", loadAvg >= 0 ? df.format((loadAvg / osBean.getAvailableProcessors()) * 100) : "N/A");

        // JVM Information
        shiina.data.put("jvm_name", java.lang.System.getProperty("java.vm.name"));
        shiina.data.put("jvm_version", java.lang.System.getProperty("java.vm.version"));
        shiina.data.put("java_version", java.lang.System.getProperty("java.version"));

        // Memory calculations
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        long usedMemory = totalMemory - freeMemory;
        
        shiina.data.put("jvm_total_memory", formatBytes(totalMemory));
        shiina.data.put("jvm_free_memory", formatBytes(freeMemory));
        shiina.data.put("jvm_max_memory", formatBytes(maxMemory));
        shiina.data.put("jvm_used_memory", formatBytes(usedMemory));
        shiina.data.put("jvm_memory_usage_percent", df.format((double) usedMemory / totalMemory * 100));
        shiina.data.put("jvm_heap_usage_percent", df.format((double) usedMemory / maxMemory * 100));

        // Raw values for progress bars
        shiina.data.put("jvm_total_memory_raw", totalMemory);
        shiina.data.put("jvm_free_memory_raw", freeMemory);
        shiina.data.put("jvm_max_memory_raw", maxMemory);
        shiina.data.put("jvm_used_memory_raw", usedMemory);

        // Heap memory details
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        shiina.data.put("heap_used", formatBytes(heapUsed));
        shiina.data.put("heap_max", formatBytes(heapMax));
        shiina.data.put("heap_usage_percent", df.format((double) heapUsed / heapMax * 100));

        // Non-heap memory details
        long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
        long nonHeapMax = memoryBean.getNonHeapMemoryUsage().getMax();
        shiina.data.put("non_heap_used", formatBytes(nonHeapUsed));
        shiina.data.put("non_heap_max", nonHeapMax > 0 ? formatBytes(nonHeapMax) : "Unlimited");

        // Thumbnail cache statistics
        HashMap<String, byte[]> thumbnailCache = GetBmThumbnail.thumbnailCache;
        AtomicInteger thumbnailCacheSizeBytes = new AtomicInteger(0);
        thumbnailCache.forEach((k, v) -> {
            thumbnailCacheSizeBytes.addAndGet(v.length);
        });

        long cacheSizeBytes = thumbnailCacheSizeBytes.get();
        shiina.data.put("thumbnail_cache_size", formatBytes(cacheSizeBytes));
        shiina.data.put("thumbnail_cache_count", thumbnailCache.size());

        // Database connections
        shiina.data.put("sql_con", Database.currentConnections);
        shiina.data.put("sql_con_list", Database.runningConnections.size());

        // Thread statistics
        shiina.data.put("thread_count", threadMXBean.getThreadCount());
        shiina.data.put("peak_thread_count", threadMXBean.getPeakThreadCount());
        shiina.data.put("daemon_thread_count", threadMXBean.getDaemonThreadCount());

        // Uptime
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        shiina.data.put("uptime", formatUptime(uptimeMs));

        return renderTemplate("ap/system/system.html", shiina, res, req);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format("%.2f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format("%.2f MB", mb);
        double gb = mb / 1024.0;
        return String.format("%.2f GB", gb);
    }

    private String formatUptime(long uptimeMs) {
        long seconds = uptimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
}
