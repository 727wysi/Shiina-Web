package dev.osunolimits.plugins.models;

import dev.osunolimits.plugins.NavbarRegister;
import dev.osunolimits.utils.osu.PermissionHelper;
import lombok.Data;

@Data
public class NavbarAdminItem {
    private String name;
    private String url;
    private String icon;
    private int actNav;
    private String permission;

    public NavbarAdminItem(String name, String url, String icon, PermissionHelper.Privileges permission) {
        this.name = name;
        this.url = url;
        this.icon = icon;
        this.permission = permission.name();
        actNav = NavbarRegister.getAdminActNav();
    }
}