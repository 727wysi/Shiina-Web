package dev.osunolimits.routes.get.clans;

import java.util.List;

import dev.osunolimits.api.ClanQuery;
import dev.osunolimits.api.ClanQuery.ClanResponse;
import dev.osunolimits.main.App;
import dev.osunolimits.modules.Shiina;
import dev.osunolimits.modules.ShiinaRoute;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import dev.osunolimits.modules.utils.SEOBuilder;
import dev.osunolimits.utils.Validation;
import dev.osunolimits.utils.osu.OsuConverter;
import spark.Request;
import spark.Response;

public class Clans extends Shiina {

    public static int pageListingSize = 10;

    @Override
    public Object handle(Request req, Response res) throws Exception {
        ShiinaRequest shiina = new ShiinaRoute().handle(req, res);
        shiina.data.put("actNav", 4);
    
        ClanQuery clanQuery = new ClanQuery(shiina.mysql);
        int page = 1;
        Boolean hasNext = false;
        int pageSize = pageListingSize;
        int offset = 0;

        if (req.queryParams("page") != null && Validation.isNumeric(req.queryParams("page"))) {
            page = Integer.parseInt(req.queryParams("page"));
        }

        offset = (page - 1) * pageSize;

        int mode = 0;
        if (OsuConverter.checkForValidMode(req.queryParams("mode"))) {
            mode = Integer.parseInt(req.queryParams("mode"));
        }

        String sort = "totalpp";
        if (req.queryParams("sort") != null && ClanQuery.CompetitionType.fromName(req.queryParams("sort")) != null) {
            sort = req.queryParams("sort");
        }

        List<ClanResponse> clans = clanQuery.getClan(ClanQuery.CompetitionType.fromName(sort), mode, pageSize + 1, offset);

        if (clans.size() > pageSize) {
            hasNext = true;
            clans = clans.subList(0, pageSize);
        }

        shiina.data.put("clans", clans);
        shiina.data.put("seo", new SEOBuilder("Clan Leaderboard | " + OsuConverter.convertModeBack(String.valueOf(mode)) + " | " + sort.toUpperCase(), App.customization.get("homeDescription").toString()));
        shiina.data.put("pageSize", clans.size());
        shiina.data.put("hasNext", hasNext);
        shiina.data.put("totalPageSize", pageSize);
        shiina.data.put("page", page);
        shiina.data.put("mode", mode);
        shiina.data.put("sort", sort);
        shiina.data.put("offset", offset + 1);
    
        return renderTemplate("clans/clans.html", shiina, res, req);
    }
}
