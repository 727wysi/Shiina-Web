package dev.osunolimits.routes.post;

import dev.osunolimits.main.App;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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


public class HandleBadgeChange extends Shiina {
    private static final Logger log = LoggerFactory.getLogger(HandleBadgeChange.class);

    private MultipartConfigElement multipartConfig;
    private static final String BADGE_DIR = App.env.get("BADGEFOLDER");
    private static final int MAX_FILE_SIZE = (Integer.parseInt(App.env.get("MAXFILESIZE"))) * 1024 * 1024;
    private static final int MAX_REQUEST_SIZE = (Integer.parseInt(App.env.get("MAXREQUESTSIZE"))) * 1024 * 1024;

    public HandleBadgeChange() {
        log.info("[Badge] BADGE_DIR from ENV: {}", BADGE_DIR);
        multipartConfig = new MultipartConfigElement(".temp/", MAX_REQUEST_SIZE, MAX_REQUEST_SIZE, 1);
        File badgeDir = new File(BADGE_DIR);
        if (!badgeDir.exists()) {
            boolean created = badgeDir.mkdirs();
            log.info("[Badge] Badge directory did not exist, created: {} (path: {})", created, BADGE_DIR);
        }
        if (!badgeDir.canWrite()) {
            log.error("[Badge] No write access to badge directory: {}", BADGE_DIR);
        } else {
            log.info("[Badge] Badge directory is writable: {}", BADGE_DIR);
        }
    }

    @Override
    public Object handle(Request req, Response res) throws Exception {
        ShiinaRequest shiina = new ShiinaRoute().handle(req, res);
        shiina.data.put("actNav", 0);

        if (!shiina.loggedIn) {
            res.redirect("/login");
            return notFound(res, shiina);
        }

        if (!PermissionHelper.hasPrivileges(shiina.user.priv, PermissionHelper.Privileges.SUPPORTER)) {
            res.redirect("/settings?error=You need to be a supporter to upload a badge.");
            return notFound(res, shiina);
        }

        int userId = shiina.user.id;
        req.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfig);

        try {
            if (req.raw().getParts().size() > 0) {
                var part = req.raw().getPart("badge");
                if (part != null) {
                    String fileName = part.getSubmittedFileName();
                    if (fileName != null && (fileName.toLowerCase().endsWith(".png") || fileName.toLowerCase().endsWith(".gif")) && part.getSize() <= MAX_FILE_SIZE) {
                        Path finalBadgePath = Path.of(BADGE_DIR, userId + fileName.substring(fileName.lastIndexOf('.')));
                        log.info("[Badge] Try to write badge to: {}", finalBadgePath);
                        try (InputStream input = part.getInputStream()) {
                            Files.copy(input, finalBadgePath, StandardCopyOption.REPLACE_EXISTING);
                            log.info("[Badge] Files.copy completed for: {}", finalBadgePath);
                        }
                        log.info("[Badge] User {} uploaded badge: {} ({} bytes) to {}", userId, fileName, part.getSize(), finalBadgePath);
                        res.header("Cache-Control", "no-cache, no-store, must-revalidate");
                        res.header("Pragma", "no-cache");
                        res.header("Expires", "0");
                        res.redirect("/settings?info=Badge uploaded successfully! If it didn't update, hit CTRL+F5");
                    } else {
                        log.warn("[Badge] Invalid badge upload attempt by user {}: fileName={}, size={}", userId, fileName, part.getSize());
                        res.redirect("/settings?error=Invalid file type or size exceeds limit.");
                    }
                } else {
                    log.warn("[Badge] No file uploaded by user {}", userId);
                    res.redirect("/settings?error=No file uploaded.");
                }
            } else {
                log.warn("[Badge] No parts found in the request for user {}", userId);
                res.redirect("/settings?error=No parts found in the request.");
            }
        } catch (Exception e) {
            log.error("[Badge] Error processing badge upload for user {}: {}", userId, e.getMessage(), e);
            res.redirect("/settings?error=Error processing the upload: " + e.getMessage());
        }

        return notFound(res, shiina);
    }
}
