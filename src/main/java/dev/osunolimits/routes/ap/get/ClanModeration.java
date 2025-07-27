
package dev.osunolimits.routes.ap.get;
import dev.osunolimits.main.App;

import dev.osunolimits.modules.Shiina;
import dev.osunolimits.modules.ShiinaRoute;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import dev.osunolimits.utils.osu.PermissionHelper;
import spark.Request;
import spark.Response;

import java.sql.ResultSet;
import java.util.*;

public class ClanModeration extends Shiina {
    @Override
    public Object handle(Request req, Response res) throws Exception {
        ShiinaRequest shiina = new ShiinaRoute().handle(req, res);
        shiina.data.put("actNav", 17);
        if (shiina.user == null || !PermissionHelper.hasPrivileges(shiina.user.priv, PermissionHelper.Privileges.MODERATOR)) {
            res.redirect("/ap/start");
            return null;
        }
        String clanIdParam = req.queryParams("id");
        if (clanIdParam == null || !clanIdParam.matches("\\d+")) {
            res.redirect("/ap/clans?error=Invalid clan id");
            return null;
        }
        int clanId = Integer.parseInt(clanIdParam);
        ResultSet rs = shiina.mysql.Query("SELECT c.id, c.name, c.tag, c.owner, u.name AS ownerName, (SELECT COUNT(*) FROM users WHERE clan_id = c.id) AS members FROM clans c LEFT JOIN users u ON c.owner = u.id WHERE c.id = ?", clanId);
        Map<String, Object> clan = new HashMap<>();
        if (rs == null || !rs.next()) {
            clan.put("id", clanId);
            clan.put("name", "неизвестно");
            clan.put("tag", "неизвестно");
            clan.put("ownerName", "неизвестно");
            clan.put("ownerId", null);
            clan.put("members", 0);
        } else {
            clan.put("id", rs.getInt("id"));
            clan.put("name", rs.getString("name") != null ? rs.getString("name") : "неизвестно");
            clan.put("tag", rs.getString("tag") != null ? rs.getString("tag") : "неизвестно");
            clan.put("ownerName", rs.getString("ownerName") != null ? rs.getString("ownerName") : "неизвестно");
            clan.put("ownerId", rs.getObject("owner") != null ? rs.getInt("owner") : null);
            clan.put("members", rs.getObject("members") != null ? rs.getInt("members") : 0);
        }
        String avatarServer = System.getenv().getOrDefault("AVATARSRV", "https://a.727wysi.fun");
        clan.put("avatarUrl", avatarServer + "/clans/" + clan.get("id"));

        // Получение списка участников клана
        try {
            dev.osunolimits.api.ClanQuery clanQuery = new dev.osunolimits.api.ClanQuery(shiina.mysql);
            clan.put("membersList", clanQuery.getMembers(clanId));
        } catch (Exception e) {
            clan.put("membersList", new java.util.ArrayList<>());
        }

        shiina.data.put("clan", clan);

        // Аудит лог по клану
        List<Map<String, Object>> auditLog = new ArrayList<>();
        ResultSet auditRS = shiina.mysql.Query(
            "SELECT a.id, a.actor_id, a.action, a.target_id, a.created_at, u1.name AS actor_name, u2.name AS target_name " +
            "FROM sh_clan_audit a " +
            "LEFT JOIN users u1 ON a.actor_id = u1.id " +
            "LEFT JOIN users u2 ON a.target_id = u2.id " +
            "WHERE a.actor_id IN (SELECT id FROM users WHERE clan_id = ?) OR a.target_id IN (SELECT id FROM users WHERE clan_id = ?) " +
            "ORDER BY a.created_at DESC LIMIT 20",
            clan.get("id"), clan.get("id")
        );
        while (auditRS != null && auditRS.next()) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("id", auditRS.getInt("id"));
            entry.put("actorId", auditRS.getInt("actor_id"));
            entry.put("actorName", auditRS.getString("actor_name") != null ? auditRS.getString("actor_name") : "неизвестно");
            entry.put("action", auditRS.getString("action"));
            entry.put("targetId", auditRS.getObject("target_id") != null ? auditRS.getInt("target_id") : null);
            entry.put("targetName", auditRS.getString("target_name"));
            entry.put("createdAt", auditRS.getString("created_at"));
            auditLog.add(entry);
        }
        shiina.data.put("auditLog", auditLog);
        return renderTemplate("/ap/clan.html", shiina, res, req);
    }
}
