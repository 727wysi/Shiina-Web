package dev.osunolimits.routes.ap.get;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import dev.osunolimits.modules.Shiina;
import dev.osunolimits.modules.ShiinaRoute;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import dev.osunolimits.utils.osu.PermissionHelper;
import lombok.Data;
import spark.Request;
import spark.Response;

public class MapRequests extends Shiina {

    private final String MAP_SQL = "SELECT `map`.`id`, `map`.`map_id`, `maps`.`set_id`, `map`.`player_id`, `map`.`datetime`, `map`.`active`, CONCAT(`maps`.`title`, ' - ', `maps`.`artist`) AS `map_name`, `users`.`name` FROM `map_requests` AS `map` INNER JOIN `users` ON `map`.`player_id` = `users`.`id` INNER JOIN `maps` ON `map`.`map_id` = `maps`.`id` ORDER BY `map`.`active` DESC, `map`.`datetime` DESC LIMIT ? OFFSET ?;";

    @Data
    public class MapRequest {
        private int id;
        private int mapId;
        private int setId;
        private int playerId;
        private String mapName;
        private String playerName;
        private String datetime;
        private boolean active;
    }

    @Override
    public Object handle(Request req, Response res) throws Exception {
        ShiinaRequest shiina = new ShiinaRoute().handle(req, res);
        shiina.data.put("actNav", 20);

        if(!shiina.loggedIn) {
            res.redirect("/login");
            return notFound(res, shiina);
        }

        if(!PermissionHelper.hasPrivileges(shiina.user.priv, PermissionHelper.Privileges.NOMINATOR)) {
            res.redirect("/");
            return notFound(res, shiina);
        }
        int page = 0;
        if(req.queryParams("page") != null) {
            page = Integer.parseInt(req.queryParams("page"));
        }

        int offset = page * 10;

        ResultSet statsResultSet = shiina.mysql.Query(MAP_SQL, 11, offset);
        List<MapRequest> mapRequests = new ArrayList<>();
        boolean hasNextPage = false;
        while (statsResultSet.next()) {
            MapRequest mapRequest = new MapRequest();
            mapRequest.setId(statsResultSet.getInt("id"));
            mapRequest.setMapId(statsResultSet.getInt("map_id"));
            mapRequest.setSetId(statsResultSet.getInt("set_id"));
            mapRequest.setPlayerId(statsResultSet.getInt("player_id"));
            mapRequest.setMapName(statsResultSet.getString("map_name"));
            mapRequest.setPlayerName(statsResultSet.getString("name"));
            mapRequest.setDatetime(statsResultSet.getString("datetime"));
            mapRequest.setActive(statsResultSet.getBoolean("active"));

            if(mapRequests.size() == 10) {
                hasNextPage = true;
                continue;
            }
            mapRequests.add(mapRequest);
           
        }
        shiina.data.put("mapRequests", mapRequests);
        shiina.data.put("hasNextPage", hasNextPage);
        shiina.data.put("page", page);
        return renderTemplate("ap/maprequests.html", shiina, res, req);
    }
    
}
