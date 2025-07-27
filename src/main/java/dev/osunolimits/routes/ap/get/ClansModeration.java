package dev.osunolimits.routes.ap.get;

import dev.osunolimits.modules.Shiina;
import dev.osunolimits.modules.ShiinaRoute;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import dev.osunolimits.utils.osu.PermissionHelper;
import spark.Request;
import spark.Response;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClansModeration extends Shiina {
    @Override
    public Object handle(Request req, Response res) throws Exception {
        ShiinaRequest shiina = new ShiinaRoute().handle(req, res);
        shiina.data.put("actNav", 17);
        if (shiina.user == null || !PermissionHelper.hasPrivileges(shiina.user.priv, PermissionHelper.Privileges.MODERATOR)) {
            res.redirect("/ap/start");
            return null;
        }
        List<Map<String, Object>> clans = new ArrayList<>();
        ResultSet rs = shiina.mysql.Query("SELECT c.id, c.name, c.tag, u.name AS ownerName, (SELECT COUNT(*) FROM users WHERE clan_id = c.id) AS members FROM clans c LEFT JOIN users u ON c.owner = u.id ORDER BY c.id DESC");
        while (rs != null && rs.next()) {
            Map<String, Object> clan = new HashMap<>();
            clan.put("id", rs.getInt("id"));
            clan.put("name", rs.getString("name") != null ? rs.getString("name") : "неизвестно");
            clan.put("tag", rs.getString("tag") != null ? rs.getString("tag") : "неизвестно");
            clan.put("ownerName", rs.getString("ownerName") != null ? rs.getString("ownerName") : "неизвестно");
            clan.put("members", rs.getObject("members") != null ? rs.getInt("members") : 0);
            clans.add(clan);
        }
        shiina.data.put("clans", clans);
        shiina.data.put("page", 0);
        shiina.data.put("totalPages", 1);
        shiina.data.put("hasNextPage", false); // Always false for now, as only one page
        return renderTemplate("/ap/clans.html", shiina, res, req);
    }
}
