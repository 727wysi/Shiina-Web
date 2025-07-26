package dev.osunolimits.routes.ap.get;

import java.sql.ResultSet;

import dev.osunolimits.modules.Shiina;
import dev.osunolimits.modules.ShiinaRoute;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import dev.osunolimits.utils.osu.PermissionHelper;
import spark.Request;
import spark.Response;

public class Start extends Shiina {


    @Override
    public Object handle(Request req, Response res) throws Exception {
        ShiinaRequest shiina = new ShiinaRoute().handle(req, res);
        shiina.data.put("actNav", 1);

        if(!shiina.loggedIn) {
            res.redirect("/login");
            return notFound(res, shiina);
        }

        if(!PermissionHelper.hasPrivileges(shiina.user.priv, PermissionHelper.Privileges.STAFF)) {
            res.redirect("/");
            return notFound(res, shiina);
        }

        ResultSet statsResultSet = shiina.mysql.Query(
            "SELECT " +
            "COUNT(`id`) AS `active_map_requests`, " +
            "(SELECT COUNT(`id`) FROM `ingame_logins` WHERE DATE(`datetime`) = CURDATE()) AS `logins_today`, " +
            "(SELECT COUNT(`id`) FROM `users`) AS `total_users`, " +
            "(SELECT COUNT(`id`) FROM `users` WHERE `priv` != 1) AS `verified_users`, " +
            "(SELECT COUNT(`rating`) FROM `ratings`) AS `map_ratings`, " +
            "(SELECT COUNT(`id`) FROM `users` WHERE `creation_time` >= UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 1 MONTH))) AS `registered_last_month`, " +
            "(SELECT COUNT(`id`) FROM `users` WHERE `latest_activity` >= UNIX_TIMESTAMP() - 300) AS `online_users`, " +
            "(SELECT COUNT(`id`) FROM `users` WHERE `donor_end` > UNIX_TIMESTAMP()) AS `donators`, " +
            "(SELECT COUNT(`id`) FROM `users` WHERE `silence_end` > UNIX_TIMESTAMP()) AS `silenced_users` " +
            "FROM `map_requests` WHERE `active` = 1;"
        );

        while (statsResultSet.next()) {
            shiina.data.put("active_map_requests", statsResultSet.getInt("active_map_requests"));
            shiina.data.put("logins_today", statsResultSet.getInt("logins_today"));
            shiina.data.put("total_users", statsResultSet.getInt("total_users"));
            shiina.data.put("verified_users", statsResultSet.getInt("verified_users"));
            shiina.data.put("map_ratings", statsResultSet.getInt("map_ratings"));
            shiina.data.put("registered_last_month", statsResultSet.getInt("registered_last_month"));
            shiina.data.put("online_users", statsResultSet.getInt("online_users"));
            shiina.data.put("donators", statsResultSet.getInt("donators"));
            shiina.data.put("silenced_users", statsResultSet.getInt("silenced_users"));
        }
        return renderTemplate("ap/start.html", shiina, res, req);
    }
    
}
