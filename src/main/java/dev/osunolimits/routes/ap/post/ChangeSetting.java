package dev.osunolimits.routes.ap.post;

import dev.osunolimits.modules.Shiina;
import dev.osunolimits.modules.ShiinaRoute;
import dev.osunolimits.modules.XmlConfig;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import dev.osunolimits.utils.osu.PermissionHelper;
import spark.Request;
import spark.Response;

public class ChangeSetting extends Shiina {

    @Override
    public Object handle(Request req, Response res) throws Exception {
        ShiinaRequest shiina = new ShiinaRoute().handle(req, res);

        if(!shiina.loggedIn) {
            res.redirect("/login");
            return notFound(res, shiina);
        }

        if(!PermissionHelper.hasPrivileges(shiina.user.priv, PermissionHelper.Privileges.ADMINISTRATOR)) {
            res.redirect("/");
            return notFound(res, shiina);
        }

        String key = req.queryParams("key");
        String value = req.queryParams("value");

        if (key == null || value == null) {
            res.status(400);
            return "Bad Request: Missing key or value";
        }

        XmlConfig.getInstance().set(key, value);

        return redirect(res, shiina, "/ap/settings?state=success&message=Setting%20" + key + "%20updated%20successfully");

    }
    
}
