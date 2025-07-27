package dev.osunolimits.routes.ap.post;

import dev.osunolimits.modules.Shiina;
import dev.osunolimits.modules.ShiinaRoute;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import dev.osunolimits.utils.osu.PermissionHelper;

import dev.osunolimits.main.App;
import spark.Request;
import spark.Response;

import java.io.File;

public class HandleClanAvatarDelete extends Shiina {
    @Override
    public Object handle(Request req, Response res) throws Exception {
        ShiinaRequest shiina = new ShiinaRoute().handle(req, res);
        if (shiina.user == null || !PermissionHelper.hasPrivileges(shiina.user.priv, PermissionHelper.Privileges.MODERATOR)) {
            res.redirect("/ap/start");
            return null;
        }
        String clanIdParam = req.queryParams("clanId");
        if (clanIdParam == null || !clanIdParam.matches("\\d+")) {
            res.redirect("/ap/clan?id=" + clanIdParam + "&error=Invalid clan id");
            return null;
        }
        System.out.println("[CLAN AVATAR DELETE] CLANAVATARFOLDER=" + System.getenv("CLANAVATARFOLDER"));
        int clanId = Integer.parseInt(clanIdParam);
        String avatarFolder = App.env.get("CLANAVATARFOLDER");
        if (!avatarFolder.endsWith("/") && !avatarFolder.endsWith("\\")) {
            avatarFolder += "/";
        }
        String[] exts = {".png", ".jpg", ".jpeg", ".webp", ".gif"};
        boolean deleted = false;
        for (String ext : exts) {
            String path = avatarFolder + clanId + ext;
            File avatarFile = new File(path);
            System.out.println("[CLAN AVATAR DELETE] Проверка: " + path + " exists=" + avatarFile.exists() + " canWrite=" + avatarFile.canWrite());
            if (avatarFile.exists()) {
                boolean del = avatarFile.delete();
                System.out.println("[CLAN AVATAR DELETE] Удаление " + path + ": " + del);
                deleted = deleted || del;
            }
        }
        if (!deleted) {
            System.out.println("[CLAN AVATAR DELETE] Не удалось удалить ни один файл для клана " + clanId);
        }
        res.redirect("/ap/clan?id=" + clanId + "&success=Avatar deleted");
        return null;
    }
}
