package dev.osunolimits.main;

import java.sql.SQLException;
import java.util.Map;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import dev.osunolimits.models.Action;
import dev.osunolimits.modules.DatabaseCleanUp;
import dev.osunolimits.modules.HourlyPlayerStatsThread;
import dev.osunolimits.modules.ShiinaDocs;
import dev.osunolimits.modules.ShiinaMultiDetection;
import dev.osunolimits.modules.ShiinaRankCache;
import dev.osunolimits.modules.utils.GroupRegistry;
import dev.osunolimits.modules.utils.RobotJsonConfig;
import dev.osunolimits.modules.utils.ThemeLoader;
import dev.osunolimits.modules.utils.UserInfoCache;
import dev.osunolimits.monetization.MonetizationConfig;
import dev.osunolimits.monetization.routes.KofiDonoHandler;
import dev.osunolimits.plugins.NavbarRegister;
import dev.osunolimits.plugins.PluginLoader;
import dev.osunolimits.plugins.models.NavbarAdminItem;
import dev.osunolimits.plugins.models.NavbarItem;
import dev.osunolimits.routes.ap.api.PubSubHandler;
import dev.osunolimits.routes.ap.api.RecoverAccount;
import dev.osunolimits.routes.ap.get.Audit;
import dev.osunolimits.routes.ap.get.Bancho;
import dev.osunolimits.routes.ap.get.ChatExplorer;
import dev.osunolimits.routes.ap.get.Commands;
import dev.osunolimits.routes.ap.get.MapRanking;
import dev.osunolimits.routes.ap.get.MapRequests;
import dev.osunolimits.routes.ap.get.ModularSettings;
import dev.osunolimits.routes.ap.get.Multiaccounts;
import dev.osunolimits.routes.ap.get.Start;
import dev.osunolimits.routes.ap.get.Themes;
import dev.osunolimits.routes.ap.get.groups.Groups;
import dev.osunolimits.routes.ap.get.groups.ManageGroup;
import dev.osunolimits.routes.ap.get.groups.ProcessGroup;
import dev.osunolimits.routes.ap.get.system.System;
import dev.osunolimits.routes.ap.get.system.SystemConnections;
import dev.osunolimits.routes.ap.get.system.SystemThreads;
import dev.osunolimits.routes.ap.get.users.ApUser;
import dev.osunolimits.routes.ap.get.users.Users;
import dev.osunolimits.routes.ap.post.ChangeSetting;
import dev.osunolimits.routes.ap.post.ChangeTheme;
import dev.osunolimits.routes.ap.post.DenyMapRequest;
import dev.osunolimits.routes.ap.post.HandleMapStatusUpdate;
import dev.osunolimits.routes.ap.post.ProcessManageGroup;
import dev.osunolimits.routes.api.get.GetBanner;
import dev.osunolimits.routes.api.get.GetBmThumbnail;
import dev.osunolimits.routes.api.get.GetComments;
import dev.osunolimits.routes.api.get.GetFirstPlaces;
import dev.osunolimits.routes.api.get.GetPlaycountGraph;
import dev.osunolimits.routes.api.get.GetPlayerScores;
import dev.osunolimits.routes.api.get.GetRankCache;
import dev.osunolimits.routes.api.get.Health;
import dev.osunolimits.routes.api.get.Search;
import dev.osunolimits.routes.api.get.auth.GetLastDayPlayerAdmin;
import dev.osunolimits.routes.api.get.auth.HandleBeatmapFavorite;
import dev.osunolimits.routes.api.get.auth.HandleClanAction;
import dev.osunolimits.routes.api.get.auth.HandleClanRequest;
import dev.osunolimits.routes.api.get.auth.HandleRelationship;
import dev.osunolimits.routes.get.Beatmap;
import dev.osunolimits.routes.get.Beatmaps;
import dev.osunolimits.routes.get.Bot;
import dev.osunolimits.routes.get.Leaderboard;
import dev.osunolimits.routes.get.User;
import dev.osunolimits.routes.get.UserScore;
import dev.osunolimits.routes.get.clans.Clan;
import dev.osunolimits.routes.get.clans.Clans;
import dev.osunolimits.routes.get.clans.ManageClan;
import dev.osunolimits.routes.get.errors.NotFound;
import dev.osunolimits.routes.get.modular.Home;
import dev.osunolimits.routes.get.modular.ModuleRegister;
import dev.osunolimits.routes.get.modular.ShiinaModule;
import dev.osunolimits.routes.get.modular.home.BigHeader;
import dev.osunolimits.routes.get.modular.home.MoreInfos;
import dev.osunolimits.routes.get.simple.Donate;
import dev.osunolimits.routes.get.simple.Login;
import dev.osunolimits.routes.get.simple.Recover;
import dev.osunolimits.routes.get.simple.Register;
import dev.osunolimits.routes.get.user.Relations;
import dev.osunolimits.routes.get.user.Settings;
import dev.osunolimits.routes.post.HandleAvatarChange;
import dev.osunolimits.routes.post.HandleClanAvatarChange;
import dev.osunolimits.routes.post.HandleBannerChange;
import dev.osunolimits.routes.post.HandleBadgeChange;
import dev.osunolimits.routes.post.HandleComment;
import dev.osunolimits.routes.post.HandleFlagChange;
import dev.osunolimits.routes.post.HandleLogin;
import dev.osunolimits.routes.post.HandleLogout;
import dev.osunolimits.routes.post.HandleModeChange;
import dev.osunolimits.routes.post.HandleNameChange;
import dev.osunolimits.routes.post.HandleRecovery;
import dev.osunolimits.routes.post.HandleRegister;
import dev.osunolimits.routes.post.HandleUserpageChange;
import dev.osunolimits.routes.redirects.GucchoBmRedirect;
import dev.osunolimits.routes.redirects.GucchoUserRedirect;
import dev.osunolimits.utils.osu.PermissionHelper.Privileges;
import io.github.cdimascio.dotenv.Dotenv;
import redis.clients.jedis.JedisPooled;
import spark.Spark;

/**
 * shiina - a modern osu! private server frontend for the web
 * By Marc Andre Herpers
 */
public class App {

    public static final Logger log = (Logger) LoggerFactory.getLogger("Shiina-Web");
    public static Dotenv loggerEnv;
    public static Dotenv env;
    public static Map<String, Object> customization;
    public static JedisPooled jedisPool;
    public static WebServer webServer;

    public static String version = "1.6prod";
    public static String dbVersion = "1.6";

    public static boolean devMode = false;

    public static ShiinaRankCache rankCache;
    public static ShiinaMultiDetection multiDetection;
    public static DatabaseCleanUp databaseCleanUp;
    public static HourlyPlayerStatsThread hourlyPlayerStatsThread;

    public static void main(String[] args) throws SQLException {

        // Register shutdown hook early
        Runtime.getRuntime().addShutdownHook(new Shutdown());

        if (args.length > 0 && args[0].equals("-dev")) {
            devMode = true;
            log.info("Running shiina in development mode, do not use this in production!");
            log.info("Also some stuff will not work when not running a prod instance in the bg!");
        }

        env = Dotenv.configure().directory(".config/").load();
        loggerEnv = Dotenv.configure().directory(".config/").filename("logger.env").load();
        log.info("Shiina-Web Rewrite " + version);
        Init init = new Init();
        init.initializeDataDirectory();
        init.initializeRedisConfiguration();
        init.initializeJettyLogging();
        init.initializeDatabase();
        init.initializeJedis();
        if (!devMode)
            init.initializeAutorunSQL();
        customization = init.initializeCustomizations();

        ThemeLoader.loadThemes();

        webServer = new WebServer();
        init.initializeWebServer(webServer);
        init.initializeOkHttpCacheReset();

        RobotJsonConfig robotJsonConfig = new RobotJsonConfig();
        robotJsonConfig.updateRobotsTxt();

        ModuleRegister.registerDefaultModule("home", new BigHeader());
        ModuleRegister.registerDefaultModule("home", ShiinaModule.fromRawHtml("HomeInfos", "infos", "home/infos.html"));
        ModuleRegister.registerDefaultModule("home", new MoreInfos());

        WebServer.get("/health", new Health());

        WebServer.get("/user/:handle", new GucchoUserRedirect());
        WebServer.get("/beatmapset/:set", new GucchoBmRedirect());

        WebServer.get("/", new Home());
        WebServer.get("/beatmaps", new Beatmaps());
        WebServer.get("/leaderboard", new Leaderboard());

        WebServer.get("/clans", new Clans());
        WebServer.get("/clan/:id", new Clan());
        WebServer.get("/clan/:id/settings", new ManageClan());
        WebServer.get("/scores/:id", new UserScore());
        WebServer.get("/b/:id", new Beatmap());
        WebServer.get("/u/1", new Bot());
        WebServer.get("/u/:id", new User());

        WebServer.get("/settings", new Settings());
        WebServer.get("/friends", new Relations());
        WebServer.post("/settings/avatar", new HandleAvatarChange());
        WebServer.post("/clan/:id/avatar", new HandleClanAvatarChange());
        WebServer.post("/settings/country", new HandleFlagChange());
        WebServer.post("/settings/name", new HandleNameChange());
        WebServer.post("/settings/mode", new HandleModeChange());
        WebServer.post("/settings/userpage", new HandleUserpageChange());
        WebServer.post("/settings/banner", new HandleBannerChange());
        WebServer.post("/settings/badge", new HandleBadgeChange());

        WebServer.get("/login", new Login());
        WebServer.get("/register", new Register());
        WebServer.get("/auth/recover", new Recover());
        WebServer.post("/recover", new HandleRecovery());
        WebServer.post("/register", new HandleRegister());
        WebServer.post("/login", new HandleLogin());
        WebServer.post("/logout", new HandleLogout());

        WebServer.post("/post/comment", new HandleComment());
        WebServer.notFound(new NotFound());

        WebServer.get("/api/v1/get_ap_players", new GetLastDayPlayerAdmin());
        WebServer.get("/api/v1/get_comments", new GetComments());
        WebServer.get("/api/v1/get_first_places", new GetFirstPlaces());
        WebServer.get("/api/v1/get_player_scores", new GetPlayerScores());
        WebServer.get("/api/v1/get_rank_cache", new GetRankCache());
        WebServer.get("/api/v1/get_playcount_graph", new GetPlaycountGraph());
        WebServer.get("/api/v1/search", new Search());
        WebServer.get("/api/v1/thumb", new GetBmThumbnail());

        WebServer.get("/api/v1/manage_cl", new HandleClanAction());
        WebServer.get("/api/v1/join_clan", new HandleClanRequest());
        WebServer.get("/api/v1/update_rel", new HandleRelationship());
        WebServer.post("/api/v1/handle_favorite", new HandleBeatmapFavorite());

        WebServer.get("/ap/users/recovery", new RecoverAccount());

        WebServer.get("/ap/bancho", new Bancho());
        WebServer.get("/ap/api/handler", new PubSubHandler());
        WebServer.get("/ap/settings", new ModularSettings());

        WebServer.get("/ap/maprequests", new MapRequests());
        WebServer.get("/ap/mapranking", new MapRanking());

        WebServer.get("/ap/clan", new dev.osunolimits.routes.ap.get.ClanModeration());
        WebServer.get("/ap/clans", new dev.osunolimits.routes.ap.get.ClansModeration());
        WebServer.get("/ap/multiaccs", new Multiaccounts());
        WebServer.get("/ap/audit", new Audit());
        WebServer.get("/ap/chat", new ChatExplorer());
        WebServer.get("/ap/start", new Start());
        WebServer.get("/ap/commands", new Commands());
        WebServer.get("/ap/themes", new Themes());
        WebServer.get("/ap/system", new System());
        WebServer.get("/ap/system/cons", new SystemConnections());
        WebServer.get("/ap/system/threads", new SystemThreads());
        WebServer.get("/ap/groups", new Groups());
        WebServer.get("/ap/groups/create", new ManageGroup(Action.CREATE));
        WebServer.get("/ap/groups/edit", new ManageGroup(Action.EDIT));
        WebServer.get("/ap/groups/delete", new ManageGroup(Action.DELETE));
        WebServer.post("/ap/groups/manage", new ProcessManageGroup());
        WebServer.get("/ap/groups/process", new ProcessGroup());
        WebServer.get("/ap/users", new Users());
        WebServer.get("/ap/user", new ApUser());
        WebServer.post("/ap/themes/change", new ChangeTheme());
        WebServer.post("/ap/settings/update", new ChangeSetting());
        WebServer.post("/ap/maprequests/deny", new DenyMapRequest());
        WebServer.post("/ap/mapranking", new HandleMapStatusUpdate());
        WebServer.post("/ap/clan/delete", new dev.osunolimits.routes.ap.post.HandleClanDelete());
        WebServer.post("/ap/clan/avatar/delete", new dev.osunolimits.routes.ap.post.HandleClanAvatarDelete());

        WebServer.get("/banner/:id", new GetBanner());
        GroupRegistry.revalidate();

        UserInfoCache userInfoCache = new UserInfoCache();
        userInfoCache.populateIfNeeded();

        MonetizationConfig config = new MonetizationConfig();
        if (config.ENABLED) {
            NavbarItem item = new NavbarItem("Donate", "donate");
            item.setLoggedInOnly(true);
            NavbarRegister.register(item);
            Spark.post("/handlekofi", new KofiDonoHandler());
            Spark.get("/donate", new Donate(config, item.getActNav()));
        }

        ShiinaDocs shiinaDocs = new ShiinaDocs();
        shiinaDocs.initializeDocs();

        PluginLoader pluginLoader = new PluginLoader();
        pluginLoader.loadPlugins();

        ModuleRegister.reloadModuleConfigurations();

        rankCache = new ShiinaRankCache();
        multiDetection = new ShiinaMultiDetection();

        databaseCleanUp = new DatabaseCleanUp();
        databaseCleanUp.start();

        hourlyPlayerStatsThread = new HourlyPlayerStatsThread();
        hourlyPlayerStatsThread.start();

    }

}
