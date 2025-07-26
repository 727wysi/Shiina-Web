package dev.osunolimits.plugins;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import dev.osunolimits.plugins.models.NavbarAdminItem;
import dev.osunolimits.plugins.models.NavbarItem;

public class NavbarRegister {

    private static final Logger log = (Logger) LoggerFactory.getLogger("NavbarRegister");
    private final static int START_ACTNAV = 22;
    private static List<NavbarItem> items = new ArrayList<>();
    private static List<NavbarAdminItem> adminItems = new ArrayList<>();

    public static void register(NavbarItem item) {
        log.info("Registering navbar item: '" + item.getName() + "' on Route (" + item.getUrl()+ ") with actNav (" + item.getActNav() + ")");
        items.add(item);
    }

    public static void registerAdmin(NavbarAdminItem item) {
        log.info("Registering admin navbar item: '" + item.getName() + "' on Route (" + item.getUrl()+ ") with actNav (" + item.getActNav() + ")");
        adminItems.add(item);
    }

    public static List<NavbarItem> getItems() {
        return items;
    }

    public static List<NavbarAdminItem> getAdminItems() {
        return adminItems;
    }

    public static int getActNav() {
        return START_ACTNAV + items.size();
    }

    public static int getAdminActNav() {
        return START_ACTNAV + adminItems.size();
    }
    
}
