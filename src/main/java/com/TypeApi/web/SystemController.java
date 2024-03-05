package com.TypeApi.web;

import com.TypeApi.common.*;
import com.TypeApi.entity.*;
import com.TypeApi.service.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.http.HttpRequest;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 接口系统控制器，负责在线修改配置文件，在线重启RuleAPI接口
 */
@Controller
@RequestMapping(value = "/system")
public class SystemController {

    ResultAll Result = new ResultAll();
    EditFile editFile = new EditFile();
    HttpClient HttpClient = new HttpClient();
    RedisHelp redisHelp = new RedisHelp();


    @Autowired
    private ApiconfigService apiconfigService;

    @Autowired
    private AdsService adsService;

    @Autowired
    private PushService pushService;

    @Autowired
    private UsersService usersService;

    @Autowired
    private HomepageService homepageService;

    @Autowired
    private AppService appService;

    @Autowired
    private RedisTemplate redisTemplate;
    UserStatus UStatus = new UserStatus();


    @Value("${webinfo.key}")
    private String key;

    @Value("${web.prefix}")
    private String dataprefix;
    /**
     * 密钥配置
     */
    private String webinfoKey;


    /**
     * 缓存配置
     */
    private String usertime;
    private String contentCache;
    private String contentInfoCache;
    private String CommentCache;
    private String userCache;
    /**
     * 邮箱配置
     */
    private String mailHost;
    private String mailUsername;
    private String mailPassword;
    /**
     * Mysql配置
     */
    private String dataUrl;
    private String dataUsername;
    private String dataPassword;
    private String dataPrefix;


    /***
     * 缓存配置
     */
    @RequestMapping(value = "/setupCache")
    @ResponseBody
    public String setupCache(@RequestParam(value = "webkey", required = false) String webkey, @RequestParam(value = "params", required = false) String params) {
        if (webkey.length() < 1) {
            return Result.getResultJson(0, "请输入正确的访问key", null);
        }
        if (!webkey.equals(this.key)) {
            return Result.getResultJson(0, "请输入正确的访问key", null);
        }
        Map jsonToMap = new HashMap();
        try {
            //读取参数，开始写入
            if (StringUtils.isNotBlank(params)) {
                jsonToMap = JSONObject.parseObject(JSON.parseObject(params).toString());
                //新的配置

            }
            String new_usertime = "";
            String new_contentCache = "";
            String new_contentInfoCache = "";
            String new_CommentCache = "";
            String new_userCache = "";

            String usertime = "webinfo.usertime=";
            String contentCache = "webinfo.contentCache=";
            String contentInfoCache = "webinfo.contentInfoCache=";
            String CommentCache = "webinfo.CommentCache=";
            String userCache = "webinfo.userCache=";
            //老的配置
            String old_usertime = usertime + this.usertime;
            String old_contentCache = contentCache + this.contentCache;
            String old_contentInfoCache = contentInfoCache + this.contentInfoCache;
            String old_CommentCache = CommentCache + this.CommentCache;
            String old_userCache = userCache + this.userCache;
            //新的配置

            if (jsonToMap.get("usertime") != null) {
                new_usertime = usertime + jsonToMap.get("usertime").toString();
            } else {
                new_usertime = usertime;
            }
            editFile.replacTextContent(old_usertime, new_usertime);
            if (jsonToMap.get("contentCache") != null) {
                new_contentCache = contentCache + jsonToMap.get("contentCache").toString();
            } else {
                new_contentCache = contentCache;
            }
            editFile.replacTextContent(old_contentCache, new_contentCache);
            if (jsonToMap.get("contentInfoCache") != null) {
                new_contentInfoCache = contentInfoCache + jsonToMap.get("contentInfoCache").toString();
            } else {
                new_contentInfoCache = contentInfoCache;
            }
            editFile.replacTextContent(old_contentInfoCache, new_contentInfoCache);
            if (jsonToMap.get("CommentCache") != null) {
                new_CommentCache = CommentCache + jsonToMap.get("CommentCache").toString();
            } else {
                new_CommentCache = CommentCache;
            }
            editFile.replacTextContent(old_CommentCache, new_CommentCache);

            if (jsonToMap.get("userCache") != null) {
                new_userCache = userCache + jsonToMap.get("userCache").toString();
            } else {
                new_userCache = userCache;
            }
            editFile.replacTextContent(old_userCache, new_userCache);
            return Result.getResultJson(1, "修改成功，手动重启后生效", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(1, "修改失败，请确认参数是否正确", null);
        }
    }


    /***
     * 获取数据库中的配置
     */
    @RequestMapping(value = "/getApiConfig")
    @ResponseBody
    public String getApiConfig(@RequestParam(value = "webkey", required = false) String webkey) {
        if (webkey.length() < 1) {
            return Result.getResultJson(0, "请输入正确的访问key", null);
        }
        if (!webkey.equals(this.key)) {
            return Result.getResultJson(0, "请输入正确的访问key", null);
        }
        Apiconfig apiconfig = apiconfigService.selectByKey(1);
        Map<String, Object> data = JSONObject.parseObject(JSONObject.toJSONString(apiconfig), Map.class);
        data.remove("levelExp");
        return Result.getResultJson(200, "获取成功", data);
    }

    /***
     * 配置修改
     */
    @RequestMapping(value = "/apiConfigUpdate")
    @ResponseBody
    public String apiConfigUpdate(@RequestParam(value = "params", required = false) String params, @RequestParam(value = "webkey", required = false) String webkey) {
        Apiconfig update = null;
        if (webkey.isEmpty()) {
            return Result.getResultJson(0, "请输入正确的访问key", null);
        }
        if (!webkey.equals(this.key)) {
            return Result.getResultJson(0, "请输入正确的访问key", null);
        }
        if (StringUtils.isNotBlank(params)) {
            JSONObject object = JSON.parseObject(params);
            update = object.toJavaObject(Apiconfig.class);
        }
        update.setId(1);
        int rows = apiconfigService.update(update);
        //更新Redis缓存
        Apiconfig apiconfig = apiconfigService.selectByKey(1);
        Map configJson = JSONObject.parseObject(JSONObject.toJSONString(apiconfig), Map.class);
        redisHelp.delete(dataprefix + "_" + "config", redisTemplate);
        redisHelp.setKey(dataprefix + "_" + "config", configJson, 6000, redisTemplate);
        JSONObject response = new JSONObject();
        response.put("code", rows);
        response.put("msg", rows > 0 ? "修改成功，当前配置已生效！" : "修改失败");
        return response.toString();
    }


    /***
     * 初始化APP
     */
    @RequestMapping(value = "/initApp")
    @ResponseBody
    public String initApp(@RequestParam(value = "webkey", required = false, defaultValue = "") String webkey) {

        try {
            if (!webkey.equals(this.key)) {
                return Result.getResultJson(201, "Key错误", null);
            }
            App app = new App();
            Integer total = appService.total(app);
            if (total < 1) {
                app.setName("应用名称");
                app.setCurrencyName("积分");
                appService.insert(app);
            } else {
                return Result.getResultJson(201, "无需初始化", null);
            }
            return Result.getResultJson(200, "已初始化完成", null);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口错误", null);
        }
    }

    /***
     * 修改应用
     */
    @RequestMapping(value = "/update")
    @ResponseBody
    public String updateApp(@RequestParam(value = "webkey", required = false, defaultValue = "") String webkey,
                            @RequestParam(value = "params", required = false) String params) {

        App update = null;
        if (!webkey.equals(this.key)) {
            return Result.getResultJson(201, "Key错误", null);
        }
        try {
            if (StringUtils.isNotBlank(params)) {
                JSONObject object = JSON.parseObject(params);
                update = object.toJavaObject(App.class);
                update.setId(1);
            }
            int rows = appService.update(update);
            Apiconfig apiconfig = UStatus.getConfig(this.dataprefix, apiconfigService, redisTemplate);
            if (rows > 0) {
                redisHelp.delete(this.dataprefix + "_" + "appList", redisTemplate);
                return Result.getResultJson(200, "修改完成", null);
            } else {
                return Result.getResultJson(201, "修改失败", null);

            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口请求异常，请联系管理员", null);
        }
    }


    /***
     * APP首页配置添加
     */
    @RequestMapping(value = "/appHomepageAdd")
    @ResponseBody
    public String appHomepage(@RequestParam(value = "page") String page,
                              @RequestParam(value = "type", required = false, defaultValue = "0") Integer type,
                              @RequestParam(value = "name") String name,
                              @RequestParam(value = "image", required = false) String image,
                              @RequestParam(value = "enable", required = false, defaultValue = "1") Integer enable,
                              HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (!permission(token)) return Result.getResultJson(201, "无权限", null);
            Homepage homepage = new Homepage();
            homepage.setType(type);
            homepage.setEnable(enable);
            homepage.setName(name);
            homepage.setPage(page);
            homepage.setImage(image);
            homepage.setCreated((int) (System.currentTimeMillis() / 1000));
            homepageService.insert(homepage);
            return Result.getResultJson(200, "添加成功", null);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    @RequestMapping(value = "/appHomepageUpdate")
    @ResponseBody
    public String appHomepageUpdate(@RequestParam(value = "id") Integer id,
                                    @RequestParam(value = "name") String name,
                                    @RequestParam(value = "page") String page,
                                    @RequestParam(value = "image") String image,
                                    @RequestParam(value = "enable") Integer enable,
                                    @RequestParam(value = "type") Integer type,
                                    HttpServletRequest request) {
        try {
            if (!permission(request.getHeader("Authorization"))) return Result.getResultJson(201, "无权限", null);
            // 查找id是否存在
            Homepage homepage = homepageService.selectByKey(id);
            if (homepage == null || homepage.toString().isEmpty()) return Result.getResultJson(201, "数据不存在", null);
            System.out.println(homepage);
            homepage.setPage(page);
            homepage.setName(name);
            homepage.setImage(image);
            homepage.setEnable(enable);
            homepage.setType(type);
            homepageService.update(homepage);
            return Result.getResultJson(200, "修改成功", null);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    @RequestMapping(value = "/appHomepageDelete")
    @ResponseBody
    public String appHomepageDelete(@RequestParam(value = "id") Integer id,
                                    HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (!permission(token)) return Result.getResultJson(201, "无权限", null);
            Homepage homepage = homepageService.selectByKey(id);
            if (homepage == null || homepage.toString().isEmpty())
                return Result.getResultJson(201, "数据存不存在", null);

            homepageService.delete(id);
            return Result.getResultJson(200, "删除成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);

        }

    }

    @RequestMapping(value = "/appHomepage")
    @ResponseBody
    public String appHomepage(@RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                              @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
                              @RequestParam(value = "order", required = false, defaultValue = "created desc") String order) {
        try {
            Map<String, Object> data = new HashMap<>();
            Homepage homepage = new Homepage();
            PageList<Homepage> homepagePageList = homepageService.selectPage(homepage, page, limit, order);
            List<Homepage> homepageList = homepagePageList.getList();
            data.put("data", homepageList);
            data.put("count", homepageList.size());

            return Result.getResultJson(200, "获取成功", data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    private boolean permission(String token) {
        if (token != null && !token.isEmpty()) {
            DecodedJWT verify = JWT.verify(token);
            Users user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
            if (user.getGroup().equals("administrator") || user.getGroup().equals("editor")) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /***
     * 查询APP详情
     */
    @RequestMapping(value = "/app")
    @ResponseBody
    public String app() {
        try {
            Map appJson = new HashMap<String, String>();
            Map cacheInfo = redisHelp.getMapValue(this.dataprefix + "_" + "appJson_1", redisTemplate);
            // 查询homePage
            Homepage homepage = new Homepage();
            homepage.setEnable(1);

            List<Homepage> homepageList = homepageService.selectList(homepage);
            Map<String, Object> data = new HashMap<>();
            if (cacheInfo.size() > 0) {
                appJson = cacheInfo;
            } else {
                App app = appService.selectByKey(1);
                if (app == null) {
                    return Result.getResultJson(201, "应用不存在或密钥错误", null);
                }
                data.put("app", app);
                data.put("appHomepage", homepageList);

                redisHelp.delete(this.dataprefix + "_" + "appJson_1", redisTemplate);
                redisHelp.setKey(this.dataprefix + "_" + "appJson_1", appJson, 10, redisTemplate);

            }
            return Result.getResultJson(200, "获取成功", data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口错误", null);
        }

    }

    @RequestMapping(value = "/vip")
    @ResponseBody
    public String vip() {
        try {
            Apiconfig apiconfig = UStatus.getConfig(dataprefix, apiconfigService, redisTemplate);
            Map<String, Object> data = new HashMap<>();
            data.put("vipPrice", apiconfig.getVipPrice());
            data.put("vipDiscount", apiconfig.getVipDiscount());
            data.put("vipDay", apiconfig.getVipDay());
            data.put("ratio", apiconfig.getScale());

            return Result.getResultJson(200, "获取成功", data);


        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

}
