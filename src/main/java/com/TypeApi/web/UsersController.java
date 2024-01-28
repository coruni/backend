package com.TypeApi.web;

import com.TypeApi.common.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.TypeApi.entity.*;
import com.TypeApi.service.*;
import com.alibaba.fastjson.TypeReference;
import com.auth0.jwt.interfaces.DecodedJWT;
import net.dreamlu.mica.core.result.R;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Controller;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.mail.MessagingException;
import javax.rmi.CORBA.Util;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

/**
 * 控制层
 * TypechoUsersController
 *
 * @author buxia97
 * @date 2021/11/29
 */
@Component
@Controller
@RequestMapping(value = "/user")
public class UsersController {

    @Autowired
    UsersService service;

    @Autowired
    private ArticleService contentsService;

    @Autowired
    private CommentsService commentsService;

    @Autowired
    private UserlogService userlogService;

    @Autowired
    private UserapiService userapiService;

    @Autowired
    private HeadpictureService headpictureService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private PaylogService paylogService;

    @Autowired
    private ApiconfigService apiconfigService;

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private InboxService inboxService;

    @Autowired
    private FanService fanService;

    @Autowired
    private ViolationService violationService;

    @Autowired
    private PushService pushService;


    @Autowired
    MailService MailService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;


    @Value("${mybatis.configuration.variables.prefix}")
    private String prefix;

    @Value("${webinfo.usertime}")
    private Integer usertime;

    @Value("${webinfo.userCache}")
    private Integer userCache;


    @Value("${web.prefix}")
    private String dataprefix;


    RedisHelp redisHelp = new RedisHelp();
    ResultAll Result = new ResultAll();
    baseFull baseFull = new baseFull();
    UserStatus UStatus = new UserStatus();
    HttpClient HttpClient = new HttpClient();
    PHPass phpass = new PHPass(8);
    EditFile editFile = new EditFile();


    /***
     * 用户列表
     */
    @RequestMapping(value = "/userList")
    @ResponseBody
    public String userList(@RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                           @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
                           @RequestParam(value = "params", required = false) String params,
                           @RequestParam(value = "searchKey", required = false) String searchKey,
                           @RequestParam(value = "order", required = false, defaultValue = "created desc") String order,
                           @RequestParam(value = "random", required = false, defaultValue = "1") Integer random,
                           HttpServletRequest request) {
        try {
            limit = limit > 50 ? 50 : limit;
            String token = request.getHeader("Authorization");
            Boolean permission = false;
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = service.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user.getGroup().equals("administrator") || user.getGroup().equals("editor")) permission = true;
            }
            // 获取查询参数
            Users query = new Users();
            if (StringUtils.isNotBlank(params)) {
                query = JSONObject.parseObject(params, Users.class);
            }
            //查询
            PageList<Users> userPage = service.selectPage(query, page, limit, searchKey, order, random);
            List<Users> userList = userPage.getList();
            JSONArray dataList = new JSONArray();
            for (Users item : userList) {
                // 转Map数据
                Map<String, Object> data = JSONObject.parseObject(JSONObject.toJSONString(item), new TypeReference<Map<String, Object>>() {
                });
                // 格式化数据
                JSONObject opt = new JSONObject();
                JSONArray head_pircture = new JSONArray();
                JSONObject address = new JSONObject();
                opt = item.getOpt() != null && !item.getOpt().toString().isEmpty() ? JSONObject.parseObject(item.getOpt().toString()) : null;
                address = item.getAddress() != null && !item.getAddress().toString().isEmpty() ? JSONObject.parseObject(item.getAddress().toString()) : null;
                // 处理头像框
                // 加入其他数据等级等
                List result = baseFull.getLevel(item.getExperience());
                Integer level = (Integer) result.get(0);
                Integer nextLevel = (Integer) result.get(1);

                // 处理会员
                Integer isVip = 0;
                if (System.currentTimeMillis() / 1000 > item.getVip()) isVip = 1;

                // 处理关注
                Userlog userlog = new Userlog();
                userlog.setUid(user.getUid());
                userlog.setToid(item.getUid());
                Integer isFollow = userlogService.total(userlog);

                // 加入数据
                data.put("address", address);
                data.put("opt", opt);
                data.put("isFollow",isFollow);
                data.put("level", level);
                data.put("isVip", isVip);
                data.put("nextLevel", nextLevel);
                // 移除铭感数据
                data.remove("password");
                if (!permission) {
                    data.remove("mail");
                    data.remove("assets");
                    data.remove("address");
                }
                dataList.add(data);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("data", dataList);
            data.put("count", userList.size());
            data.put("total", service.total(query, searchKey));
            data.put("page", page);
            return Result.getResultJson(200, "获取成功", data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     * 用户数据
     */
    @RequestMapping(value = "/userData")
    @ResponseBody
    public String userData(@RequestParam(value = "id", required = false) Integer id, HttpServletRequest request) {
        try {
            Map data = new HashMap<>();
            Integer uid = id;
            String token = request.getHeader("Authorization");
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                uid = Integer.parseInt(verify.getClaim("aud").asString());
            }
            if (uid != null && !uid.toString().isEmpty()) {
                // 获取文章数量
                Article article = new Article();
                article.setAuthorId(uid);
                article.setStatus("publish");
                Integer articleNum = contentsService.total(article, null);

                // 获取粉丝数量
                Fan fan = new Fan();
                fan.setTouid(uid);
                Integer fans = fanService.total(fan);

                // 获取关注数量
                fan.setUid(uid);
                fan.setTouid(null);
                Integer follows = fanService.total(fan);

                // 是否签到
                Userlog log = new Userlog();
                log.setUid(uid);
                log.setType("clock");
                List<Userlog> logList = userlogService.selectList(log);
                Integer clock = 0;
                if (logList.size() > 0) {
                    log = logList.get(0);
                    Long timeStmap = System.currentTimeMillis();
                    Long clockTime = Long.valueOf(log.getCreated());
                    // 将时间格式化为yyMMdd
                    SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
                    String currentTimeFormatted = sdf.format(new Date(timeStmap));
                    String createdTimeFormatted = sdf.format(new Date(clockTime));

                    if (currentTimeFormatted.equals(createdTimeFormatted)) {
                        clock = 1;
                    }
                }
                // 获取评论
                Comments comment = new Comments();
                comment.setUid(uid);
                Integer comments = commentsService.total(comment, null);
                // 加入数据
                data.put("articles", articleNum);
                data.put("fans", fans);
                data.put("follows", follows);
                data.put("clock", clock);
                data.put("comments", comments);
            }

            // 用户数据
            return Result.getResultJson(200, "请求成功", data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     * 用户信息
     */
    @RequestMapping(value = "/userInfo")
    @ResponseBody
    public String userInfo(@RequestParam(value = "id", required = false) Integer id, HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Integer isFollow = 0;
            Integer fromFollow = 0;
            Integer related = 0;
            Integer isVip = 0;
            Users user = new Users();
            Users own = new Users();
            if (id != null && !id.equals(0)) {
                user = service.selectByKey(id);
                if (user == null || user.toString().isEmpty()) return Result.getResultJson(201, "用户不存在", null);
            }
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                own = service.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (own == null || own.toString().isEmpty()) return Result.getResultJson(201, "用户不存在", null);
                // 获取是否关注和互相关注
                Fan fan = new Fan();
                if (!own.getUid().equals(user.getUid())) {
                    fan.setTouid(user.getUid());
                    fan.setUid(own.getUid());
                    isFollow = fanService.total(fan);
                    // 他是否关注我
                    fan.setTouid(own.getUid());
                    fan.setUid(user.getUid());
                    fromFollow = fanService.total(fan);
                    if (isFollow.equals(fromFollow)) related = 1;
                }
            }
            // 处理opt、地址以及头像框
            JSONObject opt = new JSONObject();
            JSONObject address = new JSONObject();
            opt = user.getOpt() != null && !user.getOpt().toString().isEmpty() ? JSONObject.parseObject(user.getOpt()) : null;
            address = user.getAddress() != null && !user.getAddress().toString().isEmpty() ? JSONObject.parseObject(user.getAddress()) : null;

            // 处理会员
            if (user.getVip() > System.currentTimeMillis() / 1000) isVip = 1;
            Map<String, Object> data = JSONObject.parseObject(JSONObject.toJSONString(user), Map.class);
            // 加入数据
            data.put("address", address);
            data.put("opt", opt);
            data.put("isFollow", isFollow);
            data.put("related", related);
            data.put("isVip", isVip);
            // 移除敏感数据
            data.remove("password");
            if (!own.getUid().equals(user.getUid()) &&
                    !("administrator".equals(user.getGroup()) || "editor".equals(user.getGroup()))) {
                data.remove("assets");
                data.remove("address");
                data.remove("mail");
            }
            return Result.getResultJson(200, "获取成功", data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     * 登陆
     * @param account 账号
     */
    @RequestMapping(value = "/login")
    @ResponseBody
    public String login(@RequestParam(value = "account") String account,
                        @RequestParam(value = "password") String password,
                        HttpServletRequest request) {

        try {
            if (account.isEmpty() || password.isEmpty()) {
                return Result.getResultJson(200, "账号密码不可为空", null);
            }

            // 检查用户是否存在
            CheckUserResult userResult = hasUser(account);
            if (!userResult.hasUser) {
                return Result.getResultJson(201, "用户不存在", null);
            }
            Users user = userResult.user;
            // 验证密码
            Boolean isPass = phpass.CheckPassword(password, user.getPassword());
            if (!isPass) {
                return Result.getResultJson(201, "密码错误", null);
            }
            // 生成Token
            Map token = new HashMap<>();
            token.put("sub ", "login");
            token.put("aud", user.getUid().toString());
            Map<String, Object> data = JSONObject.parseObject(JSONObject.toJSONString(user), new TypeReference<Map<String, Object>>() {
            });

            if (user.getAddress() != null && !user.getAddress().isEmpty()) {
                data.put("address", JSONObject.parseObject(user.getAddress()));
            }
            // 加入数据
            data.put("token", JWT.getToken(token));
            // 清除敏感数据
            data.remove("password");
            // 返回用户信息或者其他操作
            return Result.getResultJson(200, "登录成功", data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }

    }

    //验证用户是否存在
    public class CheckUserResult {
        private boolean hasUser;
        private Users user;

        public CheckUserResult(boolean hasUser, Users user) {
            this.hasUser = hasUser;
            this.user = user;
        }
    }

    private CheckUserResult hasUser(String account) {
        // 查询用户是否存在
        Boolean isEmail = baseFull.isEmail(account);
        Users users = new Users();
        if (isEmail) users.setMail(account);
        else users.setName(account);
        boolean hasUser = false;
        List<Users> userList = service.selectList(users);
        if (!userList.isEmpty()) {
            hasUser = true;
        }
        Users user = userList.isEmpty() ? null : userList.get(0);
        return new CheckUserResult(hasUser, user);
    }

    /***
     * 社会化登陆
     * @param params Bean对象JSON字符串
     */
    @RequestMapping(value = "/apiLogin")
    @ResponseBody
    public String apiLogin(@RequestParam(value = "params", required = false) String params, HttpServletRequest request) {

        Map jsonToMap = null;
        String oldpw = null;
        try {
            if (StringUtils.isNotBlank(params)) {
                jsonToMap = JSONObject.parseObject(JSON.parseObject(params).toString());
            } else {
                return Result.getResultJson(0, "请输入正确的参数", null);
            }
            String ip = baseFull.getIpAddr(request);
            Apiconfig apiconfig = UStatus.getConfig(this.dataprefix, apiconfigService, redisTemplate);
            Integer isInvite = apiconfig.getIsInvite();
            //如果是微信，则走两步判断，是小程序还是APP
            if (jsonToMap.get("appLoginType").toString().equals("weixin")) {

                //走官方接口获取accessToken和openid
                if (jsonToMap.get("js_code") == null) {
                    return Result.getResultJson(0, "APP配置异常，js_code参数不存在", null);
                }
                String js_code = jsonToMap.get("js_code").toString();
                if (jsonToMap.get("type").toString().equals("applets")) {
                    String requestUrl = "https://api.weixin.qq.com/sns/jscode2session?appid=" + apiconfig.getAppletsAppid() + "&secret=" + apiconfig.getAppletsSecret() + "&js_code=" + js_code + "&grant_type=authorization_code";
                    String res = HttpClient.doGet(requestUrl);
                    System.out.println(res);
                    if (res == null) {
                        return Result.getResultJson(0, "接口配置异常，微信官方接口请求失败", null);
                    }
                    System.out.println("微信登录小程序接口返回" + res);
                    HashMap data = JSON.parseObject(res, HashMap.class);
                    if (data.get("openid") == null) {
                        return Result.getResultJson(0, "接口配置异常，小程序openid获取失败", null);
                    }
                    jsonToMap.put("accessToken", data.get("openid"));
                    jsonToMap.put("openId", data.get("openid"));
                } else {
                    String requestUrl = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=" + apiconfig.getWxAppId() + "&secret=" + apiconfig.getWxAppSecret() + "&code=" + js_code + "&grant_type=authorization_code";
                    String res = HttpClient.doGet(requestUrl);
                    System.out.println(res);
                    if (res == null) {
                        return Result.getResultJson(0, "接口配置异常，微信官方接口请求失败", null);
                    }
                    System.out.println("微信登录app接口返回" + res);
                    HashMap data = JSON.parseObject(res, HashMap.class);
                    if (data.get("openid") == null) {
                        return Result.getResultJson(0, "接口配置异常，openid获取失败", null);
                    }
                    jsonToMap.put("accessToken", data.get("openid"));
                    jsonToMap.put("openId", data.get("openid"));
                }


            }

            //QQ也要走两步判断
            if (jsonToMap.get("appLoginType").toString().equals("qq")) {


                if (jsonToMap.get("type").toString().equals("applets")) {
                    if (jsonToMap.get("js_code") == null) {
                        return Result.getResultJson(0, "APP配置异常，js_code参数不存在", null);
                    }
                    String js_code = jsonToMap.get("js_code").toString();
                    //如果是小程序，走官方接口获取accessToken和openid


                    String requestUrl = "https://api.q.qq.com/sns/jscode2session?appid=" + apiconfig.getQqAppletsAppid() + "&secret=" + apiconfig.getQqAppletsSecret() + "&js_code=" + js_code + "&grant_type=authorization_code";
                    String res = HttpClient.doGet(requestUrl);
                    System.out.println("QQ接口返回" + res);
                    if (res == null) {
                        return Result.getResultJson(0, "接口配置异常，QQ官方接口请求失败", null);
                    }

                    HashMap data = JSON.parseObject(res, HashMap.class);
                    if (data.get("openid") == null) {
                        return Result.getResultJson(0, "接口配置异常，openid获取失败", null);
                    }
                    jsonToMap.put("accessToken", data.get("openid"));
                    jsonToMap.put("openId", data.get("openid"));
                } else {
                    if (jsonToMap.get("accessToken") == null) {
                        return Result.getResultJson(0, "登录配置异常，accessToken参数不存在", null);
                    }
                    jsonToMap.put("accessToken", jsonToMap.get("openId"));
                    jsonToMap.put("openId", jsonToMap.get("openId"));
                }
            } else {
                if (jsonToMap.get("accessToken") == null) {
                    return Result.getResultJson(0, "登录配置异常，accessToken参数不存在", null);
                }
            }
            Userapi userapi = JSON.parseObject(JSON.toJSONString(jsonToMap), Userapi.class);
            String openid = userapi.getOpenId();
            String loginType = userapi.getAppLoginType();
            Userapi isApi = new Userapi();
            isApi.setOpenId(openid);
            isApi.setAppLoginType(loginType);
            List<Userapi> apiList = userapiService.selectList(isApi);
            //大于0则走向登陆，小于0则进行注册
            if (apiList.size() > 0) {

                Userapi apiInfo = apiList.get(0);
                Users user = service.selectByKey(apiInfo.getUid().toString());
                //判断用户是否被封禁
                Integer bantime = user.getBantime();
                if (bantime.equals(1)) {
                    return Result.getResultJson(0, "你的账号已被永久封禁，如有疑问请联系管理员", null);
                } else {
                    Long date = System.currentTimeMillis();
                    Integer curtime = Integer.parseInt(String.valueOf(date).substring(0, 10));
                    if (bantime > curtime) {
                        return Result.getResultJson(0, "你的账号被暂时封禁，请耐心等待解封。", null);
                    }
                }
                Long date = System.currentTimeMillis();
                String Token = date + user.getName();
                jsonToMap.put("uid", user.getUid());

                //生成唯一性token用于验证
                jsonToMap.put("name", user.getName());
                jsonToMap.put("token", user.getName() + DigestUtils.md5DigestAsHex(Token.getBytes()));
                jsonToMap.put("time", date);
                jsonToMap.put("group", user.getGroup());
                jsonToMap.put("mail", user.getMail());
                jsonToMap.put("url", user.getUrl());
                jsonToMap.put("screenName", user.getScreenName());
                jsonToMap.put("customize", user.getCustomize());
                jsonToMap.put("introduce", user.getIntroduce());
                jsonToMap.put("experience", user.getExperience());
                //判断是否为VIP
                jsonToMap.put("vip", user.getVip());
                jsonToMap.put("isvip", 0);
                String curTime = String.valueOf(date).substring(0, 10);
                Integer viptime = user.getVip();
                if (viptime > Integer.parseInt(curTime) || viptime.equals(1)) {
                    jsonToMap.put("isvip", 1);
                }
                if (user.getAvatar() != null) {
                    jsonToMap.put("avatar", user.getAvatar());
                } else {
                    if (user.getMail() != null) {
                        if (user.getMail().indexOf("@qq.com") != -1) {
                            String qq = user.getMail().replace("@qq.com", "");
                            jsonToMap.put("avatar", "https://q1.qlogo.cn/g?b=qq&nk=" + qq + "&s=640");
                        } else {
                            jsonToMap.put("avatar", baseFull.getAvatar(apiconfig.getWebinfoAvatar(), user.getMail()));
                        }
                    } else {
                        jsonToMap.put("avatar", apiconfig.getWebinfoAvatar() + "null");
                    }
                }

                //获取用户等级
                Integer uid = user.getUid();
                Comments comments = new Comments();
                comments.setUid(uid);
                Integer lv = commentsService.total(comments, null);
                jsonToMap.put("lv", baseFull.getLv(lv));
                //更新用户登录时间和第一次登陆时间（满足typecho要求）
                String userTime = String.valueOf(date).substring(0, 10);
                Users updateuser = new Users();
                updateuser.setUid(user.getUid());
                updateuser.setLogged(Integer.parseInt(userTime));
                if (user.getLogged() == 0) {
                    updateuser.setActivated(Integer.parseInt(userTime));
                }

                Integer rows = service.update(updateuser);

                //删除之前的token后，存入redis(防止积累导致内存溢出，超时时间默认是24小时)
                String oldToken = redisHelp.getRedis(this.dataprefix + "_" + "userkey" + jsonToMap.get("name").toString(), redisTemplate);
                if (oldToken != null) {
                    redisHelp.delete(this.dataprefix + "_" + "userInfo" + oldToken, redisTemplate);
                }
                redisHelp.setRedis(this.dataprefix + "_" + "userkey" + jsonToMap.get("name").toString(), jsonToMap.get("token").toString(), this.usertime, redisTemplate);
                redisHelp.setKey(this.dataprefix + "_" + "userInfo" + jsonToMap.get("name").toString() + DigestUtils.md5DigestAsHex(Token.getBytes()), jsonToMap, this.usertime, redisTemplate);

                return Result.getResultJson(rows > 0 ? 1 : 0, rows > 0 ? "登录成功" : "登陆失败", jsonToMap);

            } else {
                //注册
                if (isInvite.equals(1)) {
                    return Result.getResultJson(0, "当前注册需要邀请码，请采用普通方式注册！", null);
                }

//                if (jsonToMap.get("headImgUrl") != null) {
//
//                }
                Users regUser = new Users();
                String name = baseFull.createRandomStr(5) + baseFull.createRandomStr(4);
                String p = baseFull.createRandomStr(9);
                String passwd = phpass.HashPassword(p);
                Long date = System.currentTimeMillis();
                String userTime = String.valueOf(date).substring(0, 10);
                regUser.setName(name);
                regUser.setCreated(Integer.parseInt(userTime));
                regUser.setGroup("subscriber");
                regUser.setScreenName(userapi.getNickName());
                regUser.setPassword(passwd.replaceAll("(\\\r\\\n|\\\r|\\\n|\\\n\\\r)", ""));
                if (jsonToMap.get("headImgUrl") != null) {
                    String headImgUrl = jsonToMap.get("headImgUrl").toString();
                    //QQ的接口头像要处理(垃圾腾讯突然修改了返回格式)
                    if (jsonToMap.get("appLoginType").toString().equals("qq")) {
                        headImgUrl = headImgUrl.replace("http://", "https://");
                        headImgUrl = headImgUrl.replace("&amp;", "&");
                    }
                    regUser.setAvatar(headImgUrl);
                }
                Integer to = service.insert(regUser);
                //注册完成后，增加绑定
                Integer uid = regUser.getUid();
                userapi.setUid(uid);
                int rows = userapiService.insert(userapi);
                //返回token
                Long regdate = System.currentTimeMillis();
                String Token = regdate + name;
                jsonToMap.put("uid", uid);
                //生成唯一性token用于验证
                jsonToMap.put("name", name);
                jsonToMap.put("token", name + DigestUtils.md5DigestAsHex(Token.getBytes()));
                jsonToMap.put("time", regdate);
                jsonToMap.put("group", "contributor");
                jsonToMap.put("groupKey", "contributor");
                jsonToMap.put("mail", "");
                jsonToMap.put("url", "");
                jsonToMap.put("screenName", userapi.getNickName());
                jsonToMap.put("avatar", apiconfig.getWebinfoAvatar() + "null");
                jsonToMap.put("lv", 0);
                jsonToMap.put("customize", "");
                jsonToMap.put("experience", 0);
                //VIP
                jsonToMap.put("vip", 0);
                jsonToMap.put("isvip", 0);

                //删除之前的token后，存入redis(防止积累导致内存溢出，超时时间默认是24小时)
                String oldToken = redisHelp.getRedis(this.dataprefix + "_" + "userkey" + name, redisTemplate);
                if (oldToken != null) {
                    redisHelp.delete(this.dataprefix + "_" + "userInfo" + oldToken, redisTemplate);
                }
                redisHelp.setRedis(this.dataprefix + "_" + "userkey" + jsonToMap.get("name").toString(), jsonToMap.get("token").toString(), this.usertime, redisTemplate);
                redisHelp.setKey(this.dataprefix + "_" + "userInfo" + jsonToMap.get("name").toString() + DigestUtils.md5DigestAsHex(Token.getBytes()), jsonToMap, this.usertime, redisTemplate);

                return Result.getResultJson(rows > 0 ? 1 : 0, rows > 0 ? "登录成功" : "登陆失败", jsonToMap);

            }
        } catch (Exception e) {
            e.printStackTrace();
            JSONObject response = new JSONObject();

            response.put("code", 0);
            response.put("msg", "登陆失败，请联系管理员");
            response.put("data", null);

            return response.toString();
        }

    }

    /***
     * 社会化绑定
     */
    @RequestMapping(value = "/bind")
    @ResponseBody
    public String bind(@RequestParam(value = "type") String type,
                       @RequestParam(value = "js_code") String js_code,
                       @RequestParam(value = "avatar") String avatar,
                       @RequestParam(value = "access_token") String access_token,
                       HttpServletRequest request) {
        try {
            Integer uid = null;
            String token = request.getHeader("Authorization");
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                uid = Integer.parseInt(verify.getClaim("aud").asString());
            }
            Apiconfig apiconfig = UStatus.getConfig(this.dataprefix, apiconfigService, redisTemplate);
            String qqUrl = String.format("https://api.q.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code", apiconfig.getQqAppletsAppid(), apiconfig.getQqAppletsSecret(), js_code);
            String wxUrl = String.format("https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code", apiconfig.getWxAppId(), apiconfig.getWxAppSecret(), js_code);

            String res = HttpClient.doGet(type.equals("qq") ? qqUrl : wxUrl);
            Map<String, String> data = JSONObject.parseObject(res, Map.class);
            System.out.println(data);
            if (data == null && data.isEmpty()) {
                return Result.getResultJson(202, "配置错误", null);
            }
            if (data.get("errcode") != "0") {
                return Result.getResultJson(201, data.get("errmsg"), null);
            }

            Userapi bind = new Userapi();
            bind.setAppLoginType(type.equals("qq") ? "qq" : "wx");
            bind.setUid(uid);
            List<Userapi> apiList = userapiService.selectList(bind);
            if (apiList.size() > 0) {
                Userapi userBind = apiList.get(0);
                userBind.setOpenId(data.get("openid"));
                userBind.setAccessToken(access_token);
                Integer updateStatus = userapiService.update(userBind);
                return Result.getResultJson(200, updateStatus > 0 ? "绑定成功" : "绑定失败", null);
            }

            bind.setAccessToken(access_token);
            bind.setOpenId(data.get("openid"));
            bind.setAppLoginType(type.equals("qq") ? "qq" : "wx");
            bind.setHeadImgUrl(avatar);
            Integer insert = userapiService.insert(bind);
            return Result.getResultJson(200, insert > 0 ? "绑定成功" : "绑定失败", null);


        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }

    }


    /**
     * 用户绑定查询
     */
    @RequestMapping(value = "/userBindStatus")
    @ResponseBody
    public String userBindStatus(@RequestParam(value = "token", required = false) String token) {

        JSONObject response = new JSONObject();
        try {
            Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
            if (uStatus == 0) {
                return Result.getResultJson(0, "用户未登录或Token验证失败", null);
            }
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            Integer uid = Integer.parseInt(map.get("uid").toString());
            Userapi userapi = new Userapi();
            userapi.setUid(uid);
            userapi.setAppLoginType("qq");
            Integer qqBind = userapiService.total(userapi);
            userapi.setAppLoginType("weixin");
            Integer weixinBind = userapiService.total(userapi);
            userapi.setAppLoginType("sinaweibo");
            Integer weiboBind = userapiService.total(userapi);
            Map jsonToMap = new HashMap();

            jsonToMap.put("qqBind", qqBind);
            jsonToMap.put("weixinBind", weixinBind);
            jsonToMap.put("weiboBind", weiboBind);

            response.put("code", 1);
            response.put("data", jsonToMap);
            response.put("msg", "");
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 0);
            response.put("data", "");
            response.put("msg", "数据异常");
            return response.toString();
        }

    }


    /***
     * register 注册用户
     */
    @RequestMapping(value = "/register")
    @ResponseBody
    public String register(@RequestParam(value = "account") String account,
                           @RequestParam(value = "password") String password,
                           @RequestParam(value = "mail") String mail,
                           @RequestParam(value = "code", required = false) String code,
                           @RequestParam(value = "inviteCode", required = false) String inviteCode) {
        try {
            Apiconfig apiconfig = UStatus.getConfig(dataprefix, apiconfigService, redisTemplate);
            Users user = new Users();
            user.setName(account);
            if (service.total(user, null) > 0) return Result.getResultJson(201, "用户名已存在", null);
            user.setName(null);
            user.setMail(mail);
            if (service.total(user, null) > 0) return Result.getResultJson(201, "邮箱已存在", null);
            user.setName(account);
            user.setPassword(phpass.HashPassword(password));
            if (apiconfig.getIsEmail().equals(1)) {
                String sendCode = redisHelp.getRedis(dataprefix + "_code" + mail, redisTemplate);
                if (sendCode != null && !sendCode.isEmpty()) {
                    if (!sendCode.equals(code)) {
                        return Result.getResultJson(201, "验证码错误", null);
                    }
                } else {
                    return Result.getResultJson(201, "验证码失效", null);
                }
            }
            // 如果开启邀请码 注册 查询传入的邀请码是否存在
            Invitation invite = new Invitation();
            if (apiconfig.getIsInvite().equals(1)) {
                if (inviteCode == null || inviteCode.isEmpty())
                    return Result.getResultJson(201, "邀请码不可为空", null);
                invite.setCode(inviteCode);
                List<Invitation> inviteList = invitationService.selectList(invite);
                invite = inviteList.get(0);
                if (inviteList.size() < 1 || invite.toString().isEmpty())
                    return Result.getResultJson(201, "邀请码不存在", null);
                if (invite.getStatus().equals(1)) return Result.getResultJson(201, "邀请码已被使用", null);
                invite.setStatus(1);
            }
            user.setGroup("contributor");
            user.setCreated((int) (System.currentTimeMillis() / 1000));
            service.insert(user);
            // 设置使用邀请码的用户
            invite.setUid(user.getUid());
            invitationService.update(invite);

            return Result.getResultJson(200, "注册成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /**
     * 登陆后操作的邮箱验证
     */
    @RequestMapping(value = "/sendCode")
    @ResponseBody
    public String sendCode(HttpServletRequest request) throws MessagingException {
        try {
            // 这个必须登录有token才能发送验证码
            String agent = request.getHeader("User-Agent");
            String ip = baseFull.getIpAddr(request);
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = service.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
            }
            Apiconfig apiconfig = UStatus.getConfig(this.dataprefix, apiconfigService, redisTemplate);
            if (apiconfig.getIsEmail().equals(0)) {
                return Result.getResultJson(201, "邮箱验证已关闭", null);
            }
            //刷邮件攻击拦截
            if (apiconfig.getBanRobots().equals(1)) {
                String isSilence = redisTemplate.opsForValue().get(ip + "_silence").toString();
                if (isSilence != null) {
                    return Result.getResultJson(0, "你已被暂时禁止请求，请耐心等待", null);
                }

                String isRepeated = redisTemplate.opsForValue().get(ip + "_isOperation").toString();
                if (isRepeated == null) {
                    redisTemplate.opsForValue().set(ip + "_isOperation", "1", 2, TimeUnit.MINUTES);
                } else {
                    int frequency = Integer.parseInt(isRepeated) + 1;
                    if (frequency == 3) {
                        securityService.safetyMessage("IP：" + ip + "，在邮箱发信疑似存在攻击行为，请及时确认处理。", "system");
                        redisTemplate.opsForValue().set(ip + "_silence", "1", 1800, TimeUnit.SECONDS);
                        return Result.getResultJson(0, "你的请求存在恶意行为，30分钟内禁止操作！", null);
                    }
                    redisTemplate.opsForValue().set(ip + "_isOperation", String.valueOf(frequency), 3, TimeUnit.MINUTES);
                    return Result.getResultJson(0, "你的操作太频繁了", null);
                }
            }
            //邮件每天最多发送10次
            String key = this.dataprefix + "_" + ip + "_code";
            Long sendCode = redisTemplate.opsForValue().increment(key, 1);
            if (sendCode == 1) {
                redisTemplate.expire(key, 86400, TimeUnit.SECONDS); // 设置过期时间为1天，以秒为单位
            } else if (sendCode > 10) {
                return Result.getResultJson(0, "你已超过最大邮件限制，请您24小时后再操作", null);
            }

            //限制结束

            //邮件59秒只能发送一次
            String iSsendCode = redisHelp.getRedis(this.dataprefix + "_" + "iSsendCode_" + agent + "_" + ip, redisTemplate);
            if (iSsendCode == null) {
                redisHelp.setRedis(this.dataprefix + "_" + "iSsendCode_" + agent + "_" + ip, "data", 59, redisTemplate);
            } else {
                return Result.getResultJson(201, "请等待1分钟后重新发送", null);
            }

            //删除之前的验证码 再发送新的验证码
            // 生成随机数种子
            Random random = new Random();
            // 生成6位验证码
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                sb.append(random.nextInt(10)); // 生成0-9之间的随机数
            }
            String verificationCode = sb.toString();

            redisHelp.delete(dataprefix + "_code" + user.getMail(), redisTemplate);
            redisHelp.setRedis(dataprefix + "_code" + user.getMail(), verificationCode, 600, redisTemplate);

            try {
                MailService.send("你本次的验证码为" + verificationCode, "<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" /><title></title><meta charset=\"utf-8\" /><style>*{padding:0px;margin:0px;box-sizing:border-box;}html{box-sizing:border-box;}body{font-size:15px;background:#fff}.main{margin:20px auto;max-width:500px;border:solid 1px #2299dd;overflow:hidden;}.main h1{display:block;width:100%;background:#2299dd;font-size:18px;color:#fff;text-align:center;padding:15px;}.text{padding:30px;}.text p{margin:10px 0px;line-height:25px;}.text p span{color:#2299dd;font-weight:bold;font-size:22px;margin-left:5px;}</style></head><body><div class=\"main\"><h1>用户验证码</h1><div class=\"text\"><p>你本次的验证码为<span>" + verificationCode + "</span>。</p><p>出于安全原因，该验证码将于10分钟后失效。请勿将验证码透露给他人。</p></div></div></body></html>",
                        new String[]{user.getMail()}, new String[]{});
            } catch (Exception e) {
                e.printStackTrace();
                return Result.getResultJson(201, "邮件发送错误", null);
            }
            return Result.getResultJson(200, "验证码已发送，有效时长10分钟", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "不正确的邮箱发信配置", null);
        }


    }

    /**
     * 注册邮箱验证
     */
    @RequestMapping(value = "/regCodeSend")
    @ResponseBody
    public String regCodeSend(@RequestParam(value = "mail") String mail,
                              HttpServletRequest request) throws MessagingException {
        try {
            Apiconfig apiconfig = UStatus.getConfig(this.dataprefix, apiconfigService, redisTemplate);
            if (apiconfig.getIsEmail().equals(0)) {
                return Result.getResultJson(201, "已关闭邮箱验证", null);
            }
            String agent = request.getHeader("User-Agent");
            String ip = baseFull.getIpAddr(request);
            //刷邮件攻击拦截
            String isSilence = redisHelp.getRedis(ip + "_silence", redisTemplate);
            if (isSilence != null) {
                return Result.getResultJson(201, "你已被暂时禁止请求，请耐心等待", null);
            }
            String isRepeated = redisHelp.getRedis(ip + "_isOperation", redisTemplate);
            if (isRepeated == null) {
                redisHelp.setRedis(ip + "_isOperation", "1", 2, redisTemplate);
            } else {
                Integer frequency = Integer.parseInt(isRepeated) + 1;
                if (frequency == 3) {
                    securityService.safetyMessage("IP：" + ip + "，在邮箱发信疑似存在攻击行为，请及时确认处理。", "system");
                    redisHelp.setRedis(ip + "_silence", "1", 1800, redisTemplate);
                    return Result.getResultJson(201, "你的请求存在恶意行为，30分钟内禁止操作！", null);
                }
                redisHelp.setRedis(ip + "_isOperation", frequency.toString(), 3, redisTemplate);
                return Result.getResultJson(201, "你的操作太频繁了", null);
            }
            //攻击拦截结束
            String regISsendCode = redisHelp.getRedis(this.dataprefix + "_" + "regISsendCode_" + agent + "_" + ip, redisTemplate);
            if (regISsendCode == null) {
                redisHelp.setRedis(this.dataprefix + "_" + "regISsendCode_" + agent + "_" + ip, "data", 59, redisTemplate);
            } else {
                return Result.getResultJson(201, "你的操作太频繁了", null);
            }

            // 上面那一堆不是我写的
            if (mail == null && mail.isEmpty()) return Result.getResultJson(201, "请输入邮箱", null);
            if (!baseFull.isEmail(mail)) return Result.getResultJson(201, "请输入正确邮箱", null);

            // 邮箱被注册了没
            Users user = new Users();
            user.setMail(mail);
            if (service.total(user, null) > 0) return Result.getResultJson(201, "邮箱已被注册", null);

            //删除之前的验证码 再发送新的验证码
            // 生成随机数种子
            Random random = new Random();
            // 生成6位验证码
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                sb.append(random.nextInt(10)); // 生成0-9之间的随机数
            }
            String verificationCode = sb.toString();
            redisHelp.delete(dataprefix + "_code" + mail, redisTemplate);
            redisHelp.setRedis(dataprefix + "_code" + mail, verificationCode, 600, redisTemplate);

            // 发送验证码
            try {
                MailService.send("你本次的验证码为" + verificationCode, "<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" /><title></title><meta charset=\"utf-8\" /><style>*{padding:0px;margin:0px;box-sizing:border-box;}html{box-sizing:border-box;}body{font-size:15px;background:#fff}.main{margin:20px auto;max-width:500px;border:solid 1px #2299dd;overflow:hidden;}.main h1{display:block;width:100%;background:#2299dd;font-size:18px;color:#fff;text-align:center;padding:15px;}.text{padding:30px;}.text p{margin:10px 0px;line-height:25px;}.text p span{color:#2299dd;font-weight:bold;font-size:22px;margin-left:5px;}</style></head><body><div class=\"main\"><h1>用户验证码</h1><div class=\"text\"><p>你本次的验证码为<span>" + verificationCode + "</span>。</p><p>出于安全原因，该验证码将于10分钟后失效。请勿将验证码透露给他人。</p></div></div></body></html>",
                        new String[]{mail}, new String[]{});
                return Result.getResultJson(200, "验证码已发送，有效时长10分钟", null);
            } catch (Exception e) {
                e.printStackTrace();
                return Result.getResultJson(201, "邮件发送错误", null);
            }
        } catch (Exception e) {
            return Result.getResultJson(400, "不正确的邮箱发信配置", null);
        }

    }

    /***
     * 找回密码
     * @param account Bean对象JSON字符串
     */
    @RequestMapping(value = "/resetPassword")
    @ResponseBody
    public String resetPassword(@RequestParam(value = "account") String account,
                                @RequestParam(value = "password") String password,
                                @RequestParam(value = "code", required = false) String code) {
        try {
            if (account == null || account.isEmpty() || account.equals("")) {
                return Result.getResultJson(201, "账号不可为空", null);
            }
            Apiconfig apiconfig = UStatus.getConfig(dataprefix, apiconfigService, redisTemplate);
            if (apiconfig.getIsEmail().equals(0)) {
                return Result.getResultJson(201, "已关闭邮箱验证，请联系管理员找回密码", null);
            }
            Users user = new Users();
            if (!baseFull.isEmail(account)) {
                user.setName(account);
                List<Users> userList = service.selectList(user);
                if (userList.size() < 1) return Result.getResultJson(201, "用户不存在", null);
                user = userList.get(0);
            } else {
                user.setMail(account);
                List<Users> userList = service.selectList(user);
                if (userList.size() < 1) return Result.getResultJson(201, "用户不存在", null);
                user = userList.get(0);
            }
            // code为空发送验证码
            if (code == null || code.isEmpty()) {
                //删除之前的验证码 再发送新的验证码
                // 生成随机数种子
                Random random = new Random();
                // 生成6位验证码
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 6; i++) {
                    sb.append(random.nextInt(10)); // 生成0-9之间的随机数
                }
                String verificationCode = sb.toString();
                redisHelp.delete(dataprefix + "_code" + user.getMail(), redisTemplate);
                redisHelp.setRedis(dataprefix + "_code" + user.getMail(), verificationCode, 600, redisTemplate);

                // 发送验证码
                try {
                    MailService.send("你本次的验证码为" + verificationCode, "<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" /><title></title><meta charset=\"utf-8\" /><style>*{padding:0px;margin:0px;box-sizing:border-box;}html{box-sizing:border-box;}body{font-size:15px;background:#fff}.main{margin:20px auto;max-width:500px;border:solid 1px #2299dd;overflow:hidden;}.main h1{display:block;width:100%;background:#2299dd;font-size:18px;color:#fff;text-align:center;padding:15px;}.text{padding:30px;}.text p{margin:10px 0px;line-height:25px;}.text p span{color:#2299dd;font-weight:bold;font-size:22px;margin-left:5px;}</style></head><body><div class=\"main\"><h1>用户验证码</h1><div class=\"text\"><p>你本次的验证码为<span>" + verificationCode + "</span>。</p><p>出于安全原因，该验证码将于10分钟后失效。请勿将验证码透露给他人。</p></div></div></body></html>",
                            new String[]{user.getMail()}, new String[]{});
                } catch (Exception e) {
                    e.printStackTrace();
                    return Result.getResultJson(201, "邮件发送错误", null);
                }
                return Result.getResultJson(200, "验证码已发送，有效时长10分钟", null);
            } else {
                String sendCode = redisHelp.getRedis(dataprefix + "_code" + user.getMail(), redisTemplate);
                if (sendCode != null && !sendCode.isEmpty()) {
                    if (!sendCode.equals(code)) {
                        return Result.getResultJson(201, "验证码错误", null);
                    }
                } else {
                    return Result.getResultJson(201, "验证码失效", null);
                }
                if (password == null && password.isEmpty()) return Result.getResultJson(201, "密码不可为空", null);
                user.setPassword(phpass.HashPassword(password));
                service.update(user);
                return Result.getResultJson(200, "重置成功", null);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口错误", null);
        }
    }

    /***
     * 客户端id push推送
     */
    @RequestMapping(value = "/setClient")
    @ResponseBody
    public String setClient(@RequestParam(value = "id") String id,
                            HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = service.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
            }
            user.setClientId(id);
            service.update(user);
            return Result.getResultJson(200, "设置成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     * 用户修改
     */
    @RequestMapping(value = "/update")
    @ResponseBody
    public String update(@RequestParam(value = "nickname", required = false) String nickname,
                         @RequestParam(value = "sex", required = false) String sex,
                         @RequestParam(value = "introduce", required = false) String introduce,
                         @RequestParam(value = "avatar", required = false) String avatar,
                         @RequestParam(value = "background", required = false) String background,
                         @RequestParam(value = "mail", required = false) String mail,
                         @RequestParam(value = "code", required = false) String code,
                         @RequestParam(value = "password", required = false) String password,
                         HttpServletRequest request) {
        try {
            Apiconfig apiconfig = UStatus.getConfig(dataprefix, apiconfigService, redisTemplate);
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = service.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
            }
            if (nickname != null && !nickname.isEmpty()) user.setScreenName(nickname);
            if (avatar != null && !avatar.isEmpty()) user.setAvatar(avatar);
            if (background != null && !background.isEmpty()) user.setUserBg(background);
            if (sex != null && !sex.isEmpty()) user.setSex(sex);
            if (introduce != null && !introduce.isEmpty()) user.setIntroduce(introduce);
            if (password != null && !password.isEmpty()) {
                // 加密密码
                user.setPassword(phpass.HashPassword(password));
            }
            if (mail != null && !mail.isEmpty()) {
                if (!baseFull.isEmail(mail)) return Result.getResultJson(201, "邮箱格式错误", null);
                Users query = new Users();
                query.setMail(mail);
                if (service.total(query, null) > 0) {
                    return Result.getResultJson(201, "邮箱已被其他用户绑定", null);
                }
                // 是否开启邮箱
                if (apiconfig.getIsEmail().equals(1)) {
                    String sendCode = redisHelp.getRedis(dataprefix + "_code" + user.getMail(), redisTemplate);
                    if (sendCode != null && !sendCode.isEmpty()) {
                        if (!sendCode.equals(code)) {
                            return Result.getResultJson(201, "验证码错误", null);
                        }
                    } else {
                        return Result.getResultJson(201, "验证码失效", null);
                    }
                }
                user.setMail(mail);
            }
            service.update(user);
            return Result.getResultJson(200, "修改成功", null);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    private boolean permission(String token) {
        if (token != null && !token.isEmpty()) {
            DecodedJWT verify = JWT.verify(token);
            Users user = service.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
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
     * 管理员修改用户
     */

    @RequestMapping(value = "/edit")
    @ResponseBody
    public String edit(
            @RequestParam(value = "id") Integer id,
            @RequestParam(value = "nickname", required = false) String nickname,
            @RequestParam(value = "sex", required = false) String sex,
            @RequestParam(value = "introduce", required = false) String introduce,
            @RequestParam(value = "mail", required = false) String mail,
            @RequestParam(value = "group", required = false) String group,
            @RequestParam(value = "opt", required = false) String opt,
            HttpServletRequest request) {
        try {
            Boolean permission = permission(request.getHeader("Authorization"));
            if (!permission) return Result.getResultJson(201, "无权限", null);
            Users user = service.selectByKey(id);
            user.setOpt(opt);
            user.setGroup(group);
            user.setScreenName(nickname);
            user.setSex(sex);
            user.setIntroduce(introduce);
            user.setMail(mail);
            service.update(user);
            return Result.getResultJson(200, "修改成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     * 用户删除
     */
    @RequestMapping(value = "/delete")
    @ResponseBody
    public String delete(@RequestParam(value = "id") Integer id,
                         HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = service.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
            }
            if (!permission(request.getHeader("Authorization"))) {
                return Result.getResultJson(201, "无权限", null);
            }
            Users deleteUser = service.selectByKey(id);
            if (user.getUid().equals(deleteUser.getUid())) return Result.getResultJson(201, "无法删除自己", null);
            if (deleteUser == null || user.toString().isEmpty()) return Result.getResultJson(201, "用户不存在", null);

            service.delete(id);
            return Result.getResultJson(200, "删除成功", null);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口错误", null);
        }

    }

    /***
     * 发起提现
     */
    @RequestMapping(value = "/withdraw")
    @ResponseBody
    public String withdraw(@RequestParam(value = "num") Integer num, HttpServletRequest request) {
        try {
            if (num == null || num.equals("")) return Result.getResultJson(201, "请输入提现额度", null);
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = service.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
            }
            if (user.getPay() == null || user.getPay().isEmpty())
                return Result.getResultJson(201, "请先设置收款方式", null);
            Userlog log = new Userlog();
            log.setType("withdraw");
            log.setUid(user.getUid());
            log.setCid(-1);
            List<Userlog> logList = userlogService.selectList(log);
            if (logList.size() > 0) return Result.getResultJson(201, "请等待上一提现请求完成", null);
            if (user.getAssets() < num) return Result.getResultJson(201, "余额不足", null);
            user.setAssets(user.getAssets() - num);
            log.setNum(num);

            // 获取当前系统时间戳
            Long timestamp = System.currentTimeMillis();

            // 格式化时间
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            String formattedTime = dateFormat.format(new Date(timestamp));

            // 写入支付记录
            Paylog pay = new Paylog();
            pay.setUid(user.getUid());
            pay.setPaytype("withdraw");
            pay.setSubject("提现余额");
            pay.setTotalAmount(String.valueOf(num * -1));
            pay.setOutTradeNo(formattedTime + (timestamp / 1000) + user.getUid());
            pay.setStatus(0);
            pay.setCreated((int) (timestamp / 1000));
            paylogService.insert(pay);
            // 更新用户信息
            service.update(user);
            // 写入userlog
            userlogService.insert(log);
            return Result.getResultJson(200, "提现请求已提交", null);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }

    }

    /***
     * 提现列表
     */
    @RequestMapping(value = "/withdrawList")
    @ResponseBody
    public String withdrawList(@RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                               @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
                               @RequestParam(value = "id", required = false) Integer id,
                               HttpServletRequest request) {
        try {
            Boolean permission = permission(request.getHeader("Authorization"));
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = service.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
            }
            Paylog pay = new Paylog();
            pay.setPaytype("withdraw");
            pay.setUid(user.getUid());
            // 如果有权限可以查询全部 以及其他人的
            if (permission) {
                pay.setUid(id != null && !id.equals("") ? id : null);
            }
            PageList<Paylog> payPage = paylogService.selectPage(pay, page, limit);
            List<Paylog> payList = payPage.getList();
            JSONArray dataList = new JSONArray();
            if (permission) {
                for (Paylog _pay : payList) {
                    Map<String, Object> data = new HashMap<>();
                    Users drawUser = service.selectByKey(_pay.getUid());
                    Map<String, Object> drawData = new HashMap<>();
                    // 删除数据
                    drawData.remove("address");
                    drawData.remove("opt");
                    data.put("userInfo", drawData);
                    dataList.add(data);
                }
            }
            Map<String, Object> data = new HashMap<>();
            data.put("data", permission ? dataList : payList);
            data.put("page", page);
            data.put("limit", limit);
            data.put("count", permission ? dataList.size() : payList.size());
            return Result.getResultJson(200, "获取成功", data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     * 提现审核
     */
    @RequestMapping(value = "/withdrawAduit")
    @ResponseBody
    public String withdrawAduit(@RequestParam(value = "type") String type,
                                @RequestParam(value = "id") Integer id,
                                @RequestParam(value = "text", required = false) String text,
                                HttpServletRequest request) {
        try {
            Apiconfig apiconfig = UStatus.getConfig(dataprefix, apiconfigService, redisTemplate);
            if (!permission(request.getHeader("Authorization"))) return Result.getResultJson(201, "无权限", null);
            Paylog pay = paylogService.selectByKey(id);
            if (pay == null || pay.toString().isEmpty()) return Result.getResultJson(201, "数据不存在", null);
            Users user = service.selectByKey(pay.getUid());

            // 给用户发消息 站内邮件 以及设置payStatus
            Inbox inbox = new Inbox();
            inbox.setCreated((int) (System.currentTimeMillis() / 1000));
            inbox.setTouid(pay.getUid());
            inbox.setValue(pay.getPid());
            inbox.setType("finance");
            if (type.equals("accept")) {
                inbox.setText("您的提现请求已通过审核");
                pay.setStatus(1);
            } else {
                inbox.setText("您的提现审核不通过，余额已返还");
                pay.setStatus(3);
                // 将余额返回给用户
                user.setAssets(user.getAssets() + (Integer.parseInt(pay.getTotalAmount()) * -1));
                service.update(user);
            }
            // push消息
            if (apiconfig.getIsPush().equals(1)) {
                try {
                    pushService.sendPushMsg(user.getClientId(), "提现通知", pay.getStatus().equals(1) ? "您的提现请求已通过审核" : "您的提现请求不通过,余额已返还", "payload", pay.getPid().toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            paylogService.update(pay);
            inboxService.insert(inbox);

            return Result.getResultJson(200, "操作成功", null);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }

    }

    /***
     * 管理员手动充扣
     */
    @RequestMapping(value = "/charge")
    @ResponseBody
    public String charge(@RequestParam(value = "num") Integer num,
                         @RequestParam(value = "type") Integer type,
                         @RequestParam(value = "id") Integer id,
                         HttpServletRequest request) {
        try {
            if (!permission(request.getHeader("Authorization"))) return Result.getResultJson(201, "无权限", null);
            if (num == 0 || num.toString().isEmpty() || num.equals(""))
                return Result.getResultJson(201, "余额不可为空", null);

            Users user = service.selectByKey(id);

            // 获取当前系统时间戳
            Long timestamp = System.currentTimeMillis();
            // 格式化时间
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            String formattedTime = dateFormat.format(new Date(timestamp));
            // 写入支付记录
            Paylog pay = new Paylog();
            pay.setStatus(1);
            pay.setUid(id);
            pay.setPaytype("charge");
            pay.setTotalAmount(String.valueOf(type.equals(0) ? num * -1 : num));
            pay.setCreated((int) (System.currentTimeMillis() / 1000));
            pay.setOutTradeNo(formattedTime + (timestamp / 1000) + user.getUid());
            if (type.equals(0)) {
                pay.setSubject("系统扣款");
                user.setAssets(user.getAssets() - num);
            }
            if (type.equals(1)) {
                pay.setSubject("系统充值");
                user.setAssets(user.getAssets() + num);
            }
            service.update(user);
            paylogService.insert(pay);
            return Result.getResultJson(200, type.equals(0) ? "扣款成功" : "充值成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }


    /***
     * 注册配置
     */
    @RequestMapping(value = "/regConfig")
    @ResponseBody
    public String regConfig() {
        try {
            Apiconfig apiconfig = UStatus.getConfig(this.dataprefix, apiconfigService, redisTemplate);
            Map<String, Object> data = new HashMap<>();
            data.put("isEmail", apiconfig.getIsEmail());
            data.put("isInvite", apiconfig.getIsInvite());
            return Result.getResultJson(200, "获取成功", data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /**
     * 创建邀请码
     **/
    @RequestMapping(value = "/madeCode")
    @ResponseBody
    public String madeCode(@RequestParam(value = "num") Integer num,
                           HttpServletRequest request) {
        try {
            if (!permission(request.getHeader("Authorization"))) return Result.getResultJson(201, "无权限", null);
            if (num == null || num.equals("") || num.equals(0))
                return Result.getResultJson(201, "请输入正确的数量", null);
            Invitation invite = new Invitation();
            String token = request.getHeader("Authorization");
            Integer uid = null;
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                uid = Integer.parseInt(verify.getClaim("aud").asString());
            }
            Long timeStamp = System.currentTimeMillis() / 1000;
            invite.setCreated(Math.toIntExact(timeStamp));
            invite.setUid(uid);
            for (int i = 0; i < num; i++) {
                invite.setCode(baseFull.createRandomStr(8));
                invite.setStatus(0);
                invitationService.insert(invite);
            }
            return Result.getResultJson(200, "已生成" + num + "条邀请码", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     * 邀请码列表
     *
     */
    @RequestMapping(value = "/codeList")
    @ResponseBody
    public String codeList(@RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                           @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
                           @RequestParam(value = "type", defaultValue = "0") Integer type,
                           HttpServletRequest request) {
        try {
            if (!permission(request.getHeader("Authorization"))) return Result.getResultJson(201, "无权限", null);
            Invitation invite = new Invitation();
            invite.setStatus(type);
            PageList<Invitation> invitePage = invitationService.selectPage(invite, page, limit);
            List<Invitation> inviteList = invitePage.getList();
            Map<String, Object> data = new HashMap<>();
            data.put("page", page);
            data.put("limit", limit);
            data.put("data", inviteList);
            data.put("count", inviteList.size());
            data.put("total", invitationService.total(invite));
            return Result.getResultJson(200, "获取成功", data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     * 导出邀请码
     *
     */
    @RequestMapping(value = "/codeExcel")
    @ResponseBody
    public void codeExcel(@RequestParam(value = "limit") Integer limit,
                          @RequestParam(value = "type", defaultValue = "0") Integer type,
                          HttpServletResponse response,
                          HttpServletRequest request) throws IOException {
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet("邀请码列表");
        if (!permission(request.getHeader("Authorization"))) {
            response.setContentType("application/octet-stream");
            response.setHeader("Content-disposition", "attachment;filename=nodata.xls");
            response.flushBuffer();
            workbook.write(response.getOutputStream());
        }
        Invitation query = new Invitation();
        query.setStatus(type);
        PageList<Invitation> pageList = invitationService.selectPage(query, 1, limit);
        List<Invitation> list = pageList.getList();

        String fileName = "InvitationExcel" + ".xls";//设置要导出的文件的名字
        //新增数据行，并且设置单元格数据

        int rowNum = 1;

        String[] headers = {"ID", "邀请码", "创建人"};
        //headers表示excel表中第一行的表头

        HSSFRow row = sheet.createRow(0);
        //在excel表中添加表头

        for (int i = 0; i < headers.length; i++) {
            HSSFCell cell = row.createCell(i);
            HSSFRichTextString text = new HSSFRichTextString(headers[i]);
            cell.setCellValue(text);
        }
        for (com.TypeApi.entity.Invitation Invitation : list) {
            HSSFRow row1 = sheet.createRow(rowNum);
            row1.createCell(0).setCellValue(Invitation.getId());
            row1.createCell(1).setCellValue(Invitation.getCode());
            row1.createCell(2).setCellValue(Invitation.getUid());
            rowNum++;
        }

        response.setContentType("application/octet-stream");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName);
        response.flushBuffer();
        workbook.write(response.getOutputStream());
    }

    /***
     * 用户信息
     */
    @RequestMapping(value = "/inbox")
    @ResponseBody
    public String inbox(@RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                        @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
                        @RequestParam(value = "type", required = false) String type,
                        HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = service.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
            }
            Inbox query = new Inbox();
            query.setType(type);
            query.setTouid(user.getUid());
            PageList<Inbox> inboxPage = inboxService.selectPage(query, page, limit);
            List<Inbox> inboxList = inboxPage.getList();
            JSONArray dataList = new JSONArray();
            if (type.equals("comment")) {
                for (Inbox _inbox : inboxList) {
                    Map<String, Object> data = JSONObject.parseObject(JSONObject.toJSONString(_inbox), Map.class);
                    // 查询发送方信息
                    Users sender = service.selectByKey(_inbox.getUid());
                    Map<String, Object> dataSender = JSONObject.parseObject(JSONObject.toJSONString(sender));
                    if (sender != null && !sender.toString().isEmpty()) {
                        dataSender.remove("password");
                        dataSender.remove("address");
                        dataSender.remove("assets");
                        dataSender.remove("opt");
                        dataSender.remove("head_picture");
                        dataSender.remove("mail");
                    }

                    // 查询回复的评论
                    Comments reply = commentsService.selectByKey(_inbox.getValue());
                    Map<String, Object> dataReply = JSONObject.parseObject(JSONObject.toJSONString(reply));
                    Map<String, Object> articleData = new HashMap<>();
                    if (reply != null && !reply.toString().isEmpty()) {
                        JSONArray images = new JSONArray();
                        images = reply.getImages() != null && !reply.getImages().toString().isEmpty() ? JSONArray.parseArray(reply.getImages()) : null;
                        dataReply.put("images", images);
                        // 查询评论的用户
                        Users replyUser = service.selectByKey(reply.getUid());
                        Map<String, Object> dataReplyUser = JSONObject.parseObject(JSONObject.toJSONString(sender));
                        if (replyUser != null && !replyUser.toString().isEmpty()) {
                            dataReplyUser.remove("password");
                            dataReplyUser.remove("address");
                            dataReplyUser.remove("assets");
                            dataReplyUser.remove("opt");
                            dataReplyUser.remove("head_picture");
                            dataReplyUser.remove("mail");
                        }

                        Article article = contentsService.selectByKey(reply.getCid());
                        // 如果不存在的话

                        if (article != null && !article.toString().isEmpty()) {
                            articleData.put("title", article.getTitle());
                            articleData.put("authorId", article.getAuthorId());
                            articleData.put("id", article.getCid());
                        } else {
                            articleData.put("title", "文章已被删除");
                            articleData.put("id", 0);
                            articleData.put("authorId", 0);
                        }
                        dataReply.put("userInfo", dataReplyUser);
                    }
                    data.put("reply", dataReply);
                    data.put("userInfo", dataSender);
                    data.put("article", articleData);
                    dataList.add(data);
                }
            }
            Map<String, Object> data = new HashMap<>();
            data.put("page", page);
            data.put("limit", limit);
            data.put("data", type.equals("comment") ? dataList : inboxList);
            data.put("count", inboxList.size());
            data.put("total", inboxService.total(query));
            return Result.getResultJson(200, "获取成功", data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     * 获取未读消息数量
     *
     */
    @RequestMapping(value = "/noticeNum")
    @ResponseBody
    public String noticeNum(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = service.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
            }
            Inbox inbox = new Inbox();
            inbox.setUid(user.getUid());
            inbox.setIsread(0);
            inbox.setType("comment");
            Integer comments = inboxService.total(inbox);
            inbox.setType("system");
            Integer systems = inboxService.total(inbox);
            inbox.setType("finance");
            Integer finances = inboxService.total(inbox);

            Map<String, Object> data = new HashMap<>();
            data.put("comments", comments);
            data.put("system", systems);
            data.put("finances", finances);
            data.put("total", comments + systems + finances);
            return Result.getResultJson(200, "获取成功", data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     * 将所有消息已读
     *
     */
    @RequestMapping(value = "/clearNum")
    @ResponseBody
    public String clearNum(@RequestParam(value = "type") String type,
                           HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = service.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
            }
            String sql = "UPDATE " + prefix + "_inbox SET isread = 1 WHERE touid = ?";
            if (type != null) {
                sql = "UPDATE " + prefix + "_inbox SET isread = 1 WHERE touid = ? AND type = ?";
            }
            jdbcTemplate.update(sql, user.getUid(), type);

            return Result.getResultJson(200, "清除完成", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }

    }

    /***
     * 发送消息
     */
    @RequestMapping(value = "/sendMsg")
    @ResponseBody
    public String sendMsg(@RequestParam(value = "id") Integer id,
                          @RequestParam(value = "text") String text,
                          HttpServletRequest request) {
        try {
            Apiconfig apiconfig = UStatus.getConfig(dataprefix, apiconfigService, redisTemplate);
            if (!permission(request.getHeader("Authorization"))) return Result.getResultJson(201, "无权限", null);
            Users user = service.selectByKey(id);
            if (user == null || user.toString().isEmpty()) return Result.getResultJson(201, "用户不存在", null);
            if (text == null || text.isEmpty()) return Result.getResultJson(201, "内容不可为空", null);

            Inbox inbox = new Inbox();
            inbox.setIsread(0);
            inbox.setTouid(id);
            inbox.setType("system");
            inbox.setText(text);
            inbox.setUid(0);
            // 写入数据库
            inboxService.insert(inbox);
            if (apiconfig.getIsPush().equals(1) && user.getClientId() != null) {
                try {
                    pushService.sendPushMsg(user.getClientId(), "系统提醒", text, "payload", "system");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return Result.getResultJson(200, "发送成功", null);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     * 关注用户
     */
    @RequestMapping(value = "/follow")
    @ResponseBody
    public String follow(@RequestParam(value = "id") Integer id,
                         HttpServletRequest request) {
        try {
            Apiconfig apiconfig = UStatus.getConfig(dataprefix, apiconfigService, redisTemplate);
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = service.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
            }
            // 查询用户是否存在
            Users toFanUser = service.selectByKey(id);
            if (toFanUser == null || toFanUser.toString().isEmpty())
                return Result.getResultJson(201, "用户不存在", null);
            // 查询是否关注过该用户
            Fan fan = new Fan();
            fan.setUid(user.getUid());
            fan.setTouid(id);
            List<Fan> fanList = fanService.selectList(fan);
            fan.setCreated((int) (System.currentTimeMillis() / 1000));
            // 关注过该用户就删除信息 取消关注
            if (fanList.size() > 0) {
                fanService.delete(fanList.get(0).getId());
                return Result.getResultJson(200, "已取消关注", null);
            } else {
                fanService.insert(fan);
                return Result.getResultJson(200, "关注成功", null);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }


    /***
     * 关注列表
     * @param type 0 我关注的人 1 关注我的人
     */
    @RequestMapping(value = "/followList")
    @ResponseBody
    public String followList(@RequestParam(value = "id", required = false) Integer id,
                             @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                             @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
                             @RequestParam(value = "type") Integer type,
                             HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = service.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
            }
            // 如果传入id的话就查询其他人的关注列表 默认查询我关注的人
            // 查询被关注人的列表
            Fan fan = new Fan();
            if (type.equals(0)) {
                fan.setUid(user.getUid());
                if (id != null && !id.equals(0) && !id.equals("")) {
                    fan.setUid(id);
                }

            }
            // 查询关注我的人
            if (type.equals(1)) {
                fan.setUid(null);
                fan.setTouid(user.getUid());
                if (id != null && !id.equals(0) && !id.equals("")) {
                    fan.setTouid(id);
                }
            }
            PageList<Fan> fanPage = fanService.selectPage(fan, page, limit);
            List<Fan> fanList = fanPage.getList();
            JSONArray dataList = new JSONArray();
            for (Fan _fan : fanList) {
                Map<String, Object> data = JSONObject.parseObject(JSONObject.toJSONString(_fan), Map.class);
                // 查询用户信息 被关注人信息
                JSONObject opt = new JSONObject();
                if (type.equals(0)) {
                    Users fanUser = service.selectByKey(_fan.getTouid());
                    Map<String, Object> dataUser = JSONObject.parseObject(JSONObject.toJSONString(fanUser), Map.class);
                    // 格式化用户信息
                    opt = fanUser.getOpt() != null && !fanUser.getOpt().toString().isEmpty() ? JSONObject.parseObject(fanUser.getOpt()) : null;

                    dataUser.remove("password");
                    dataUser.remove("address");
                    dataUser.remove("mail");
                    // 替换信息
                    dataUser.put("opt", opt);
                    dataList.add(dataUser);
                }
                // 查询关注我的人
                if (type.equals(1)) {
                    Users fanUser = service.selectByKey(_fan.getTouid());
                    Map<String, Object> dataUser = JSONObject.parseObject(JSONObject.toJSONString(fanUser), Map.class);
                    // 格式化用户信息 先移除敏感信息
                    dataUser.remove("password");
                    dataUser.remove("address");
                    dataUser.remove("mail");
                    opt = fanUser.getOpt() != null && !fanUser.getOpt().toString().isEmpty() ? JSONObject.parseObject(fanUser.getOpt()) : null;

                    // 替换信息
                    dataUser.put("opt", opt);
                    dataList.add(dataUser);
                }
            }
            Map<String, Object> data = new HashMap<>();
            data.put("page", page);
            data.put("limit", limit);
            data.put("data", dataList);
            data.put("count", dataList.size());
            data.put("total", fanService.total(fan));
            return Result.getResultJson(200, "获取成功", data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }

    }


    /***
     * 封禁指定用户
     */
    @RequestMapping(value = "/ban")
    @ResponseBody
    public String ban(@RequestParam(value = "id") Integer id,
                      @RequestParam(value = "text") String text,
                      @RequestParam(value = "days") Integer days,
                      HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (!permission(token)) return Result.getResultJson(201, "无权限", null);
            Integer uid = null;
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                uid = Integer.parseInt(verify.getClaim("aud").asString());
            }
            Long timeStamp = System.currentTimeMillis() / 1000;
            Long banTime = timeStamp + (days * 86400);
            if (!permission(request.getHeader("Authorization"))) return Result.getResultJson(201, "无权限", null);
            //查询用户是否存在
            Users user = service.selectByKey(id);
            if (user == null || user.toString().isEmpty()) return Result.getResultJson(201, "用户不存在", null);
            if (user.getBantime() > timeStamp) return Result.getResultJson(201, "用户封禁中", null);
            if (days == null || days.equals(0) || days.equals(""))
                return Result.getResultJson(201, "请输入封禁天数", null);

            // 写入封禁记录
            Violation violation = new Violation();
            violation.setCreated(Math.toIntExact(timeStamp));
            violation.setUid(user.getUid());
            violation.setType("ban");
            violation.setText(text);
            violation.setHandler(uid);
            violationService.insert(violation);
            // 更新用户信息
            user.setBantime(Math.toIntExact(banTime));
            service.update(user);

            return Result.getResultJson(200, "封禁成功", null);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     * 解封用户
     */
    @RequestMapping("/unban")
    @ResponseBody
    public String unban(@RequestParam(value = "id") Integer id,
                        HttpServletRequest request) {
        try {
            if (!permission(request.getHeader("Authorization"))) return Result.getResultJson(201, "无权限", null);
            String token = request.getHeader("Authorization");
            Integer uid = null;
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                uid = Integer.parseInt(verify.getClaim("aud").asString());
            }
            Long timeStamp = System.currentTimeMillis() / 1000;
            Users user = service.selectByKey(id);
            if (user == null || user.toString().isEmpty()) return Result.getResultJson(201, "用户不存在", null);
            if (user.getBantime() < timeStamp) return Result.getResultJson(201, "该用户状态正常", null);

            // 更改用户的封禁时间
            user.setBantime(Math.toIntExact(timeStamp));
            service.update(user);
            return Result.getResultJson(200, "解除成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     * 封禁列表
     */
    @RequestMapping(value = "/banList")
    @ResponseBody
    public String banList(@RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                          @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
                          @RequestParam(value = "params", required = false) Integer params,
                          @RequestParam(value = "order", required = false, defaultValue = "created desc") String order,
                          HttpServletRequest request) {
        try {
            if (!permission(request.getHeader("Authorization"))) return Result.getResultJson(201, "无权限", null);
            Violation violation = new Violation();
            if (params != null && !params.toString().isEmpty()) {
                violation = JSONObject.parseObject(JSONObject.toJSONString(params), Violation.class);
            }
            PageList<Violation> violationPageList = violationService.selectPage(violation, page, limit);
            List<Violation> violationList = violationPageList.getList();
            JSONArray dataList = new JSONArray();
            for (Violation _violation : violationList) {
                Map<String, Object> data = JSONObject.parseObject(JSONObject.toJSONString(_violation), Map.class);
                // 获取用户信息
                Users vioUser = service.selectByKey(_violation.getUid());
                Map<String, Object> dataUser = JSONObject.parseObject(JSONObject.toJSONString(vioUser), Map.class);
                // 删除信息
                dataUser.remove("address");
                dataUser.remove("opt");
                dataUser.remove("passowrd");
                dataUser.remove("head_picture");

                // data加入信息
                data.put("userInfo", dataUser);
                dataList.add(data);
            }
            Map<String, Object> data = new HashMap<>();
            data.put("page", page);
            data.put("limit", limit);
            data.put("data", dataList);
            data.put("count", dataList.size());
            data.put("total", violationService.total(violation));

            return Result.getResultJson(200, "获取成功", data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     * 用户数据清理
     */
    @RequestMapping(value = "/clean")
    @ResponseBody
    public String clean(@RequestParam(value = "type") Integer type,
                        @RequestParam(value = "id") Integer id,
                        HttpServletRequest request) {
        try {
            //1是清理用户签到，2是清理用户资产日志，3是清理用户订单数据，4是清理无效卡密
            if (!permission(request.getHeader("Authorization"))) return Result.getResultJson(201, "无权限", null);
            String token = request.getHeader("Authorization");
            Users admin = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                admin = service.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
            }
            Users user = service.selectByKey(id);
            if (user == null) {
                return Result.getResultJson(0, "该用户不存在", null);
            }
            if (user.getGroup().equals("administrator")) {
                return Result.getResultJson(0, "不允许删除管理员的文章", null);
            }
            String text = null;
            //清除该用户所有文章
            if (type.equals(1)) {
                jdbcTemplate.execute("DELETE FROM " + this.prefix + "_contents WHERE authorId = " + id + ";");
                text = "文章数据";
            }
            //清除该用户所有评论
            if (type.equals(2)) {
                jdbcTemplate.execute("DELETE FROM " + this.prefix + "_comments WHERE authorId = " + id + ";");
                text = "评论数据";
            }
            //清除该用户所有动态
            if (type.equals(3)) {
                jdbcTemplate.execute("DELETE FROM " + this.prefix + "_space WHERE uid = " + id + ";");
                text = "动态数据";
            }
            //清除该用户所有商品
            if (type.equals(4)) {
                jdbcTemplate.execute("DELETE FROM " + this.prefix + "_shop WHERE uid = " + id + ";");
                text = "商品数据";
            }
            //清除该用户签到记录
            if (type.equals(5)) {
                jdbcTemplate.execute("DELETE FROM " + this.prefix + "_userlog WHERE type='clock' and uid = " + id + ";");
                text = "日志数据";
            }
            securityService.safetyMessage("管理员：" + admin.getName() + "，清除了用户" + user.getName() + "所有" + text, "system");
            return Result.getResultJson(200, "清除成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }

    }


    /***
     * 赠送vip
     *
     */
    @RequestMapping(value = "/giveVip")
    @ResponseBody
    public String giveVip(@RequestParam(value = "vid") Integer id,
                          @RequestParam(value = "days") Integer days,
                          HttpServletRequest request) {
        try {
            if (!permission(request.getHeader("Authorization"))) return Result.getResultJson(201, "无权限", null);
            Users user = service.selectByKey(id);
            if (user == null || user.toString().isEmpty()) return Result.getResultJson(201, "用户不存在", null);
            if (days == null || days.equals(0) || days.equals(""))
                return Result.getResultJson(201, "请输入正确天数", null);
            Long timeStamp = System.currentTimeMillis() / 1000;
            if (user.getVip().equals(1)) return Result.getResultJson(201, "该用户为永久VIP", null);
            if (user.getVip() > timeStamp) {
                user.setVip(user.getVip() + (86400 * days));
            } else {
                user.setVip((int) (timeStamp + (86400 * days)));
            }
            // 写入信息
            Inbox inbox = new Inbox();
            inbox.setText("管理员赠送了您" + days + "天的会员");
            inbox.setUid(0);
            inbox.setTouid(user.getUid());
            inbox.setType("system");
            inbox.setIsread(0);
            inbox.setValue(days);
            inboxService.insert(inbox);
            service.update(user);
            return Result.getResultJson(200, "赠送成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    @RequestMapping("/sign")
    @ResponseBody
    public String sign(HttpServletRequest request) {
        try {
            Apiconfig apiconfig = UStatus.getConfig(dataprefix, apiconfigService, redisTemplate);

            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = service.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user == null || user.toString().isEmpty()) return Result.getResultJson(201, "用户不存在", null);
            }

            if (redisHelp.getRedis("signed_" + user.getName().toString(), redisTemplate) != null)
                return Result.getResultJson(200, "今天已签到", null);
            // 获取当前日期
            LocalDate today = LocalDate.now();
            // 如果用户还没签到，计算距离今天结束还有多少秒
            LocalDateTime endOfToday = LocalDateTime.of(today, LocalTime.MAX);
            Duration durationUntilEndOfDay = Duration.between(LocalDateTime.now(), endOfToday);
            long secondsUntilEndOfDay = durationUntilEndOfDay.getSeconds();

            // 写入redis
            redisHelp.setRedis("signed_" + user.getName().toString(), "1", (int) secondsUntilEndOfDay, redisTemplate);

            // 给用户添加积分和经验
            user.setAssets(user.getAssets() + apiconfig.getClock());
            user.setExperience(user.getExperience() + apiconfig.getClockExp());
            service.update(user);
            //timestamp
            long timestamp = System.currentTimeMillis() / 1000;
            // 写入pay
            Paylog paylog = new Paylog();
            paylog.setUid(user.getUid());
            paylog.setCreated((int) timestamp);
            paylog.setPaytype("sign");
            paylog.setSubject("签到奖励");
            paylog.setTotalAmount(String.valueOf(apiconfig.getClock()));

            // 写入log
            Userlog userlog = new Userlog();
            userlog.setUid(user.getUid());
            userlog.setNum(apiconfig.getClockExp());
            userlog.setToid(user.getUid());
            userlog.setCreated((int) timestamp);
            userlog.setType("signExp");

            paylogService.insert(paylog);
            userlogService.insert(userlog);

            return Result.getResultJson(200, "签到成功，积分+" + apiconfig.getClock() + "经验+" + apiconfig.getClockExp(), null);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }


}