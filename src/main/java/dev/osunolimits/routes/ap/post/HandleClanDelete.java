package dev.osunolimits.routes.ap.post;

import dev.osunolimits.modules.Shiina;
import dev.osunolimits.modules.ShiinaRoute;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import dev.osunolimits.utils.osu.PermissionHelper;
import spark.Request;
import spark.Response;

public class HandleClanDelete extends Shiina {
    @Override
    public Object handle(Request req, Response res) throws Exception {
        ShiinaRequest shiina = new ShiinaRoute().handle(req, res);
        if (shiina.user == null || !PermissionHelper.hasPrivileges(shiina.user.priv, PermissionHelper.Privileges.MODERATOR)) {
            res.redirect("/ap/start");
            return null;
        }
        String clanIdParam = req.queryParams("clanId");
        if (clanIdParam == null || !clanIdParam.matches("\\d+")) {
            res.redirect("/ap/clans?error=Invalid clan id");
            return null;
        }
        int clanId = Integer.parseInt(clanIdParam);
        shiina.mysql.Exec("DELETE FROM clans WHERE id = ?", clanId);
        shiina.mysql.Exec("DELETE FROM sh_clan_audit WHERE actor_id IN (SELECT id FROM users WHERE clan_id = ?) OR target_id IN (SELECT id FROM users WHERE clan_id = ?)", clanId, clanId);
        shiina.mysql.Exec("DELETE FROM sh_clan_pending WHERE clanid = ?", clanId);
        shiina.mysql.Exec("DELETE FROM sh_clan_denied WHERE clanid = ?", clanId);
        shiina.mysql.Exec("UPDATE users SET clan_id = NULL, clan_priv = 0 WHERE clan_id = ?", clanId);
        res.redirect("/ap/clans?success=Clan deleted");
        return null;
    }
}
