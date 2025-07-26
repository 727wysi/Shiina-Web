package dev.osunolimits.routes.post;

import dev.osunolimits.main.App;
import dev.osunolimits.modules.Shiina;
import dev.osunolimits.modules.ShiinaRoute;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import dev.osunolimits.utils.osu.PermissionHelper;
import spark.Request;
import spark.Response;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javax.servlet.MultipartConfigElement;

import net.coobird.thumbnailator.Thumbnails;

public class HandleClanAvatarChange extends Shiina {

    private MultipartConfigElement multipartConfig;
    private static final String CLAN_AVATAR_DIR = "/home/bancho.py-ex/.data/avatars/clans";
    private static final int MAX_FILE_SIZE = (Integer.parseInt(App.env.get("MAXFILESIZE"))) * 1024 * 1024;
    private static final int MAX_REQUEST_SIZE = (Integer.parseInt(App.env.get("MAXREQUESTSIZE"))) * 1024 * 1024;

    public HandleClanAvatarChange() {
        multipartConfig = new MultipartConfigElement(".temp/", MAX_REQUEST_SIZE, MAX_REQUEST_SIZE, 1);
    }

    @Override
    public Object handle(Request req, Response res) throws Exception {
        // Debug: log avatar dir and final path
        System.out.println("[CLAN AVATAR UPLOAD] CLAN_AVATAR_DIR: " + CLAN_AVATAR_DIR);
        ShiinaRequest shiina = new ShiinaRoute().handle(req, res);
        shiina.data.put("actNav", 0);

        if (!shiina.loggedIn) {
            res.redirect("/login");
            return null;
        }

        // clanId from route param
        String clanIdParam = req.params("id");
        if (clanIdParam == null || !clanIdParam.matches("\\d+")) {
            res.redirect("/clans?error=Invalid clan id");
            return null;
        }
        int clanId = Integer.parseInt(clanIdParam);

        // Проверка: пользователь должен быть владельцем клана (clan_priv == 3 и clan_id совпадает)
        String clanPermQuery = "SELECT `clan_priv`, `clan_id` FROM `users` WHERE `id` = ?";
        try (var clanPermRS = shiina.mysql.Query(clanPermQuery, shiina.user.id)) {
            if (!clanPermRS.next() || clanPermRS.getInt("clan_priv") != 3 || clanPermRS.getInt("clan_id") != clanId) {
                res.redirect("/clan/" + clanId + "/settings?error=Only clan owner can change avatar");
                return null;
            }
        }

        req.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfig);

        try {
            if (req.raw().getParts().size() > 0) {
                var part = req.raw().getPart("avatar");
                if (part != null) {
                    String fileName = part.getSubmittedFileName();

                    if (fileName != null
                            && (fileName.toLowerCase().endsWith(".png") || fileName.toLowerCase().endsWith(".gif"))
                            && part.getSize() <= MAX_FILE_SIZE) {
                        File avatarDir = new File(CLAN_AVATAR_DIR);
                        if (!avatarDir.exists()) {
                            avatarDir.mkdirs();
                        }

                        // Удалить старые аватарки клана
                        for (File file : avatarDir.listFiles()) {
                            if (file.getName().matches(clanId + "\\.(png|jpg|gif)")) {
                                file.delete();
                            }
                        }

                        Path finalAvatarPath = Path.of(CLAN_AVATAR_DIR,
                                clanId + (fileName.toLowerCase().endsWith(".gif") ? ".gif" : ".png"));
                        System.out.println("[CLAN AVATAR UPLOAD] Saving to: " + finalAvatarPath);

                        if (fileName.toLowerCase().endsWith(".gif") && PermissionHelper.hasPrivileges(shiina.user.priv,
                                PermissionHelper.Privileges.SUPPORTER)) {
                            try (InputStream input = part.getInputStream()) {
                                Files.copy(input, finalAvatarPath, StandardCopyOption.REPLACE_EXISTING);
                            }
                        } else if (fileName.toLowerCase().endsWith(".png")) {
                            try (InputStream input = part.getInputStream()) {
                                Thumbnails.of(input)
                                        .size(500, 500)
                                        .outputFormat("png")
                                        .toFile(finalAvatarPath.toFile());
                            }
                        } else {
                            res.redirect("/clan/" + clanId + "/settings?error=You need to be a supporter to use GIFs.");
                            return null;
                        }

                        res.header("Cache-Control", "no-cache, no-store, must-revalidate");
                        res.header("Pragma", "no-cache");
                        res.header("Expires", "0");
                        res.redirect("/clan/" + clanId + "/settings?info=Clan avatar uploaded successfully! If it didn't update, hit CTRL+F5");
                        return null;
                    } else {
                        res.redirect("/clan/" + clanId + "/settings?error=Invalid file type or size exceeds limit.");
                        return null;
                    }
                } else {
                    res.redirect("/clan/" + clanId + "/settings?error=No file uploaded.");
                    return null;
                }
            } else {
                res.redirect("/clan/" + clanId + "/settings?error=No parts found in the request.");
                return null;
            }
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Request entity too large")) {
                res.redirect("/clan/" + clanId + "/settings?error=File too large. Max size: " + (MAX_FILE_SIZE / (1024 * 1024)) + "MB");
                return null;
            }
            res.redirect("/clan/" + clanId + "/settings?error=Error processing the upload: " + e.getMessage());
            return null;
        }
    }
}
