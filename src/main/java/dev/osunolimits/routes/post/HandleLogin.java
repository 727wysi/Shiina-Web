package dev.osunolimits.routes.post;

import java.sql.ResultSet;

import com.google.gson.Gson;

import dev.osunolimits.externals.TurnstileQuery;
import dev.osunolimits.main.App;
import dev.osunolimits.modules.Shiina;
import dev.osunolimits.modules.ShiinaRoute;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import dev.osunolimits.modules.utils.UserInfoCache;
import dev.osunolimits.utils.Auth;
import dev.osunolimits.utils.Auth.SessionUser;
import okhttp3.OkHttpClient;
import spark.Request;
import spark.Response;

public class HandleLogin extends Shiina {

    private final Gson GSON;

    public HandleLogin() {
        GSON = new Gson();
    }


    @Override
    public Object handle(Request req, Response res) throws Exception {
        ShiinaRequest shiina = new ShiinaRoute().handle(req, res);
        shiina.data.put("actNav", 0);
    
        String captchaResponse = req.queryParams("cf-turnstile-response");

        if(req.cookie("shiina") != null) {
            if(App.jedisPool.get("shiina:auth:" + req.cookie("shiina")) != null) {
                shiina.data.put("info", "You are already logged in");
                return renderTemplate("login.html", shiina, res, req);
            }
        }

        if(captchaResponse == null || captchaResponse.isEmpty()) {
            shiina.data.put("error", "Invalid Captcha");
            return renderTemplate("login.html", shiina, res, req);
        }

        String input = req.queryParams("input");

        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@" +
                            "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";

        if(input == null || input.isEmpty()) {
            shiina.data.put("error", "Invalid (Username/Mail) or Password");
            return renderTemplate("login.html", shiina, res, req);
        }

        String password = req.queryParams("password");
        if(password == null || password.isEmpty()) {
            shiina.data.put("error", "Invalid (Username/Mail) or Password");
            return renderTemplate("login.html", shiina, res, req);
        }
        boolean rememberMe = false;
        if(req.queryParams("remember") != null) {
            rememberMe = true;
        }

        TurnstileQuery turnstileQuery = new TurnstileQuery(new OkHttpClient());
        if(!turnstileQuery.verifyCaptcha(captchaResponse).success) {
            shiina.data.put("error", "Invalid Captcha");
            return renderTemplate("login.html", shiina, res, req);
        }

        String selectSql = "";
        String mailSql = "SELECT `safe_name`, `pw_bcrypt`, `name`, `id`, `priv`, `email` FROM `users` WHERE `email` = ?";
        String nameSql = "SELECT `safe_name`, `pw_bcrypt`, `name`, `id`, `priv`, `email` FROM `users` WHERE `name` = ?";

        if(input.matches(emailRegex)) {
            selectSql = mailSql;
        } else {
            selectSql = nameSql;
        }

        ResultSet validationRs = shiina.mysql.Query(selectSql, input);
        if(!validationRs.next()) {
            shiina.data.put("error", "Invalid (Username/Mail) or Password");
            return renderTemplate("login.html", shiina, res, req);
        }
        String pwBcrypt = validationRs.getString("pw_bcrypt");

        if(!Auth.checkPw(password, pwBcrypt)) {
            shiina.data.put("error", "Invalid (Username/Mail) or Password");
            return renderTemplate("login.html", shiina, res, req);
        }

        String token = Auth.generateNewToken();

        int userId = validationRs.getInt("id");
        UserInfoCache userInfoCache = new UserInfoCache();
        userInfoCache.reloadUserIfNotPresent(userId);

        SessionUser user = new Auth().new SessionUser();
        user.id = validationRs.getInt("id");
        user.created = (int) (System.currentTimeMillis() / 1000L);
        user.ip = req.ip();

        String userJson = GSON.toJson(user);

        App.jedisPool.setex("shiina:auth:"+token, 604800, userJson);

        if(rememberMe) {
            res.cookie("shiina", token, 604800);
        }else {
            res.cookie("shiina", token);
        }

        res.redirect("/?register=success");
        return notFound(res, shiina);
    }

    
}
