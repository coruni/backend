package com.Fanbbs.web;

import com.Fanbbs.common.*;
import com.Fanbbs.entity.*;
import com.Fanbbs.service.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
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

    /***
     * 获取数据库中的配置
     */
    @RequestMapping(value = "/getApiConfig")
    @ResponseBody
    public String getApiConfig(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Boolean permission = permission(token);
            if (!permission) return Result.getResultJson(201, "无权限", null);
            Apiconfig apiconfig = apiconfigService.selectByKey(1);
            Map<String, Object> data = JSONObject.parseObject(JSONObject.toJSONString(apiconfig), Map.class);
            data.remove("levelExp");
            return Result.getResultJson(200, "获取成功", data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     * 配置修改
     */
    @RequestMapping(value = "/apiConfigUpdate")
    @ResponseBody
    public String apiConfigUpdate(@RequestParam(value = "params", required = false) String params,
                                  HttpServletRequest request) {
        try {
            Apiconfig update = new Apiconfig();
            Boolean permission = permission(request.getHeader("Authorization"));
            if (!permission) return Result.getResultJson(201, "无权限", null);
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

            return Result.getResultJson(200, "修改完成", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     * 修改应用
     */
    @RequestMapping(value = "/update")
    @ResponseBody
    public String updateApp(@RequestParam(value = "params", required = false) String params,
                            HttpServletRequest request) {
        try {
            Boolean permission = permission(request.getHeader("Authorization"));
            if (!permission) return Result.getResultJson(201, "无权限", null);
            App update = new App();
            if (StringUtils.isNotBlank(params)) {
                JSONObject object = JSON.parseObject(params);
                update = object.toJavaObject(App.class);
                update.setId(1);
            }
            appService.update(update);
            Apiconfig apiconfig = UStatus.getConfig(this.dataprefix, apiconfigService, redisTemplate);
            return Result.getResultJson(200, "修改完成", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口请求异常", null);
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
        if (token == null || token.isEmpty()) return false;
        DecodedJWT verify = JWT.verify(token);
        Users user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
        if (user.getUid() == null) return false;
        if (user.getGroup().equals("administrator") || user.getGroup().equals("editor")) {
            return true;
        }
        return false;
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
