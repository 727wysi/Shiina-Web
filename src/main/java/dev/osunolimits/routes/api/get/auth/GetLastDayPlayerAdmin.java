package dev.osunolimits.routes.api.get.auth;

import dev.osunolimits.main.App;
import dev.osunolimits.modules.Shiina;
import dev.osunolimits.modules.ShiinaRoute;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import dev.osunolimits.routes.api.get.ShiinaAPIHandler;
import dev.osunolimits.utils.osu.PermissionHelper;
import spark.Request;
import spark.Response;

public class GetLastDayPlayerAdmin extends Shiina {

    @Override
    public Object handle(Request req, Response res) throws Exception {
        ShiinaRequest shiina = new ShiinaRoute().handle(req, res);

        if (shiina.user == null) {
            res.redirect("/login");
            return notFound(res, shiina);
        }

        if(!PermissionHelper.hasPrivileges(shiina.user.priv, PermissionHelper.Privileges.STAFF)) {
            res.redirect("/");
            return notFound(res, shiina);
        }

        ShiinaAPIHandler shiinaAPIHandler = new ShiinaAPIHandler();
        return shiinaAPIHandler.renderJSON(App.hourlyPlayerStatsThread.getStore().getLast24Hours(), shiina, res);
    }
    
}
