package dev.osunolimits.routes.ap.get;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import dev.osunolimits.modules.Shiina;
import dev.osunolimits.modules.ShiinaRoute;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import dev.osunolimits.utils.Validation;
import dev.osunolimits.utils.osu.PermissionHelper;
import lombok.Data;
import spark.Request;
import spark.Response;

public class MapRanking extends Shiina {

    @Data
    public class MapRankingBeatmap {
        private int id;
        private int setId;
        private String version;
        private int status;
        private int mode;
        private double bpm;
        private double cs;
        private double ar;
        private double od;
        private double hp;
        private double diff;
    }

    @Data
    public class MapRankingBeatmapSet {
        private int setId;
        private String title;
        private String artist;
        private String creator;
        private List<MapRankingBeatmap> beatmaps = new ArrayList<>();
    }

    @Override
    public Object handle(Request req, Response res) throws Exception {
        ShiinaRequest shiina = new ShiinaRoute().handle(req, res);
        shiina.data.put("actNav", 21);

        if(!shiina.loggedIn) {
            res.redirect("/login");
            return notFound(res, shiina);
        }

        if(!PermissionHelper.hasPrivileges(shiina.user.priv, PermissionHelper.Privileges.NOMINATOR)) {
            res.redirect("/");
            return notFound(res, shiina);
        }

        String setId = req.queryParams("setId");
        String search = req.queryParams("search");
        if(setId != null) {
            shiina.data.put("search", "https://osu.ppy.sh/beatmapsets/" + setId);
        }

        String reqId = null;
        if(req.queryParams("reqId") != null) {
            reqId = req.queryParams("reqId");
        } 

        if(search != null) {
            // Check if it's an osunolimits URL (e.g., https://osunolimits.dev/b/2813660)
            if (search.contains("/b/") || search.matches(".*/b/\\d+.*")) {
                // Extract beatmap ID from osunolimits URL
                String[] parts = search.split("/b/");
                if (parts.length > 1) {
                    String beatmapIdStr = parts[1].split("[^0-9]")[0]; // Get only the numeric part
                    if (beatmapIdStr.matches("\\d+")) {
                        int beatmapId = Integer.parseInt(beatmapIdStr);

                        ResultSet rs = shiina.mysql.Query("SELECT set_id FROM maps WHERE id = ?", beatmapId);
                        if (rs.next()) {
                            setId = String.valueOf(rs.getInt("set_id"));
                            shiina.data.put("search", "https://osu.ppy.sh/beatmapsets/" + setId);
                        }
                        
                    }
                }
            } else {
                // Handle osu.ppy.sh URLs (e.g., https://osu.ppy.sh/beatmapsets/2304366#osu/4939518)
                String[] parts = search.split("#");
                if (parts.length > 0) {
                    String[] setParts = parts[0].split("/");
                    if (setParts.length > 0) {
                        setId = setParts[setParts.length - 1];
                        shiina.data.put("search", "https://osu.ppy.sh/beatmapsets/" + setId);
                    }
                }
            }
        }

        if(setId != null && Validation.isNumeric(setId)) {
            ResultSet setRs = shiina.mysql.Query("SELECT `id`, `title`, `artist`, `creator` FROM `maps` WHERE `set_id` = ? LIMIT 1", setId);
            MapRankingBeatmapSet mapSet = new MapRankingBeatmapSet();
            if(setRs.next()) {
                mapSet.setSetId(Integer.parseInt(setId));
                mapSet.setTitle(setRs.getString("title"));
                mapSet.setArtist(setRs.getString("artist"));
                mapSet.setCreator(setRs.getString("creator"));
                shiina.data.put("mapSet", mapSet);
            }

            ResultSet beatmapRs = shiina.mysql.Query("SELECT `id`, `set_id`, `version`, `status`, `mode`, `bpm`, `cs`, `ar`, `od`, `hp`, `diff` FROM `maps` WHERE `set_id` = ? ORDER BY `maps`.`mode` ASC, `maps`.`diff` DESC", setId);
            List<MapRankingBeatmap> beatmaps = new ArrayList<>();
            while (beatmapRs.next()) {
                MapRankingBeatmap beatmap = new MapRankingBeatmap();
                beatmap.setId(beatmapRs.getInt("id"));
                beatmap.setSetId(beatmapRs.getInt("set_id"));
                beatmap.setVersion(beatmapRs.getString("version"));
                beatmap.setStatus(beatmapRs.getInt("status"));
                beatmap.setMode(beatmapRs.getInt("mode"));
                beatmap.setBpm(beatmapRs.getDouble("bpm"));
                beatmap.setCs(beatmapRs.getDouble("cs"));
                beatmap.setAr(beatmapRs.getDouble("ar"));
                beatmap.setOd(beatmapRs.getDouble("od"));
                beatmap.setHp(beatmapRs.getDouble("hp"));
                beatmap.setDiff(beatmapRs.getDouble("diff"));
                beatmaps.add(beatmap);
            }
            mapSet.setBeatmaps(beatmaps);
            shiina.data.put("map", mapSet);
        }else {
            shiina.data.put("search", "");
        }
        shiina.data.put("reqId", reqId);

        return renderTemplate("ap/mapranking.html", shiina, res, req);
    }
    
}
