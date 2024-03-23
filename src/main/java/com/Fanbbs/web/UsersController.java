package com.Fanbbs.web;

import com.Fanbbs.common.*;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.Fanbbs.entity.*;
import com.Fanbbs.service.*;
import com.alibaba.fastjson.TypeReference;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

/***
 * Author Coruni
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

    @Autowired
    private RankService rankService;


    @Value("${mybatis.configuration.variables.prefix}")
    private String prefix;

    @Value("${web.prefix}")
    private String dataprefix;


    RedisHelp redisHelp = new RedisHelp();
    ResultAll Result = new ResultAll();
    baseFull baseFull = new baseFull();
    UserStatus UStatus = new UserStatus();
    HttpClient HttpClient = new HttpClient();
    PHPass phpass = new PHPass(8);


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
            Users user = getUser(token);
            Boolean permission = permission(user);
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
                opt = item.getOpt() != null && !item.getOpt().isEmpty() ? JSONObject.parseObject(item.getOpt()) : null;
                address = item.getAddress() != null && !item.getAddress().isEmpty() ? JSONObject.parseObject(item.getAddress()) : null;
                // 处理头像框
                // 加入其他数据等级等
                List<Integer> result = baseFull.getLevel(item.getExperience(), dataprefix, apiconfigService, redisTemplate);
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
                data.put("isFollow", isFollow);
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
     * 用户信息
     */
    @RequestMapping(value = "/userInfo")
    @ResponseBody
    public String userInfo(@RequestParam(value = "id", required = false) Integer id, HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Integer isFollow = 0;
            Integer fromFollow = 0;
            int related = 0;
            int isVip = 0;
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
            opt = user.getOpt() != null && !user.getOpt().toString().isEmpty() ? JSONObject.parseObject(user.getOpt()) : new JSONObject();
            address = user.getAddress() != null && !user.getAddress().toString().isEmpty() ? JSONObject.parseObject(user.getAddress()) : new JSONObject();

            // 处理会员
            if (user != null && user.getVip() != null && user.getVip() > System.currentTimeMillis() / 1000) isVip = 1;

            // 处理等级
            List levelInfo = baseFull.getLevel(user.getExperience(), dataprefix, apiconfigService, redisTemplate);
            Integer level = Integer.parseInt(levelInfo.get(0).toString());
            Integer nextExp = Integer.parseInt(levelInfo.get(1).toString());
            Map<String, Object> data = JSONObject.parseObject(JSONObject.toJSONString(user), Map.class);
            if (user != null && !user.toString().isEmpty()) {
                // 获取文章数量
                Article article = new Article();
                article.setAuthorId(user.getUid());
                article.setStatus("publish");
                Integer articleNum = contentsService.total(article, null);

                // 获取粉丝数量
                Fan fan = new Fan();
                fan.setTouid(user.getUid());
                Integer fans = fanService.total(fan);

                // 获取关注数量
                fan.setUid(user.getUid());
                fan.setTouid(null);
                Integer follows = fanService.total(fan);

                // 是否签到
                Userlog log = new Userlog();
                log.setUid(user.getUid());
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
                comment.setUid(user.getUid());
                Integer comments = commentsService.total(comment, null);
                // 加入数据
                data.put("articles", articleNum);
                data.put("fans", fans);
                data.put("follows", follows);
                data.put("clock", clock);
                data.put("comments", comments);
            }
            // 加入数据
            data.put("address", address);
            data.put("opt", opt);
            data.put("isFollow", isFollow);
            data.put("related", related);
            data.put("isVip", isVip);
            data.put("level", level);
            data.put("nextExp", nextExp);
            // 移除敏感数据
            data.remove("password");

            if(user.getUid()==null ||!user.getUid().equals(own.getUid())) data.remove("address");

            data.remove("mail");

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

            if (user.getStatus().equals(0)) return Result.getResultJson(201, "账号已注销", null);
            // 验证密码
            Boolean isPass = phpass.CheckPassword(password, user.getPassword());
            if (!isPass) {
                return Result.getResultJson(201, "密码错误", null);
            }
            // 生成Token
            Map token = new HashMap<>();
            token.put("sub ", "login");
            token.put("aud", user.getUid().toString());

            Map<String, Object> data = JSONObject.parseObject(JSONObject.toJSONString(user), Map.class);


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
     *
     * @param provider
     * @param openid
     * @param access_token
     * @param code
     * @return
     */
    @RequestMapping(value = "/OAuth")
    @ResponseBody
    public String OAuth(@RequestParam(value = "provider") String provider,
                        @RequestParam(value = "openid", required = false) String openid,
                        @RequestParam(value = "access_token", required = false) String access_token,
                        @RequestParam(value = "code", required = false) String code) {
        try {
            Apiconfig apiconfig = UStatus.getConfig(dataprefix, apiconfigService, redisTemplate);
            //定义一个变量存储获取到的第三方信息
            Map<String, Object> data = new HashMap<>();
            Users user = new Users();
            //先判定provider
            if (provider.equals("qq")) {
                Map<String, Object> userInfo = getOauthResult(access_token, openid, code, provider, apiconfig);
                if (userInfo.get("status").equals(0)) return Result.getResultJson(201, "数据不匹配", null);
                // 提取数据
                System.out.println(userInfo.get("info").toString());
                Map<String, Object> info = JSONObject.parseObject(JSONObject.toJSONString(userInfo.get("info")), Map.class);
                // 查询数据库是否存在
                Userapi userapi = new Userapi();
                userapi.setOpenId(openid);
                userapi.setAppLoginType(provider);
                List<Userapi> userapiList = userapiService.selectList(userapi);
                if (userapiList.size() == 0) {
                    // 数据为0 为用户创建新的账号
                    user.setStatus(1);
                    user.setAvatar(info.get("figureurl_qq_2").toString());
                    user.setScreenName(info.get("nickname").toString());
                    user.setName(userInfo.get("openid").toString().substring(0, 8));
                    user.setSex(info.get("gender").toString());
                    service.insert(user);
                    userapi.setAppLoginType(provider);
                    userapi.setOpenId(openid);
                    userapi.setUid(user.getUid());
                    userapiService.insert(userapi);
                    user = service.selectByKey(user.getUid());
                } else {
                    user = service.selectByKey(userapiList.get(0).getUid());
                }
            }

            if (provider.equals("weixin")) {
                String url = String.format("https://api.weixin.qq.com/sns/oauth2/access_token?appid=$s&secret=%s&code=%s&grant_type=authorization_code", apiconfig.getWxAppId(), apiconfig.getWxAppSecret(), code);
                CloseableHttpClient httpClient = HttpClients.createDefault();
                HttpGet httpGet = new HttpGet(url);
                CloseableHttpResponse response = httpClient.execute(httpGet);
                try {
                    HttpEntity entity = response.getEntity();
                    String result = EntityUtils.toString(entity);
                    EntityUtils.consume(entity);
                    Map<String, Object> info = JSONObject.parseObject(result, Map.class);
                    if (info.containsKey("access_token")) {
                        httpGet = new HttpGet(String.format("https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s", info.get("access_token"), info.get("openid")));
                        CloseableHttpResponse userResponse = httpClient.execute(httpGet);
                        try {
                            entity = userResponse.getEntity();
                            String userResult = EntityUtils.toString(entity);
                            EntityUtils.consume(entity);
                            Map<String, Object> userInfo = JSONObject.parseObject(userResult, Map.class);
                            Userapi userapi = new Userapi();
                            // 获取到了openid查询数据库是否存在
                            userapi.setOpenId(userInfo.get("openid").toString());
                            userapi.setAppLoginType(provider);
                            List<Userapi> userapiList = userapiService.selectList(userapi);
                            if (userapiList.size() == 0) {
                                // 数据为0 为用户创建新的账号
                                user.setStatus(1);
                                user.setGroup("contributor");
                                user.setAvatar(userInfo.get("headimgurl").toString());
                                user.setScreenName(userInfo.get("nickname").toString());
                                // 暂空 需要写入一个用户名
                                user.setSex(userInfo.get("sex").toString());
                                service.insert(user);
                                userapi.setAppLoginType(provider);
                                userapi.setOpenId(openid);
                                userapi.setUid(user.getUid());
                                userapiService.insert(userapi);
                                user = service.selectByKey(user.getUid());
                            } else {
                                user = service.selectByKey(userapiList.get(0).getUid());

                            }
                        } finally {
                            userResponse.close();
                        }
                    } else {
                        return Result.getResultJson(201, info.get("errmsg").toString(), null);
                    }
                } finally {
                    response.close();
                }
            }

            // 生成Token
            Map token = new HashMap<>();
            token.put("sub ", "login");
            token.put("aud", user.getUid().toString());
            data = JSONObject.parseObject(JSONObject.toJSONString(user), Map.class);

            List level = baseFull.getLevel(user.getExperience(), dataprefix, apiconfigService, redisTemplate);
            data.put("level", level.get(0));
            data.put("nextExp", level.get(1));
            data.put("token", JWT.getToken(token));
            data.remove("password");
            if (user.getAddress() != null && !user.getAddress().isEmpty()) {
                data.put("address", JSONObject.parseObject(user.getAddress()));
            }
            return Result.getResultJson(200, "登录成功", data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);

        }
    }

    private Map<String, Object> getOauthResult(String access_token, String openid, String code, String provider, Apiconfig apiconfig) {
        Map<String, Object> data = new HashMap<>();
        try {
            if (provider.equals("qq")) {
                String url = String.format("https://graph.qq.com/user/get_user_info?access_token=%s&oauth_consumer_key=%s&openid=%s", access_token, apiconfig.getQqAppletsAppid(), openid);
                CloseableHttpClient httpClient = HttpClients.createDefault();
                HttpGet httpGet = new HttpGet(url);
                CloseableHttpResponse response = httpClient.execute(httpGet);
                Map<String, Object> info = new HashMap<>();
                try {
                    HttpEntity entity = response.getEntity();
                    String result = EntityUtils.toString(entity);
                    EntityUtils.consume(entity);
                    info = JSONObject.parseObject(result, Map.class);
                    //  如果获取到ret状态不为0则返回错误信息
                    if (!info.get("ret").equals(0)) {
                        data.put("status", 0);
                        data.put("info", info);
                        return data;
                    }
                } finally {
                    response.close();
                }

                // 再获取用户openid
                url = String.format("https://graph.qq.com/oauth2.0/me?access_token=%s&fmt=json", access_token);
                httpGet = new HttpGet(url);
                CloseableHttpResponse openidRes = httpClient.execute(httpGet);

                try {
                    HttpEntity openidEntity = openidRes.getEntity();
                    String openidResult = EntityUtils.toString(openidEntity);
                    EntityUtils.consume(openidEntity);
                    Map<String, Object> openidInfo = JSONObject.parseObject(openidResult, Map.class);
                    if (!openidInfo.get("openid").equals(openid)) {
                        data.put("status", 0);
                        return data;
                    }
                    data.put("status", 1);
                    data.put("info", info);
                    data.put("openid", openidInfo.get("openid"));
                } finally {
                    if (openidRes != null) {
                        openidRes.close();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
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
            } catch (Exception e) {
                e.printStackTrace();
                return Result.getResultJson(201, "邮件发送错误", null);
            }
            return Result.getResultJson(200, "验证码已发送，有效时长10分钟", null);
        } catch (Exception e) {
            return Result.getResultJson(400, "不正确的邮箱发信配置", null);
        }

    }

    /***
     * 重置密码
     * @param account
     * @param password
     * @param code
     * @return
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
                if (userList.isEmpty()) return Result.getResultJson(201, "用户不存在", null);
                user = userList.get(0);
            } else {
                user.setMail(account);
                List<Users> userList = service.selectList(user);
                if (userList.isEmpty()) return Result.getResultJson(201, "用户不存在", null);
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
            Users user = getUser(token);
            if (user.getUid() == null || user.getUid().equals(0)) return Result.getResultJson(201, "用户不存在", null);
            user.setClientId(id);
            service.update(user);
            return Result.getResultJson(200, "设置成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    @RequestMapping(value = "/changePassword")
    @ResponseBody
    public String changePassword(@RequestParam(value = "newPassword") String newPassword,
                                 @RequestParam(value = "oldPassword") String oldPassword,
                                 HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Users user = getUser(token);
            if (user.getUid() == null || user.toString().isEmpty())
                return Result.getResultJson(201, "用户不存在，请重新登录", null);

            if (!phpass.CheckPassword(oldPassword, user.getPassword()))
                return Result.getResultJson(201, "密码不正确", null);
            // 验证通过写入新hash
            user.setPassword(phpass.HashPassword(newPassword));
            service.update(user);
            return Result.getResultJson(200, "修改成功", null);

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
                         @RequestParam(value = "address", required = false) String address,
                         @RequestParam(value = "background", required = false) String background,
                         @RequestParam(value = "mail", required = false) String mail,
                         @RequestParam(value = "code", required = false) String code,
                         HttpServletRequest request) {
        try {
            Apiconfig apiconfig = UStatus.getConfig(dataprefix, apiconfigService, redisTemplate);
            String token = request.getHeader("Authorization");
            Users user = getUser(token);
            if (user.getUid() == null || user.getUid().equals(0)) return Result.getResultJson(201, "用户不存在", null);

            user.setScreenName(nickname);
            user.setAvatar(avatar);
            user.setUserBg(background);
            user.setSex(sex);
            user.setIntroduce(introduce);
            user.setAddress(address);
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
            @RequestParam(value = "vip", required = false) Integer vip,
            HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            boolean permission = permission(getUser(token));
            if (!permission) return Result.getResultJson(201, "无权限", null);
            Users user = service.selectByKey(id);
            user.setOpt(opt);
            user.setGroup(group);
            user.setScreenName(nickname);
            user.setSex(sex);
            user.setIntroduce(introduce);
            user.setMail(mail);
            user.setVip(vip);
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
            Users user = getUser(token);
            if (!permission(user)) {
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
            Users user = getUser(token);
            if (user.getUid() == null || user.getUid().equals(0)) return Result.getResultJson(201, "用户不存在", null);
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

            String token = request.getHeader("Authorization");
            Users user = getUser(token);
            Boolean permission = permission(user);

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
            String token = request.getHeader("Authorization");
            if (!permission(getUser(token))) return Result.getResultJson(201, "无权限", null);
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
            String token = request.getHeader("Authorization");
            if (!permission(getUser(token))) return Result.getResultJson(201, "无权限", null);
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
            String token = request.getHeader("Authorization");
            Users user = getUser(token);
            if (!permission(user)) return Result.getResultJson(201, "无权限", null);
            if (num == null || num.equals("") || num.equals(0))
                return Result.getResultJson(201, "请输入正确的数量", null);
            Invitation invite = new Invitation();
            int user_id = user.getUid();
            Long timeStamp = System.currentTimeMillis() / 1000;
            invite.setCreated(Math.toIntExact(timeStamp));
            invite.setUid(user_id);
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
            String token = request.getHeader("Authorization");
            Users user = getUser(token);
            if (!permission(user)) return Result.getResultJson(201, "无权限", null);
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
    public String codeExcel(@RequestParam(value = "type", defaultValue = "0") Integer type,
                            HttpServletResponse response,
                            HttpServletRequest request) throws IOException {
        try {
            String token = request.getHeader("Authorization");
            if (!permission(getUser(token))) return Result.getResultJson(201, "无权限", null);

            Invitation query = new Invitation();
            query.setStatus(type);
            List<Invitation> invitationList = invitationService.selectList(query);

            try (Workbook workbook = new XSSFWorkbook()) {
                String[] headers = {"ID", "邀请码", "创建人"};
                Sheet sheet = workbook.createSheet("invite Data");
                // 写入表头
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < headers.length; i++) {
                    headerRow.createCell(i).setCellValue(headers[i]);
                }
                for (int i = 0; i < invitationList.size(); i++) {
                    Row row = sheet.createRow(i + 1);
                    Invitation invitation = invitationList.get(i);
                    row.createCell(0).setCellValue(invitation.getId());
                    row.createCell(1).setCellValue(invitation.getCode());
                    row.createCell(2).setCellValue(invitation.getUid());
                    row.createCell(3).setCellValue(invitation.getStatus() > 0 ? "已使用" : "未使用");
                    row.createCell(5).setCellValue(invitation.getCreated());
                }

                response.setContentType("application/octet-stream");
                response.setHeader("Content-Disposition", "attachment; filename=邀请码.xlsx");
                workbook.write(response.getOutputStream());
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return Result.getResultJson(500, "导出Excel文件失败", null);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    @RequestMapping(value = "/delCode")
    @ResponseBody
    public String delCode(@RequestParam(value = "id") Integer id,
                          HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (!permission(getUser(token))) return Result.getResultJson(201, "无权限", null);
            invitationService.delete(id);
            return Result.getResultJson(200, "删除成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
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
            Users user = getUser(token);
            if (user.getUid() == null || user.toString().isEmpty())
                return Result.getResultJson(201, "用户不存在，请重新登录", null);
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
                    Map<String, Object> dataSender;
                    if (sender != null && !sender.toString().isEmpty()) {
                        dataSender = JSONObject.parseObject(JSONObject.toJSONString(sender));
                    } else {
                        dataSender = new HashMap<>();
                    }
                    if (sender != null && !sender.toString().isEmpty()) {
                        dataSender.remove("password");
                        dataSender.remove("address");
                        dataSender.remove("assets");
                        dataSender.remove("opt");
                        dataSender.remove("head_picture");
                        dataSender.remove("mail");
                    } else {
                        dataSender.put("screenName", "用户已注销");
                        dataSender.put("isFollow", 0);
                        dataSender.put("isVip", 0);
                        dataSender.put("avatar", null);
                    }

                    // 查询回复的评论
                    Comments reply = commentsService.selectByKey(_inbox.getValue());
                    Map<String, Object> dataReply = JSONObject.parseObject(JSONObject.toJSONString(reply));
                    Map<String, Object> articleData = new HashMap<>();
                    if (reply != null && !reply.toString().isEmpty()) {
                        JSONArray images = new JSONArray();
                        try {
                            images = reply.getImages() != null && !reply.getImages().toString().isEmpty() ? JSONArray.parseArray(reply.getImages()) : null;

                        } catch (Exception e) {
                            images = null;
                        }
                        dataReply.put("images", images);
                        // 查询评论的用户
                        Users replyUser = service.selectByKey(reply.getUid());
                        Map<String, Object> dataReplyUser;
                        if (replyUser != null && !replyUser.toString().isEmpty()) {
                            dataReplyUser = JSONObject.parseObject(JSONObject.toJSONString(replyUser), Map.class);
                        } else {
                            dataReplyUser = new HashMap<>();
                        }
                        if (replyUser != null && !replyUser.toString().isEmpty()) {
                            dataReplyUser.remove("password");
                            dataReplyUser.remove("address");
                            dataReplyUser.remove("assets");
                            dataReplyUser.remove("opt");
                            dataReplyUser.remove("head_picture");
                            dataReplyUser.remove("mail");
                        } else {
                            dataReplyUser.put("screenName", "用户已注销");
                            dataReplyUser.put("isFollow", 0);
                            dataReplyUser.put("isVip", 0);
                            dataReplyUser.put("avatar", null);
                        }

                        Article article = contentsService.selectByKey(reply.getCid());
                        // 如果不存在的话

                        if (article != null && !article.toString().isEmpty()) {
                            articleData.put("title", article.getTitle());
                            articleData.put("authorId", article.getAuthorId());
                            articleData.put("id", article.getCid());
                            articleData.put("type",article.getType());
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
            Users user = getUser(token);
            if (user.getUid() == null || user.toString().isEmpty())
                return Result.getResultJson(201, "用户不存在，请重新登录", null);
            Inbox inbox = new Inbox();
            inbox.setTouid(user.getUid());
            inbox.setIsread(0);
            inbox.setType("comment");
            int comments = inboxService.total(inbox);
            inbox.setType("system");
            int systems = inboxService.total(inbox);
            inbox.setType("finance");
            int finances = inboxService.total(inbox);

            Map<String, Object> data = new HashMap<>();
            data.put("comments", comments);
            data.put("systems", systems);
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
            Users user = getUser(token);
            if (user.getUid() == null || user.toString().isEmpty())
                return Result.getResultJson(201, "用户不存在，请重新登录", null);
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
            String token = request.getHeader("Authorization");
            if (!permission(getUser(token))) return Result.getResultJson(201, "无权限", null);
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
            Users user = getUser(token);
            if (user.getUid() == null || user.toString().isEmpty())
                return Result.getResultJson(201, "用户不存在", null);
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
            Users user = getUser(token);
            if (user.getUid() == null || user.toString().isEmpty())
                return Result.getResultJson(201, "用户不能存在", null);
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
            Users user = getUser(token);
            if (user.getUid() == null || user.toString().isEmpty())
                return Result.getResultJson(201, "用户不存在", null);
            if (!permission(user)) return Result.getResultJson(201, "无权限", null);
            Long timeStamp = System.currentTimeMillis() / 1000;
            Long banTime = timeStamp + (days * 86400);
            //查询用户是否存在
            Users banUser = service.selectByKey(id);
            if (banUser == null || banUser.toString().isEmpty()) return Result.getResultJson(201, "用户不存在", null);
            if (banUser.getBantime() > timeStamp) return Result.getResultJson(201, "用户封禁中", null);
            if (days == null || days.equals(0) || days.equals(""))
                return Result.getResultJson(201, "请输入封禁天数", null);

            // 写入封禁记录
            Violation violation = new Violation();
            violation.setCreated(Math.toIntExact(timeStamp));
            violation.setUid(banUser.getUid());
            violation.setType("ban");
            violation.setText(text);
            violation.setHandler(banUser.getUid());
            violationService.insert(violation);
            // 更新用户信息
            banUser.setBantime(Math.toIntExact(banTime));
            service.update(banUser);

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
            String token = request.getHeader("Authorization");
            Users user = getUser(token);
            if (user.getUid() == null) return Result.getResultJson(201, "用户不存在", null);
            if (!permission(user)) return Result.getResultJson(201, "无权限", null);

            Long timeStamp = System.currentTimeMillis() / 1000;
            Users banUser = service.selectByKey(id);
            if (banUser == null || banUser.toString().isEmpty()) return Result.getResultJson(201, "用户不存在", null);
            if (banUser.getBantime() < timeStamp) return Result.getResultJson(201, "该用户状态正常", null);

            // 更改用户的封禁时间
            banUser.setBantime(Math.toIntExact(timeStamp));
            service.update(banUser);
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
            String token = request.getHeader("Authorization");
            Users user = getUser(token);
            if (!permission(user)) return Result.getResultJson(201, "无权限", null);

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
            String token = request.getHeader("Authorization");
            Users user = getUser(token);
            //1是清理用户签到，2是清理用户资产日志，3是清理用户订单数据，4是清理无效卡密
            if (!permission(user)) return Result.getResultJson(201, "无权限", null);
            Users clanUser = service.selectByKey(id);
            if (clanUser == null) {
                return Result.getResultJson(0, "该用户不存在", null);
            }
            if (clanUser.getGroup().equals("administrator")) {
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
            securityService.safetyMessage("管理员：" + user.getName() + "，清除了用户" + user.getName() + "所有" + text, "system");
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
            String token = request.getHeader("Authorization");
            Users user = getUser(token);
            if (!permission(user)) return Result.getResultJson(201, "无权限", null);
            Users giftUser = service.selectByKey(id);
            if (giftUser == null || giftUser.toString().isEmpty()) return Result.getResultJson(201, "用户不存在", null);
            if (days == null || days.equals(0) || days.equals(""))
                return Result.getResultJson(201, "请输入正确天数", null);
            Long timeStamp = System.currentTimeMillis() / 1000;
            if (giftUser.getVip().equals(1)) return Result.getResultJson(201, "该用户为永久VIP", null);
            if (giftUser.getVip() > timeStamp) {
                giftUser.setVip(giftUser.getVip() + (86400 * days));
            } else {
                giftUser.setVip((int) (timeStamp + (86400 * days)));
            }
            // 写入信息
            Inbox inbox = new Inbox();
            inbox.setText("管理员赠送了您" + days + "天的会员");
            inbox.setUid(0);
            inbox.setTouid(giftUser.getUid());
            inbox.setType("system");
            inbox.setIsread(0);
            inbox.setValue(days);
            inboxService.insert(inbox);
            service.update(giftUser);
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
            Users user = getUser(token);
            if (user.getUid() == null || user.toString().isEmpty())
                return Result.getResultJson(201, "用户不存在", null);

            if (redisHelp.getRedis("signed_" + user.getName().toString(), redisTemplate) != null)
                return Result.getResultJson(200, "今天已签到", null);

            // 获取今天结束时间
            Integer endTime = baseFull.endTime();
            // 写入redis
            redisHelp.setRedis("signed_" + user.getName().toString(), "1", endTime, redisTemplate);

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
            paylog.setStatus(1);
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

    @RequestMapping(value = "/tasks")
    @ResponseBody
    public String tasks(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Users user = getUser(token);
            if (user.getUid() == null || user.toString().isEmpty())
                return Result.getResultJson(201, "用户不存在", null);
            // 初始化返回信息
            int isSign = 0;
            int likes = 0;
            int views = 0;
            int shares = 0;
            int marks = 0;

            Map<String, Object> data = new HashMap<>();
            if (redisHelp.getRedis("signed_" + user.getName().toString(), redisTemplate) != null)
                isSign = 1;

            if (redisHelp.getRedis("likes_" + user.getName(), redisTemplate) != null)
                likes = Integer.parseInt(redisHelp.getRedis("likes_" + user.getName(), redisTemplate));

            if (redisHelp.getRedis("views_" + user.getName(), redisTemplate) != null)
                views = Integer.parseInt(redisHelp.getRedis("views_" + user.getName(), redisTemplate));

            if (redisHelp.getRedis("marks_" + user.getName(), redisTemplate) != null)
                marks = Integer.parseInt(redisHelp.getRedis("marks_" + user.getName(), redisTemplate));

            if (redisHelp.getRedis("shares_" + user.getName(), redisTemplate) != null)
                shares = Integer.parseInt(redisHelp.getRedis("shares_" + user.getName(), redisTemplate));


            data.put("isSign", isSign);
            data.put("likes", likes);
            data.put("views", views);
            data.put("marks", marks);
            data.put("shares", shares);
            return Result.getResultJson(200, "获取成功", data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }


    }

    @RequestMapping(value = "/destroy")
    @ResponseBody
    public String destory(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = service.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user == null || user.toString().isEmpty()) return Result.getResultJson(201, "用户不存在", null);
            }
            user.setStatus(0);
            service.update(user);
            return Result.getResultJson(200, "注销成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口错误", null);
        }
    }

    /***
     * 权限判断
     * @param user
     * @return
     */
    private boolean permission(Users user) {
        if (user.getUid() == null || user.getUid().equals(0)) return false;
        if (user.getGroup().equals("administrator") || user.getGroup().equals("editor")) return true;
        return false;
    }

    /***
     * 获取用户信息
     * @param token
     * @return
     */
    private Users getUser(String token) {
        if (token == null || token.isEmpty()) return new Users();
        // 获取用户信息
        DecodedJWT verify = JWT.verify(token);
        Users user = service.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
        return user;
    }
}