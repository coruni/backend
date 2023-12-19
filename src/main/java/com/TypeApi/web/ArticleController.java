package com.TypeApi.web;

import com.TypeApi.common.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.TypeApi.entity.*;
import com.TypeApi.service.*;
import net.dreamlu.mica.xss.core.XssCleanIgnore;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * 控制层
 * ArticleController
 */
@Component
@Controller
@RequestMapping(value = "/article")
public class ArticleController {

    @Autowired
    ArticleService service;

    @Autowired
    private ShopService shopService;

    @Autowired
    private PaylogService paylogService;

    @Autowired
    private FieldsService fieldsService;

    @Autowired
    private RelationshipsService relationshipsService;

    @Autowired
    private UserlogService userlogService;

    @Autowired
    private HeadpictureService headpictureService;

    @Autowired
    private CategoryService metasService;

    @Autowired
    private UsersService usersService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private CommentsService commentsService;

    @Autowired
    private ApiconfigService apiconfigService;

    @Autowired
    private FanService fanService;

    @Autowired
    private PushService pushService;

    @Autowired
    private InboxService inboxService;

    @Autowired
    private SpaceService spaceService;

    @Autowired
    private AdsService adsService;


    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MailService MailService;

    @Value("${webinfo.contentCache}")
    private Integer contentCache;

    @Value("${webinfo.contentInfoCache}")
    private Integer contentInfoCache;


    @Value("${web.prefix}")
    private String dataprefix;

    @Value("${mybatis.configuration.variables.prefix}")
    private String prefix;


    @Autowired
    private JdbcTemplate jdbcTemplate;
    RedisHelp redisHelp = new RedisHelp();
    ResultAll Result = new ResultAll();
    baseFull baseFull = new baseFull();
    UserStatus UStatus = new UserStatus();
    HttpClient HttpClient = new HttpClient();
    EditFile editFile = new EditFile();

    /**
     * 查询文章详情
     */
    @RequestMapping(value = "/info")
    @ResponseBody
    public String contentsInfo(@RequestParam(value = "key", required = false) String key, @RequestParam(value = "isMd", required = false, defaultValue = "0") Integer isMd, @RequestParam(value = "token", required = false) String token, HttpServletRequest request) {
        Article article = null;

        //如果开启全局登录，则必须登录才能得到数据
        Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
        Apiconfig apiconfig = UStatus.getConfig(this.dataprefix, apiconfigService, redisTemplate);
        if (apiconfig.getIsLogin().equals(1)) {
            if (uStatus == 0) {
                return Result.getResultJson(0, "用户未登录或Token验证失败", null);
            }
        }
        //验证结束
        Integer uid = null;

        Map contensjson = new HashMap<String, String>();
        Map cacheInfo = redisHelp.getMapValue(this.dataprefix + "_" + "contentsInfo_" + key + "_" + isMd, redisTemplate);

        try {
            Integer isLogin;
            if (uStatus == 0) {
                isLogin = 0;
            } else {
                isLogin = 1;
                if (token != null && !token.isEmpty()) {
                    Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
                    uid = Integer.parseInt(map.get("uid").toString());
                }
            }
            //如果是登录用户，且传入了token，就不缓存
            if (cacheInfo.size() > 0 & isLogin == 0) {
                contensjson = cacheInfo;
            } else {
                article = service.selectByKey(key);
                if (article == null) {
                    return Result.getResultJson(0, "该文章不存在", null);
                }
                if (!article.getStatus().equals("publish")) {
                    return Result.getResultJson(0, "文章暂未公开访问", null);
                }
                String text = article.getText();
                String oldText = article.getText();
                //要做处理将typecho的图片插入格式变成markdown
                List imgList = baseFull.getImageSrc(text);
                List codeList = baseFull.getImageCode(text);
                for (int c = 0; c < codeList.size(); c++) {
                    String codeimg = codeList.get(c).toString();
                    String urlimg = imgList.get(c).toString();
                    text = text.replace(codeimg, "![image" + c + "](" + urlimg + ")");
                }
                text = text.replace("<!--markdown-->", "");
                List codeImageMk = baseFull.getImageMk(text);
                for (int d = 0; d < codeImageMk.size(); d++) {
                    String mk = codeImageMk.get(d).toString();
                    text = text.replace(mk, "");
                }
                if (isMd == 1) {
                    //如果isMd等于1，则输出解析后的md代码
                    Parser parser = Parser.builder().build();
                    Node document = parser.parse(text);
                    HtmlRenderer renderer = HtmlRenderer.builder().build();
                    text = renderer.render(document);

                }
                // 用正则表达式匹配并替换[hide type=pay]这是付费查看的内容[/hide]，并根据type值替换成相应的提示
                Integer isReply = 0;
                Integer isPaid = 0;
                if (uid != null && uid != 0) {
                    // 获取评论状态
                    Comments replyStatus = new Comments();
                    replyStatus.setCid(article.getCid());
                    replyStatus.setAuthorId(uid);
                    Integer rStatus = commentsService.total(replyStatus, "");
                    if (rStatus > 0) {
                        isReply = 1;
                    }
                    // 获取购买状态
                    Paylog paylog = new Paylog();
                    paylog.setPaytype("article");
                    paylog.setUid(uid);
                    paylog.setCid(article.getCid());
                    Integer pStatus = paylogService.total(paylog);
                    if (pStatus > 0) {
                        isPaid = 1;
                    }
                }

                Pattern pattern = Pattern.compile("\\[hide type=(pay|reply)\\](.*?)\\[/hide\\]");
                Matcher matcher = pattern.matcher(text);
                StringBuffer replacedText = new StringBuffer();
                while (matcher.find()) {
                    String type = matcher.group(1);
                    String content = matcher.group(2);
                    String replacement = "";
                    if ("pay".equals(type) && isPaid == 0 && uid != article.getAuthorId()) {
                        replacement = "【付费查看：这是付费内容，付费后可查看】";
                    } else if ("reply".equals(type) && isReply == 0 && uid != article.getAuthorId()) {
                        replacement = "【回复查看：这是回复内容，回复后可查看】";
                    } else {
                        replacement = content;  // 如果不需要替换，则保持原样
                    }
                    matcher.appendReplacement(replacedText, replacement);

                }
                text = matcher.appendTail(replacedText).toString();

                // 获取是否islike && isMark
                Integer isLike = 0;
                Integer isMark = 0;
                if (uid != null) {
                    Userlog searchParams = new Userlog();
                    searchParams.setUid(uid);
                    searchParams.setCid(article.getCid());
                    List<Userlog> likeList = userlogService.selectList(searchParams);
                    if (likeList.size() > 0) {
                        for (int i = 0; i < likeList.size(); i++) {
                            String type = likeList.get(i).getType().toString();
                            if (type.equals("likes")) {
                                isLike = 1;
                            } else if (type.equals("mark")) {
                                isMark = 1;
                            }
                        }
                    }
                }


                // 加入文章作者信息
                Map authorInfo = new HashMap();
                if (article.getAuthorId() != null) {
                    Users author = usersService.selectByKey(article.getAuthorId());

                    if (author != null) {
                        String name = author.getName();
                        if (author.getScreenName() != null && author.getScreenName() != "") {
                            name = author.getScreenName();
                        }
                        String avatar = apiconfig.getWebinfoAvatar() + "null";
                        if (author.getAvatar() != null && author.getAvatar() != "") {
                            avatar = author.getAvatar();
                        } else {
                            if (author.getMail() != null && author.getMail() != "") {
                                String mail = author.getMail();

                                if (mail.indexOf("@qq.com") != -1) {
                                    String qq = mail.replace("@qq.com", "");
                                    avatar = "https://q1.qlogo.cn/g?b=qq&nk=" + qq + "&s=640";
                                } else {
                                    avatar = baseFull.getAvatar(apiconfig.getWebinfoAvatar(), author.getMail());
                                }
                                //avatar = baseFull.getAvatar(apiconfig.getWebinfoAvatar(), author.getMail());
                            }
                        }

                        // 是否关注
                        if (uid != null) {
                            Fan fan = new Fan();
                            Integer authorId = Integer.parseInt(article.getAuthorId().toString());
                            fan.setUid(uid);
                            fan.setTouid(authorId);
                            Integer isfollow = fanService.total(fan);
                            authorInfo.put("isfollow", isfollow);
                        }

                        JSONObject opt = JSONObject.parseObject(author.getOpt());
                        if (opt instanceof Object) {
                            opt = JSONObject.parseObject(author.getOpt());
                            Integer headId = Integer.parseInt(opt.get("head_picture").toString());
                            // 查询opt中head_picture的数据 并替换
                            Headpicture head_picture = headpictureService.selectByKey(headId);
                            if (head_picture != null) {
                                opt.put("head_picture", head_picture.getLink().toString());
                            }
                        }
                        // 获取用户等级
                        List<Integer> levelAndExp = baseFull.getLevel(author.getExperience());
                        Integer level = levelAndExp.get(0);
                        Integer nextExp = levelAndExp.get(1);
                        authorInfo.put("name", name);
                        authorInfo.put("avatar", avatar);
                        authorInfo.put("customize", author.getCustomize());
                        authorInfo.put("opt", opt);
                        authorInfo.put("level", level);
                        authorInfo.put("nextExp", nextExp);
                        authorInfo.put("experience", author.getExperience());
                        authorInfo.put("introduce", author.getIntroduce());
                        //判断是否为VIP
                        authorInfo.put("isvip", 0);
                        Long date = System.currentTimeMillis();
                        String curTime = String.valueOf(date).substring(0, 10);
                        Integer viptime = author.getVip();

                        if (viptime > Integer.parseInt(curTime) || viptime.equals(1)) {
                            authorInfo.put("isvip", 1);
                        }
                        if (viptime.equals(1)) {
                            //永久VIP
                            authorInfo.put("isvip", 2);
                        }
                    } else {
                        authorInfo.put("name", "用户已注销");
                        authorInfo.put("avatar", apiconfig.getWebinfoAvatar() + "null");
                    }
                }

                //获取文章id，从而获取自定义字段，和分类标签
                String cid = article.getCid().toString();
                Fields f = new Fields();
                f.setCid(Integer.parseInt(cid));
                List<Fields> fields = fieldsService.selectList(f);
                Relationships rs = new Relationships();
                rs.setCid(Integer.parseInt(cid));
                List<Relationships> relationships = relationshipsService.selectList(rs);

                List metas = new ArrayList();
                List tags = new ArrayList();
                for (int i = 0; i < relationships.size(); i++) {
                    Map json = JSONObject.parseObject(JSONObject.toJSONString(relationships.get(i)), Map.class);
                    if (json != null) {
                        String mid = json.get("mid").toString();
                        Category metasList = metasService.selectByKey(mid);
                        if (metasList != null) {
                            Map metasInfo = JSONObject.parseObject(JSONObject.toJSONString(metasList), Map.class);
                            String type = metasInfo.get("type").toString();
                            if (type.equals("category")) {
                                metas.add(metasInfo);
                            }
                            if (type.equals("tag")) {
                                tags.add(metasInfo);
                            }
                        }

                    }

                }
                contensjson = JSONObject.parseObject(JSONObject.toJSONString(article), Map.class);
                // 格式化文章opt
                JSONObject opt = JSONObject.parseObject(contensjson.get("opt").toString());
                if (opt instanceof Object) {
                    opt = JSONObject.parseObject(contensjson.get("opt").toString());
                } else {
                    opt = null;
                }
                Object imagesObject = contensjson.get("images");
                // 判断值是否为 null
                if (imagesObject != null) {
                    // 判断值的类型是否为 JSONArray
                    if (imagesObject instanceof JSONArray) {
                        // 如果是 JSONArray，直接使用
                        contensjson.put("images", imagesObject);
                    } else {
                        // 如果不是 JSONArray，尝试将其解析为 JSONArray
                        JSONArray imagesArray = JSON.parseArray(imagesObject.toString());

                        // 判断解析结果是否为 null 或者为空
                        if (imagesArray == null || imagesArray.isEmpty()) {
                            // 如果为空，将其替换为 imgList
                            contensjson.put("images", imgList);
                        } else {
                            // 如果不为空，直接使用解析的值
                            contensjson.put("images", imagesArray);
                        }
                    }
                } else {
                    // 如果值为 null，将其替换为 imgList
                    contensjson.put("images", imgList);
                }
                //转为map，再加入字段
                contensjson.remove("password");
                contensjson.put("fields", fields);
                contensjson.put("category", metas);
                contensjson.put("authorInfo", authorInfo);
                contensjson.put("isLike", isLike);
                contensjson.put("isMark", isMark);
                contensjson.put("tag", tags);
                contensjson.put("text", text);
                contensjson.put("opt", opt);
                boolean status = oldText.contains("<!--markdown-->");
                if (status) {
                    contensjson.put("markdown", 1);
                } else {
                    contensjson.put("markdown", 0);
                }

                //文章阅读量增加
                String agent = request.getHeader("User-Agent");
                String ip = baseFull.getIpAddr(request);
                String isRead = redisHelp.getRedis(this.dataprefix + "_" + "isRead" + "_" + ip + "_" + agent + "_" + key, redisTemplate);
                if (isRead == null) {
                    //添加阅读量
                    Integer views = Integer.parseInt(contensjson.get("views").toString());
                    views = views + 1;
                    Article toContents = new Article();
                    toContents.setCid(Integer.parseInt(key));
                    toContents.setViews(views);
                    service.update(toContents);

                }
                redisHelp.setRedis(this.dataprefix + "_" + "isRead" + "_" + ip + "_" + agent + "_" + key, "yes", 900, redisTemplate);
                redisHelp.delete(this.dataprefix + "_" + "contentsInfo_" + key + "_" + isMd, redisTemplate);
                redisHelp.setKey(this.dataprefix + "_" + "contentsInfo_" + key + "_" + isMd, contensjson, this.contentInfoCache, redisTemplate);
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (cacheInfo.size() > 0) {
                contensjson = cacheInfo;
            }
        }

        JSONObject concentInfo = JSON.parseObject(JSON.toJSONString(contensjson), JSONObject.class);
        return concentInfo.toJSONString();
        //return new ApiResult<>(ResultCode.success.getCode(), typechoContents, ResultCode.success.getDescr(), request.getRequestURI());
    }


    /***
     * 表单查询请求
     * @param searchParams Bean对象JSON字符串
     * @param page         页码
     * @param limit        每页显示数量
     */
    @RequestMapping(value = "/articleList")
    @ResponseBody
    public String contentsList(@RequestParam(value = "searchParams", required = false) String searchParams,
                               @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                               @RequestParam(value = "limit", required = false, defaultValue = "15") Integer limit,
                               @RequestParam(value = "searchKey", required = false, defaultValue = "") String searchKey,
                               @RequestParam(value = "order", required = false, defaultValue = "") String order,
                               @RequestParam(value = "random", required = false, defaultValue = "0") Integer random,
                               @RequestParam(value = "token", required = false, defaultValue = "") String token) {
        Article query = new Article();
        if (limit > 50) {
            limit = 50;
        }
        String sqlParams = "null";
        List cacheList = new ArrayList();
        String group = "";
        Integer total = 0;
        Integer uid = null;
        //如果开启全局登录，则必须登录才能得到数据
        Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
        Apiconfig apiconfig = UStatus.getConfig(this.dataprefix, apiconfigService, redisTemplate);
        if (apiconfig.getIsLogin().equals(1)) {
            if (uStatus == 0) {
                return Result.getResultJson(0, "用户未登录或Token验证失败", null);
            } else {
                if (token != null && !token.isEmpty()) {
                    Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
                    uid = Integer.parseInt(map.get("uid").toString());
                }
            }
        }
        //验证结束


        if (StringUtils.isNotBlank(searchParams)) {
            JSONObject object = JSON.parseObject(searchParams);
            //如果不是登陆状态，那么只显示开放状态文章。如果是，则查询自己发布的文章

            if (token == "" || uStatus == 0) {

                object.put("status", "publish");
            } else {
                if (object.get("status") == null) {
                    object.put("status", "publish");
                }
                //后面再优化
                // aid = redisHelp.getValue(this.dataprefix+"_"+"userInfo"+token,"uid",redisTemplate).toString();
//                Map map =redisHelp.getMapValue(this.dataprefix+"_"+"userInfo"+token,redisTemplate);
//                group = map.get("group").toString();
//                if(!group.equals("administrator")&&!group.equals("editor")){
//                    object.put("authorId",aid);
//                }

            }

            query = object.toJavaObject(Article.class);
            Map paramsJson = JSONObject.parseObject(JSONObject.toJSONString(query), Map.class);
            sqlParams = paramsJson.toString();

        }
        total = service.total(query, searchKey);
        List jsonList = new ArrayList();
        //管理员和编辑以登录状态请求时，不调用缓存
        if (!group.equals("administrator") && !group.equals("editor")) {
            cacheList = redisHelp.getList(this.dataprefix + "_" + "contentsList_" + page + "_" + limit + "_" + sqlParams + "_" + order + "_" + searchKey + "_" + random, redisTemplate);
        }
        //监听异常，如果有异常则调用redis缓存中的list，如果无异常也调用redis，但是会更新数据
        try {
            if (cacheList.size() > 0) {
                jsonList = cacheList;
            } else {

                PageList<Article> pageList = service.selectPage(query, page, limit, searchKey, order, random);
                List list = pageList.getList();
                if (list.size() < 1) {
                    JSONObject noData = new JSONObject();
                    noData.put("code", 1);
                    noData.put("msg", "");
                    noData.put("data", new ArrayList());
                    noData.put("count", 0);
                    noData.put("total", total);
                    return noData.toString();
                }
                for (int i = 0; i < list.size(); i++) {
                    Map json = JSONObject.parseObject(JSONObject.toJSONString(list.get(i)), Map.class);
                    //加入自定义字段信息，这里取消注释即可开启，但是数据库查询会消耗性能
                    String cid = json.get("cid").toString();
                    Fields f = new Fields();
                    f.setCid(Integer.parseInt(cid));
                    List<Fields> fields = fieldsService.selectList(f);
                    json.put("fields", fields);

                    Relationships rs = new Relationships();
                    rs.setCid(Integer.parseInt(cid));
                    List<Relationships> relationships = relationshipsService.selectList(rs);

                    List metas = new ArrayList();
                    List tags = new ArrayList();
                    if (relationships.size() > 0) {
                        for (int j = 0; j < relationships.size(); j++) {
                            Map info = JSONObject.parseObject(JSONObject.toJSONString(relationships.get(j)), Map.class);
                            if (info != null) {
                                String mid = info.get("mid").toString();

                                Category metasList = metasService.selectByKey(mid);
                                if (metasList != null) {
                                    Map metasInfo = JSONObject.parseObject(JSONObject.toJSONString(metasList), Map.class);
                                    String type = metasInfo.get("type").toString();
                                    if (type.equals("category")) {
                                        metas.add(metasInfo);
                                    }
                                    if (type.equals("tag")) {
                                        tags.add(metasInfo);
                                    }
                                }

                            }

                        }
                    }

                    //写入作者详细信息
                    Integer authorId = Integer.parseInt(json.get("authorId").toString());
                    if (authorId > 0) {
                        Users author = usersService.selectByKey(authorId);
                        Map authorInfo = new HashMap();
                        if (author != null) {
                            String name = author.getName();
                            if (author.getScreenName() != null && author.getScreenName() != "") {
                                name = author.getScreenName();
                            }
                            String avatar = apiconfig.getWebinfoAvatar() + "null";
                            if (author.getAvatar() != null && author.getAvatar() != "") {
                                avatar = author.getAvatar();
                            } else {
                                if (author.getMail() != null && author.getMail() != "") {
                                    String mail = author.getMail();

                                    if (mail.indexOf("@qq.com") != -1) {
                                        String qq = mail.replace("@qq.com", "");
                                        avatar = "https://q1.qlogo.cn/g?b=qq&nk=" + qq + "&s=640";
                                    } else {
                                        avatar = baseFull.getAvatar(apiconfig.getWebinfoAvatar(), author.getMail());
                                    }
                                    //avatar = baseFull.getAvatar(apiconfig.getWebinfoAvatar(), author.getMail());
                                }
                            }

                            // 是否关注
                            Fan fan = new Fan();
                            fan.setUid(uid);
                            fan.setTouid(authorId);
                            Integer isfollow = fanService.total(fan);

                            JSONObject opt = JSONObject.parseObject(author.getOpt());
                            if (opt instanceof Object) {
                                opt = JSONObject.parseObject(author.getOpt());
                                Integer headId = Integer.parseInt(opt.get("head_picture").toString());
                                // 查询opt中head_picture的数据 并替换
                                Headpicture head_picture = headpictureService.selectByKey(headId);
                                if (head_picture != null) {
                                    opt.put("head_picture", head_picture.getLink().toString());
                                }

                            }
                            // 获取用户等级
                            List<Integer> levelAndExp = baseFull.getLevel(author.getExperience());
                            Integer level = levelAndExp.get(0);
                            Integer nextExp = levelAndExp.get(1);

                            authorInfo.put("name", name);
                            authorInfo.put("avatar", avatar);
                            authorInfo.put("customize", author.getCustomize());
                            authorInfo.put("opt", opt);
                            authorInfo.put("level", level);
                            authorInfo.put("nextExp", nextExp);
                            authorInfo.put("experience", author.getExperience());
                            authorInfo.put("isfollow", isfollow);
                            authorInfo.put("introduce", author.getIntroduce());
                            //判断是否为VIP
                            authorInfo.put("isvip", 0);
                            Long date = System.currentTimeMillis();
                            String curTime = String.valueOf(date).substring(0, 10);
                            Integer viptime = author.getVip();

                            if (viptime > Integer.parseInt(curTime) || viptime.equals(1)) {
                                authorInfo.put("isvip", 1);
                            }
                            if (viptime.equals(1)) {
                                //永久VIP
                                authorInfo.put("isvip", 2);
                            }
                        } else {
                            authorInfo.put("name", "用户已注销");
                            authorInfo.put("avatar", apiconfig.getWebinfoAvatar() + "null");
                        }


                        json.put("authorInfo", authorInfo);
                    }

                    String text = json.get("text").toString();
                    boolean status = text.contains("<!--markdown-->");
                    if (status) {
                        json.put("markdown", 1);
                    } else {
                        json.put("markdown", 0);
                    }
                    List imgList = baseFull.getImageSrc(text);

                    text = baseFull.toStrByChinese(text);

                    // 格式化文章opt
                    JSONObject opt = JSONObject.parseObject((String) json.get("opt"));
                    if (opt instanceof Object) {
                        opt = JSONObject.parseObject(json.get("opt").toString());
                    } else {
                        opt = null;
                    }

                    Object imagesObject = json.get("images");
                    // 判断值是否为 null
                    if (imagesObject != null) {
                        // 判断值的类型是否为 JSONArray
                        if (imagesObject instanceof JSONArray) {
                            // 如果是 JSONArray，直接使用
                            json.put("images", imagesObject);
                        } else {
                            // 如果不是 JSONArray，尝试将其解析为 JSONArray
                            JSONArray imagesArray = JSON.parseArray(imagesObject.toString());

                            // 判断解析结果是否为 null 或者为空
                            if (imagesArray == null || imagesArray.isEmpty()) {
                                // 如果为空，将其替换为 imgList
                                json.put("images", imgList);
                            } else {
                                // 如果不为空，直接使用解析的值
                                json.put("images", imagesArray);
                            }
                        }
                    } else {
                        // 如果值为 null，将其替换为 imgList
                        json.put("images", imgList);
                    }
                    json.put("text", text.length() > 400 ? text.substring(0, 400) : text);
                    json.put("category", metas);
                    json.put("tag", tags);
                    json.put("opt", opt);
                    //获取文章挂载的商品
                    Shop shop = new Shop();
                    shop.setCid(Integer.parseInt(cid));
                    shop.setStatus(1);
                    List<Shop> shopList = shopService.selectList(shop);
                    //去除付费内容显示
                    for (int s = 0; s < shopList.size(); s++) {
                        shopList.get(s).setValue(null);
                    }
                    json.put("shop", shopList);
                    json.remove("password");

                    jsonList.add(json);

                }
                redisHelp.delete(this.dataprefix + "_" + "contentsList_" + page + "_" + limit + "_" + sqlParams + "_" + order + "_" + searchKey + "_" + random, redisTemplate);
                redisHelp.setList(this.dataprefix + "_" + "contentsList_" + page + "_" + limit + "_" + sqlParams + "_" + order + "_" + searchKey + "_" + random, jsonList, this.contentCache, redisTemplate);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (cacheList.size() > 0) {
                jsonList = cacheList;
            }

        }

        JSONObject response = new JSONObject();
        response.put("code", 1);
        response.put("msg", "");
        response.put("data", null != jsonList ? jsonList : new JSONArray());
        response.put("count", jsonList.size());
        response.put("total", total);
        return response.toString();
    }

    /***
     * 发布文章
     * @param params Bean对象JSON字符串
     */
    @RequestMapping(value = "/articleAdd")
    @XssCleanIgnore
    @ResponseBody
    public String contentsAdd(@RequestParam(value = "params", required = false) String params,
                              @RequestParam(value = "token", required = false) String token,
                              @RequestParam(value = "text", required = false) String text,
                              @RequestParam(value = "mid", required = false) String mid,
                              @RequestParam(value = "isMd", required = false, defaultValue = "1") Integer isMd,
                              @RequestParam(value = "isSpace", required = false, defaultValue = "0") Integer isSpace,
                              @RequestParam(value = "isDraft", required = false, defaultValue = "0") Integer isDraft,
                              HttpServletRequest request) {
        try {
            Article insert = null;
            String ip = baseFull.getIpAddr(request);
            Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
            Map jsonToMap = new HashMap();
            String category = "";
            String tag = "";
            Integer sid = -1;
            if (uStatus == 0) {
                return Result.getResultJson(0, "用户未登录或Token验证失败", null);
            }
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            Integer logUid = Integer.parseInt(map.get("uid").toString());
            Apiconfig apiconfig = UStatus.getConfig(this.dataprefix, apiconfigService, redisTemplate);


            if (apiconfig.getBanRobots().equals(1)) {
                //登录情况下，刷数据攻击拦截
                String isSilence = redisHelp.getRedis(this.dataprefix + "_" + logUid + "_silence", redisTemplate);
                if (isSilence != null) {
                    return Result.getResultJson(0, "你已被禁言，请耐心等待", null);
                }
                String isRepeated = redisHelp.getRedis(this.dataprefix + "_" + logUid + "_isRepeated", redisTemplate);
                if (isRepeated == null) {
                    redisHelp.setRedis(this.dataprefix + "_" + logUid + "_isRepeated", "1", 3, redisTemplate);
                } else {
                    Integer frequency = Integer.parseInt(isRepeated) + 1;
                    if (frequency == 3) {
                        securityService.safetyMessage("用户ID：" + logUid + "，在文章发布接口疑似存在攻击行为，请及时确认处理。", "system");
                        redisHelp.setRedis(this.dataprefix + "_" + logUid + "_silence", "1", apiconfig.getSilenceTime(), redisTemplate);
                        return Result.getResultJson(0, "你的请求存在恶意行为，10分钟内禁止操作！", null);
                    } else {
                        redisHelp.setRedis(this.dataprefix + "_" + logUid + "_isRepeated", frequency.toString(), 3, redisTemplate);
                    }
                    return Result.getResultJson(0, "你的操作太频繁了", null);
                }
            }

            //攻击拦截结束


            Integer isWaiting = 0;
            if (StringUtils.isNotBlank(params)) {
                jsonToMap = JSONObject.parseObject(JSON.parseObject(params).toString());

                //支持两种模式提交文章内容
                if (text == null) {
                    text = jsonToMap.get("text").toString();
                }
                //获取发布者信息
                String uid = map.get("uid").toString();
                //判断是否开启邮箱验证

                Integer isEmail = apiconfig.getIsEmail();
                if (isEmail > 0) {
                    //判断用户是否绑定了邮箱
                    Users users = usersService.selectByKey(uid);
                    if (users.getMail() == null) {
                        return Result.getResultJson(0, "发布文章前，请先绑定邮箱", null);
                    }
                }
                //生成typecho数据库格式的创建时间戳
                Long date = System.currentTimeMillis();
                String userTime = String.valueOf(date).substring(0, 10);
                //获取商品id
                if (jsonToMap.get("sid") != null) {
                    sid = Integer.parseInt(jsonToMap.get("sid").toString());
                }


                //获取参数中的分类和标签
                if (jsonToMap.containsKey("category")) {
                    if (jsonToMap.get("category").toString().isEmpty()) {
                        category = "1";
                    } else {
                        category = jsonToMap.get("category").toString();
                    }
                } else {
                    return Result.getResultJson(0, "分类不可为空", null);
                }


                if (jsonToMap.get("tag") != null) {
                    tag = jsonToMap.get("tag").toString();
                }
                if (text.length() < 1) {
                    return Result.getResultJson(0, "文章内容不能为空", null);
                } else {
                    if (text.length() > 60000) {
                        return Result.getResultJson(0, "超出最大文章内容长度", null);
                    }

                    //是否开启代码拦截
                    if (apiconfig.getDisableCode().equals(1)) {
                        if (baseFull.haveCode(text).equals(1)) {
                            return Result.getResultJson(0, "你的内容包含敏感代码，请修改后重试！", null);
                        }
                    }
                    //满足typecho的要求，加入markdown申明
                    if (isMd.equals(1)) {
                        boolean status = text.contains("<!--markdown-->");
                        if (!status) {
                            text = "<!--markdown-->" + text;
                        }
                    }

                }
                if (isMd.equals(1)) {
                    text = text.replace("||rn||", "\n");
                }
                //写入创建时间和作者
                jsonToMap.put("created", userTime);
                jsonToMap.put("modified", userTime);
                jsonToMap.put("replyTime", userTime);
                jsonToMap.put("authorId", uid);


                Map userMap = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
                String group = userMap.get("group").toString();
                //普通用户最大发文限制
                if (!group.equals("administrator") && !group.equals("editor")) {
                    String postNum = redisHelp.getRedis(this.dataprefix + "_" + logUid + "_postNum", redisTemplate);
                    if (postNum == null) {
                        redisHelp.setRedis(this.dataprefix + "_" + logUid + "_postNum", "1", 86400, redisTemplate);
                    } else {
                        Integer post_Num = Integer.parseInt(postNum) + 1;
                        if (post_Num > apiconfig.getPostMax() && apiconfig.getPostMax() != -1) {
                            return Result.getResultJson(0, "你已超过最大发布数量限制，请您24小时后再操作", null);
                        } else {
                            redisHelp.setRedis(this.dataprefix + "_" + logUid + "_postNum", post_Num.toString(), 86400, redisTemplate);
                        }


                    }
                }
                //限制结束
                //标题强制验证违禁
                String forbidden = apiconfig.getForbidden();
                String title = jsonToMap.get("title").toString();
                Integer titleForbidden = baseFull.getForbidden(forbidden, title);
                if (titleForbidden.equals(1)) {
                    return Result.getResultJson(0, "标题存在违禁词", null);
                }
                //根据后台的开关判断是否需要审核
                if (isDraft.equals(0)) {
                    Integer contentAuditlevel = apiconfig.getContentAuditlevel();
                    if (contentAuditlevel.equals(0)) {
                        jsonToMap.put("status", "publish");
                    }
                    if (contentAuditlevel.equals(1)) {

                        if (!group.equals("administrator") && !group.equals("editor")) {
                            Integer isForbidden = baseFull.getForbidden(forbidden, text);
                            if (isForbidden.equals(0)) {
                                jsonToMap.put("status", "publish");
                            } else {
                                jsonToMap.put("status", "waiting");
                            }
                        } else {
                            jsonToMap.put("status", "publish");
                        }

                    }
                    if (contentAuditlevel.equals(2)) {
                        if (!group.equals("administrator") && !group.equals("editor")) {
                            jsonToMap.put("status", "waiting");
                        } else {
                            jsonToMap.put("status", "publish");
                        }
                    }


                } else {
                    jsonToMap.put("status", "publish");
                    jsonToMap.put("type", "post_draft");
                }

                jsonToMap.put("text", text);
                //部分字段不允许定义

                jsonToMap.put("commentsNum", 0);
                jsonToMap.put("allowPing", 1);
                jsonToMap.put("allowFeed", 1);
                jsonToMap.put("allowComment", 1);
                jsonToMap.put("orderKey", 0);
                jsonToMap.put("parent", 0);
                jsonToMap.remove("password");
                jsonToMap.remove("sid");
                jsonToMap.remove("isrecommend");
                insert = JSON.parseObject(JSON.toJSONString(jsonToMap), Article.class);

            }
            int rows = service.insert(insert);

            Integer cid = insert.getCid();
            //文章添加完成后，再处理分类和标签还有挂载商品，还有slug
            Article slugUpdate = new Article();
            slugUpdate.setSlug(cid.toString());
            slugUpdate.setCid(cid);
            service.update(slugUpdate);

            if (rows > 0) {
                if (category.length() > 0) {
                    if (category.contains(",")) {
                        // 如果包含逗号，进行拆分处理
                        String[] categoryList = category.split(",");
                        List list = Arrays.asList(baseFull.threeClear(categoryList));

                        for (int v = 0; v < list.size(); v++) {
                            Relationships toCategory = new Relationships();
                            String id = list.get(v).toString();
                            if (!id.equals("")) {
                                toCategory.setCid(cid);
                                toCategory.setMid(Integer.parseInt(id));
                                List<Relationships> cList = relationshipsService.selectList(toCategory);
                                if (cList.size() < 1) {
                                    relationshipsService.insert(toCategory);
                                }
                            }
                        }
                    } else {
                        // 如果不包含逗号，直接处理单一数据
                        Relationships toCategory = new Relationships();
                        toCategory.setCid(cid);
                        toCategory.setMid(Integer.parseInt(category));
                        List<Relationships> cList = relationshipsService.selectList(toCategory);
                        if (cList.size() < 1) {
                            relationshipsService.insert(toCategory);
                        }
                    }
                }
                if (!tag.isEmpty()) {
                    if (tag.contains(",")) {
                        String[] tagList = tag.split(",");
                        List list = Arrays.asList(baseFull.threeClear(tagList));
                        for (int v = 0; v < list.size(); v++) {
                            Relationships toTag = new Relationships();
                            String id = list.get(v).toString();
                            if (!id.equals("")) {
                                toTag.setCid(cid);
                                toTag.setMid(Integer.parseInt(id));
                                relationshipsService.insert(toTag);
                            }
                        }
                    } else {
                        Relationships toTag = new Relationships();
                        toTag.setCid(cid);
                        toTag.setMid(Integer.parseInt(tag));
                        relationshipsService.insert(toTag);
                    }
                }

            }
            Long date = System.currentTimeMillis();
            String created = String.valueOf(date).substring(0, 10);

            if (isDraft.equals(0)) {
                if (isSpace.equals(1)) {
                    //判断用户经验值
                    Integer spaceMinExp = apiconfig.getSpaceMinExp();
                    Users curUser = usersService.selectByKey(logUid);
                    Integer Exp = curUser.getExperience();
                    if (Exp < spaceMinExp) {
                        return Result.getResultJson(0, "发布动态最低要求经验值为" + spaceMinExp + ",你当前经验值" + Exp, null);
                    }
                    Space space = new Space();
                    space.setType(1);
                    space.setText("发布了新文章");
                    space.setCreated(Integer.parseInt(created));
                    space.setModified(Integer.parseInt(created));
                    space.setUid(logUid);
                    space.setToid(cid);
                    spaceService.insert(space);
                }
            }
            String resText = "发布成功";
            if (isWaiting > 0) {
                resText = "文章将在审核后发布！";

            } else {
                Users updateUser = new Users();
                updateUser.setUid(logUid);
                updateUser.setPosttime(Integer.parseInt(created));
                //如果无需审核，则立即增加经验
                Integer postExp = apiconfig.getPostExp();
                if (postExp > 0) {
                    //生成操作记录

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                    String curtime = sdf.format(new Date(date));
                    Userlog userlog = new Userlog();
                    userlog.setUid(logUid);
                    //cid用于存放真实时间
                    userlog.setCid(Integer.parseInt(curtime));
                    userlog.setType("postExp");
                    Integer size = userlogService.total(userlog);
                    //只有前三次发布文章获得经验
                    if (size < 3) {
                        userlog.setNum(postExp);
                        userlog.setCreated(Integer.parseInt(created));
                        userlogService.insert(userlog);
                        //修改用户资产
                        Users oldUser = usersService.selectByKey(logUid);
                        Integer experience = oldUser.getExperience();
                        experience = experience + postExp;
                        updateUser.setExperience(experience);


                    }
                }
                usersService.update(updateUser);

            }
            //添加付费阅读
            editFile.setLog("用户" + logUid + "请求发布了新文章");
            JSONObject response = new JSONObject();
            response.put("code", rows > 0 ? 1 : 0);
            response.put("data", rows);
            response.put("msg", rows > 0 ? resText : "发布失败");
            redisHelp.deleteKeysWithPattern("*" + this.dataprefix + "_contentsList_1*", redisTemplate);
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(0, "接口请求异常，请联系管理员", null);
        }
    }

    /***
     * 文章修改
     * @param params Bean对象JSON字符串
     */
    @RequestMapping(value = "/articleUpdate")
    @XssCleanIgnore
    @ResponseBody
    public String contentsUpdate(@RequestParam(value = "params", required = false) String params,
                                 @RequestParam(value = "token", required = false) String token,
                                 @RequestParam(value = "postStatus", required = false) String postStatus,
                                 @RequestParam(value = "isDraft", required = false, defaultValue = "0") Integer isDraft,
                                 @RequestParam(value = "mid", required = false, defaultValue = "1") Integer mid,
                                 @RequestParam(value = "price", required = false, defaultValue = "0") Integer price,
                                 @RequestParam(value = "discount", required = false, defaultValue = "1.0") String discount) {

        try {
            Article update = null;
            Map jsonToMap = null;
            Article info = new Article();
            String category = "";
            String tag = "";
            Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
            if (uStatus == 0) {
                return Result.getResultJson(0, "用户未登录或Token验证失败", null);
            }
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            Integer isWaiting = 0;
            Integer logUid = Integer.parseInt(map.get("uid").toString());
            if (StringUtils.isNotBlank(params)) {
                Apiconfig apiconfig = UStatus.getConfig(this.dataprefix, apiconfigService, redisTemplate);
                jsonToMap = JSONObject.parseObject(JSON.parseObject(params).toString());

                if (jsonToMap.containsKey("text")) {
                    if (jsonToMap.get("text").toString().isEmpty()) {
                        return Result.getResultJson(0, "文章内容不能为空", null);
                    } else {

                    }
                }

                //生成typecho数据库格式的修改时间戳
                Long date = System.currentTimeMillis();
                String userTime = String.valueOf(date).substring(0, 10);
                jsonToMap.put("modified", userTime);

                info = service.selectByKey(jsonToMap.get("cid").toString());
                if (info == null) {
                    return Result.getResultJson(0, "文章不存在", null);
                }
                //验证用户是否为作品的作者，以及权限
                Integer uid = Integer.parseInt(map.get("uid").toString());
                String group = map.get("group").toString();
                if (!group.equals("administrator") && !group.equals("editor")) {

                    Integer authorId = info.getAuthorId();
                    if (!uid.equals(authorId)) {
                        return Result.getResultJson(0, "你无权操作此文章", null);
                    }
                }


                //获取参数中的分类和标签（暂时不允许定义）
                if (jsonToMap.containsKey("category")) {
                    category = jsonToMap.get("category").toString();
                    if (category.isEmpty()) {
                        category = mid.toString();
                    }

                }

                if (jsonToMap.containsKey("tag")) {
                    if (!jsonToMap.get("tag").toString().isEmpty()) {
                        tag = jsonToMap.get("tag").toString();
                    }
                }

                if (jsonToMap.containsKey("text")) {
                    if (jsonToMap.get("text").toString().isEmpty()) {
                        return Result.getResultJson(0, "内容不可为空", null);
                    } else {
                        //满足typecho的要求，加入markdown申明
                        //是否开启代码拦截
                        if (apiconfig.getDisableCode().equals(1)) {
                            if (baseFull.haveCode(jsonToMap.get("text").toString()).equals(1)) {
                                return Result.getResultJson(0, "你的内容包含敏感代码，请修改后重试！", null);
                            }
                        }
                    }
                }


                jsonToMap.put("text", jsonToMap.get("text"));
                //部分字段不允许定义
                jsonToMap.remove("authorId");
                jsonToMap.remove("commentsNum");
                jsonToMap.remove("allowPing");
                jsonToMap.remove("allowFeed");
                jsonToMap.remove("password");
                jsonToMap.remove("orderKey");
                jsonToMap.remove("parent");
                jsonToMap.remove("created");
                jsonToMap.remove("slug");
                jsonToMap.remove("views");
                jsonToMap.remove("likes");
                jsonToMap.remove("sid");
                jsonToMap.remove("replyTime");
                if (!group.equals("administrator") && !group.equals("editor")) {
                    jsonToMap.remove("isrecommend");
                    jsonToMap.remove("istop");
                    jsonToMap.remove("isswiper");

                }
//                //状态重新变成待审核
//                if(!group.equals("administrator")){
//                    jsonToMap.put("status","waiting");
//                }
                //标题强制验证违禁
                String forbidden = apiconfig.getForbidden();
                String title = jsonToMap.get("title").toString();
                Integer titleForbidden = baseFull.getForbidden(forbidden, title);
                if (titleForbidden.equals(1)) {
                    return Result.getResultJson(0, "标题存在违禁词", null);
                }
                //根据后台的开关判断
                if (isDraft.equals(0)) {
                    Integer contentAuditlevel = apiconfig.getContentAuditlevel();
                    // 未开启则默认全部审核通过
                    if (contentAuditlevel.equals(0)) {
                        jsonToMap.put("status", "publish");
                    }
                    // 如果开启审核 管理员默认发布 否则待审
                    if (contentAuditlevel.equals(1)) {

                        if (!group.equals("administrator") && !group.equals("editor")) {
                            Integer isForbidden = baseFull.getForbidden(forbidden, jsonToMap.get("text").toString());
                            if (isForbidden.equals(0)) {
                                jsonToMap.put("status", "publish");
                            } else {
                                jsonToMap.put("status", "waiting");
                            }
                        } else {
                            jsonToMap.put("status", "publish");
                        }

                    }
                    //除管理员外，文章默认待审核
                    if (contentAuditlevel.equals(2)) {
                        if (!group.equals("administrator") && !group.equals("editor")) {
                            jsonToMap.put("status", "waiting");
                        } else {
                            jsonToMap.put("status", "publish");
                        }
                    }
                } else {
                    jsonToMap.put("status", "publish");
                    jsonToMap.put("type", "post_draft");
                }

                // 脱离上面判断 开始获取传参Status
                if (postStatus != null && !postStatus.isEmpty()) {
                    if (group.equals("administrator") || group.equals("editor")) {
                        // 如果用户组不是管理员或编辑者，根据传参设置状态为传入的postStatus
                        jsonToMap.put("status", postStatus);
                    }
                } else {
                    jsonToMap.put("status", "publish");
                }
                update = JSON.parseObject(JSON.toJSONString(jsonToMap), Article.class);
            }

            int rows = service.update(update);
            //处理标签和分类
            Integer cid = Integer.parseInt(jsonToMap.get("cid").toString());
            //删除原本的分类标签映射，反正都会更新，那就一起更新
            relationshipsService.delete(cid);

            //文章添加完成后，再处理分类和标签，只有文章能设置标签和分类
            if (rows > 0) {
                if (category.length() > 0) {
                    if (category.contains(",")) {
                        // 如果包含逗号，进行拆分处理
                        String[] categoryList = category.split(",");
                        List list = Arrays.asList(baseFull.threeClear(categoryList));

                        for (int v = 0; v < list.size(); v++) {
                            Relationships toCategory = new Relationships();
                            String id = list.get(v).toString();
                            if (!id.equals("")) {
                                toCategory.setCid(cid);
                                toCategory.setMid(Integer.parseInt(id));
                                List<Relationships> cList = relationshipsService.selectList(toCategory);
                                if (cList.size() < 1) {
                                    relationshipsService.insert(toCategory);
                                }
                            }
                        }
                    } else {
                        // 如果不包含逗号，直接处理单一数据
                        Relationships toCategory = new Relationships();
                        toCategory.setCid(cid);
                        toCategory.setMid(Integer.parseInt(category));
                        List<Relationships> cList = relationshipsService.selectList(toCategory);
                        if (cList.size() < 1) {
                            relationshipsService.insert(toCategory);
                        }
                    }
                }

                if (!tag.isEmpty()) {
                    if (tag.contains(",")) {
                        String[] tagList = tag.split(",");
                        List list = Arrays.asList(baseFull.threeClear(tagList));
                        for (int v = 0; v < list.size(); v++) {
                            Relationships toTag = new Relationships();
                            String id = list.get(v).toString();
                            if (!id.equals("")) {
                                toTag.setCid(cid);
                                toTag.setMid(Integer.parseInt(id));
                                relationshipsService.insert(toTag);
                            }
                        }
                    } else {
                        Relationships toTag = new Relationships();
                        toTag.setCid(cid);
                        toTag.setMid(Integer.parseInt(tag));
                        relationshipsService.insert(toTag);
                    }
                }
            }

            editFile.setLog("用户" + logUid + "请求修改了文章" + cid);
            String resText = "修改成功";
            if (isWaiting > 0) {
                resText = "文章将在审核后发布！";
            }
            //清除缓存
            redisHelp.deleteKeysWithPattern("*" + this.dataprefix + "_" + "contentsInfo_" + cid + "*", redisTemplate);
            redisHelp.deleteKeysWithPattern("*" + this.dataprefix + "_contentsList_1*", redisTemplate);
            JSONObject response = new JSONObject();
            response.put("code", rows > 0 ? 1 : 0);
            response.put("data", rows);
            response.put("msg", rows > 0 ? resText : "修改失败");
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(0, "接口请求异常，请联系管理员", null);
        }
    }

    /***
     * 文章删除
     */
    @RequestMapping(value = "/articleDelete")
    @ResponseBody
    public String formDelete(@RequestParam(value = "key", required = false) String key, @RequestParam(value = "token", required = false) String token) {
        try {
            Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
            if (uStatus == 0) {
                return Result.getResultJson(0, "用户未登录或Token验证失败", null);
            }
            //String group = (String) redisHelp.getValue("userInfo"+token,"group",redisTemplate);
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            Integer uid = Integer.parseInt(map.get("uid").toString());
            String group = map.get("group").toString();
            Article contents = service.selectByKey(key);
            Apiconfig apiconfig = UStatus.getConfig(this.dataprefix, apiconfigService, redisTemplate);
            if (!group.equals("administrator") && !group.equals("editor")) {

                if (apiconfig.getAllowDelete().equals(0)) {
                    return Result.getResultJson(0, "系统禁止删除文章", null);
                }

                Integer aid = contents.getAuthorId();
                if (!aid.equals(uid)) {
                    return Result.getResultJson(0, "你无权进行此操作", null);
                }
//                jsonToMap.put("status","0");
            }
            //发送消息
            Long date = System.currentTimeMillis();
            String created = String.valueOf(date).substring(0, 10);
            Inbox insert = new Inbox();
            insert.setUid(uid);
            insert.setTouid(contents.getAuthorId());
            insert.setType("system");
            insert.setText("你的文章【" + contents.getTitle() + "】已被删除");
            insert.setCreated(Integer.parseInt(created));
            inboxService.insert(insert);

            Integer logUid = Integer.parseInt(map.get("uid").toString());
            int rows = service.delete(key);
            //删除与分类的映射
            int st = relationshipsService.delete(key);

            //更新用户经验
            Integer deleteExp = apiconfig.getDeleteExp();
            if (deleteExp > 0) {
                Users oldUser = usersService.selectByKey(contents.getAuthorId());
                if (oldUser != null) {
                    Integer experience = oldUser.getExperience();
                    experience = experience - deleteExp;
                    Users updateUser = new Users();
                    updateUser.setUid(contents.getAuthorId());
                    updateUser.setExperience(experience);
                    usersService.update(updateUser);
                }


            }
            editFile.setLog("管理员" + logUid + "请求删除文章" + key);
            //删除列表redis
            redisHelp.deleteKeysWithPattern("*" + this.dataprefix + "_contentsList_1*", redisTemplate);
            JSONObject response = new JSONObject();
            response.put("code", rows > 0 ? 1 : 0);
            response.put("data", rows);
            response.put("msg", rows > 0 ? "操作成功" : "操作失败");
            return response.toString();
        } catch (Exception e) {
            return Result.getResultJson(0, "操作失败", null);
        }
    }

    /***
     * 文章审核
     */
    @RequestMapping(value = "/articleAudit")
    @ResponseBody
    public String contentsAudit(@RequestParam(value = "key", required = false) String key,
                                @RequestParam(value = "token", required = false) String token,
                                @RequestParam(value = "type", required = false, defaultValue = "0") Integer type,
                                @RequestParam(value = "reason", required = false) String reason) {
        try {
            if (type == null) {
                type = 0;
            }
            Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
            if (uStatus == 0) {
                return Result.getResultJson(0, "用户未登录或Token验证失败", null);
            }
            Apiconfig apiconfig = UStatus.getConfig(this.dataprefix, apiconfigService, redisTemplate);
            String newtitle = apiconfig.getWebinfoTitle();
            //String group = (String) redisHelp.getValue("userInfo"+token,"group",redisTemplate);
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            String group = map.get("group").toString();
            Integer logUid = Integer.parseInt(map.get("uid").toString());
            if (!group.equals("administrator") && !group.equals("editor")) {
                return Result.getResultJson(0, "你没有操作权限", null);
            }
            Article info = service.selectByKey(key);
            if (info.getStatus().equals("publish")) {
                return Result.getResultJson(0, "该文章已审核通过", null);
            }
            Integer cUid = info.getAuthorId();
            info.setCid(Integer.parseInt(key));
            //0为审核通过，1为不通过，并发送消息
            if (type.equals(0)) {
                info.setStatus("publish");
            } else {
                if (reason == "" || reason == null) {
                    return Result.getResultJson(0, "请输入拒绝理由", null);
                }
                info.setStatus("reject");
            }
            Integer rows = service.update(info);
            //给作者发送邮件
            Users ainfo = usersService.selectByKey(info.getAuthorId());
            String title = info.getTitle();
            Integer uid = ainfo.getUid();
            //根据过审状态发送不同的内容
            if (type.equals(0)) {
                if (apiconfig.getIsEmail().equals(2)) {
                    if (ainfo.getMail() != null) {
                        String email = ainfo.getMail();
                        try {
                            MailService.send("用户：" + uid + ",您的文章已审核通过", "<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" /><title></title><meta charset=\"utf-8\" /><style>*{padding:0px;margin:0px;box-sizing:border-box;}html{box-sizing:border-box;}body{font-size:15px;background:#fff}.main{margin:20px auto;max-width:500px;border:solid 1px #2299dd;overflow:hidden;}.main h1{display:block;width:100%;background:#2299dd;font-size:18px;color:#fff;text-align:center;padding:15px;}.text{padding:30px;}.text p{margin:10px 0px;line-height:25px;}.text p span{color:#2299dd;font-weight:bold;font-size:22px;margin-left:5px;}</style></head>" +
                                            "<body><div class=\"main\"><h1>文章审核</h1><div class=\"text\"><p>用户 " + uid + "，你的文章<" + title + ">已经审核通过！</p>" +
                                            "<p>可前往<a href=\"" + apiconfig.getWebinfoUrl() + "\">" + newtitle + "</a>查看详情</p></div></div></body></html>",
                                    new String[]{email}, new String[]{});
                        } catch (Exception e) {
                            System.err.println("邮箱发信配置错误：" + e);
                        }


                    }
                }

                //发送消息
                Long date = System.currentTimeMillis();
                String created = String.valueOf(date).substring(0, 10);
                Inbox insert = new Inbox();
                insert.setUid(logUid);
                insert.setTouid(info.getAuthorId());
                insert.setType("system");
                insert.setText("你的文章【" + info.getTitle() + "】已审核通过");
                insert.setCreated(Integer.parseInt(created));
                inboxService.insert(insert);
            } else {
                if (apiconfig.getIsEmail().equals(2)) {
                    if (ainfo.getMail() != null) {
                        String email = ainfo.getMail();
                        try {
                            MailService.send("用户：" + uid + ",您的文章未审核通过", "<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" /><title></title><meta charset=\"utf-8\" /><style>*{padding:0px;margin:0px;box-sizing:border-box;}html{box-sizing:border-box;}body{font-size:15px;background:#fff}.main{margin:20px auto;max-width:500px;border:solid 1px #2299dd;overflow:hidden;}.main h1{display:block;width:100%;background:#2299dd;font-size:18px;color:#fff;text-align:center;padding:15px;}.text{padding:30px;}.text p{margin:10px 0px;line-height:25px;}.text p span{color:#2299dd;font-weight:bold;font-size:22px;margin-left:5px;}</style></head>" +
                                            "<body><div class=\"main\"><h1>文章审核</h1><div class=\"text\"><p>用户 " + uid + "，你的文章<" + title + ">未审核通过！理由如下：" + reason + "</p>" +
                                            "<p>可前往<a href=\"" + apiconfig.getWebinfoUrl() + "\">" + newtitle + "</a>查看详情</p></div></div></body></html>",
                                    new String[]{email}, new String[]{});
                        } catch (Exception e) {
                            System.err.println("邮箱发信配置错误：" + e);
                        }
                    }
                }
                //发送消息
                Long date = System.currentTimeMillis();
                String created = String.valueOf(date).substring(0, 10);
                Inbox insert = new Inbox();
                insert.setUid(logUid);
                insert.setTouid(info.getAuthorId());
                insert.setType("system");
                insert.setText("你的文章【" + info.getTitle() + "】未审核通过。理由如下：" + reason);
                insert.setCreated(Integer.parseInt(created));
                inboxService.insert(insert);
            }
            try {
                if (type.equals(0)) {
                    //审核后增加经验
                    Integer postExp = apiconfig.getPostExp();
                    if (postExp > 0) {
                        //生成操作记录
                        Long date = System.currentTimeMillis();
                        String created = String.valueOf(date).substring(0, 10);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                        String curtime = sdf.format(new Date(date));

                        Userlog userlog = new Userlog();
                        userlog.setUid(cUid);
                        //cid用于存放真实时间
                        userlog.setCid(Integer.parseInt(curtime));
                        userlog.setType("postExp");
                        Integer size = userlogService.total(userlog);
                        //只有前三次发布文章获得经验
                        if (size < 3) {
                            userlog.setNum(postExp);
                            userlog.setCreated(Integer.parseInt(created));
                            userlogService.insert(userlog);
                            //修改用户资产
                            Users oldUser = usersService.selectByKey(cUid);
                            Integer experience = oldUser.getExperience();
                            experience = experience + postExp;
                            Users updateUser = new Users();
                            updateUser.setUid(cUid);
                            updateUser.setExperience(experience);
                            usersService.update(updateUser);
                        }
                    }

                }

            } catch (Exception e) {
                System.out.println("经验增加出错！");
                e.printStackTrace();
            }


            editFile.setLog("管理员" + logUid + "请求审核文章" + key);
            //删除列表redis
            redisHelp.deleteKeysWithPattern("*" + this.dataprefix + "_contentsList_1*", redisTemplate);
            JSONObject response = new JSONObject();
            response.put("code", rows > 0 ? 1 : 0);
            response.put("data", rows);
            response.put("msg", rows > 0 ? "操作成功，缓存缘故，数据可能存在延迟" : "操作失败");
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(0, "接口请求异常，请联系管理员", null);
        }
    }

    /***
     * 文章推荐&加精
     */
    @RequestMapping(value = "/articleRecommend")
    @ResponseBody
    public String addRecommend(@RequestParam(value = "key", required = false) String key, @RequestParam(value = "recommend", required = false) Integer recommend, @RequestParam(value = "token", required = false) String token) {
        try {
            Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
            if (uStatus == 0) {
                return Result.getResultJson(0, "用户未登录或Token验证失败", null);
            }

            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            String group = map.get("group").toString();
            if (!group.equals("administrator") && !group.equals("editor")) {
                return Result.getResultJson(0, "你没有操作权限", null);
            }
            ;
            Integer logUid = Integer.parseInt(map.get("uid").toString());
            Article info = service.selectByKey(key);
            Long date = System.currentTimeMillis();
            String time = String.valueOf(date).substring(0, 10);
            Integer modified = Integer.parseInt(time);
            info.setModified(modified);
            info.setCid(Integer.parseInt(key));
            info.setIsrecommend(recommend);
            Integer rows = service.update(info);
            editFile.setLog("管理员" + logUid + "请求推荐文章" + key);
            //删除列表redis
            redisHelp.deleteKeysWithPattern("*" + this.dataprefix + "_contentsList_1*", redisTemplate);
            JSONObject response = new JSONObject();
            response.put("code", rows > 0 ? 1 : 0);
            response.put("data", rows);
            response.put("msg", rows > 0 ? "操作成功" : "操作失败");
            return response.toString();
        } catch (Exception e) {
            return Result.getResultJson(0, "操作失败", null);
        }
    }

    /***
     * 文章置顶
     */
    @RequestMapping(value = "/articleTop")
    @ResponseBody
    public String addTop(@RequestParam(value = "key", required = false) String key, @RequestParam(value = "istop", required = false) Integer istop, @RequestParam(value = "token", required = false) String token) {
        try {
            Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
            if (uStatus == 0) {
                return Result.getResultJson(0, "用户未登录或Token验证失败", null);
            }

            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            String group = map.get("group").toString();
            if (!group.equals("administrator") && !group.equals("editor")) {
                return Result.getResultJson(0, "你没有操作权限", null);
            }
            Integer logUid = Integer.parseInt(map.get("uid").toString());
            Article info = service.selectByKey(key);
            //生成typecho数据库格式的修改时间戳
            Long date = System.currentTimeMillis();
            String time = String.valueOf(date).substring(0, 10);
            Integer modified = Integer.parseInt(time);
            info.setModified(modified);
            info.setCid(Integer.parseInt(key));
            info.setIstop(istop);
            Integer rows = service.update(info);
            editFile.setLog("管理员" + logUid + "请求置顶文章" + key);
            //删除列表redis
            redisHelp.deleteKeysWithPattern("*" + this.dataprefix + "_contentsList_1*", redisTemplate);
            JSONObject response = new JSONObject();
            response.put("code", rows > 0 ? 1 : 0);
            response.put("data", rows);
            response.put("msg", rows > 0 ? "操作成功" : "操作失败");
            return response.toString();
        } catch (Exception e) {
            return Result.getResultJson(0, "操作失败", null);
        }
    }

    /***
     * 文章轮播
     */
    @RequestMapping(value = "/articleSwiper")
    @ResponseBody
    public String addSwiper(@RequestParam(value = "key", required = false) String key, @RequestParam(value = "isswiper", required = false) Integer isswiper, @RequestParam(value = "token", required = false) String token) {
        try {
            Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
            if (uStatus == 0) {
                return Result.getResultJson(0, "用户未登录或Token验证失败", null);
            }

            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            String group = map.get("group").toString();
            if (!group.equals("administrator") && !group.equals("editor")) {
                return Result.getResultJson(0, "你没有操作权限", null);
            }
            Integer logUid = Integer.parseInt(map.get("uid").toString());
            Article info = service.selectByKey(key);
            //生成typecho数据库格式的修改时间戳
            Long date = System.currentTimeMillis();
            String time = String.valueOf(date).substring(0, 10);
            Integer modified = Integer.parseInt(time);
            info.setModified(modified);
            info.setCid(Integer.parseInt(key));
            info.setIsswiper(isswiper);
            Integer rows = service.update(info);
            editFile.setLog("管理员" + logUid + "请求轮播文章" + key);
            //删除列表redis
            redisHelp.deleteKeysWithPattern("*" + this.dataprefix + "_contentsList_1*", redisTemplate);
            JSONObject response = new JSONObject();
            response.put("code", rows > 0 ? 1 : 0);
            response.put("data", rows);
            response.put("msg", rows > 0 ? "操作成功" : "操作失败");
            return response.toString();
        } catch (Exception e) {
            return Result.getResultJson(0, "操作失败", null);
        }
    }

    /**
     * 购买文章隐藏内容
     */
    @RequestMapping(value = "/buyHide")
    @ResponseBody
    public String buyHide(@RequestParam(value = "token", required = true) String token,
                          @RequestParam(value = "cid", required = true) Integer cid) {
        try {
            Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
            Apiconfig apiconfig = UStatus.getConfig(this.dataprefix, apiconfigService, redisTemplate);
            if (uStatus == 0) {
                return Result.getResultJson(0, "用户未登录或Token验证失败", null);
            } else {

                // 获取文章信息中的价格
                Article articleInfo = service.selectByKey(cid);
                String price = "0";
                if (articleInfo.getPrice() > 0) {
                    price = String.valueOf(-articleInfo.getPrice());
                }

                // 获取全站配置中的会员折扣
                // 获取用户信息
                Map userInfo = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
                Users user = usersService.selectByKey(userInfo.get("uid"));
                Integer points = user.getAssets();
                if (points < articleInfo.getPrice()) {
                    return Result.getResultJson(1, "积分不足", null);
                }

                // 查询是否已经购买过
                Paylog buyStatus = new Paylog();
                buyStatus.setUid(Integer.parseInt(userInfo.get("uid").toString()));
                buyStatus.setCid(cid);
                Integer isBuy = paylogService.total(buyStatus);
                if (isBuy > 0) {
                    return Result.getResultJson(1, "无需重复购买", null);
                }

                // 判断是否是VIP然后执行折扣
                Long date = System.currentTimeMillis();
                String curTime = String.valueOf(date).substring(0, 10);
                Integer viptime = user.getVip();
                Boolean isVip = false;
                if (viptime > Integer.parseInt(curTime) || viptime.equals(1)) {
                    if (articleInfo.getPrice() > 0) {
                        // 判断折扣更低
                        price = "-" + ((int) Math.floor(articleInfo.getPrice() * articleInfo.getDiscount() > Float.valueOf(apiconfig.getVipDiscount()) ? Float.valueOf(apiconfig.getVipDiscount()) : articleInfo.getDiscount()));
                    }
                    isVip = true;
                } else {
                    price = "-" + (articleInfo.getPrice() * articleInfo.getDiscount());
                }
                // 获取日期 生成订单
                Long timestamp = System.currentTimeMillis();
                // 获取当前日期和时间
                LocalDateTime currentDateTime = LocalDateTime.now();
                // 定义日期时间格式
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                // 格式化日期时间
                String formattedDate = currentDateTime.format(dateFormatter);
                // 写入购买日志
                Paylog paylog = new Paylog();
                paylog.setOutTradeNo(formattedDate + timestamp);
                paylog.setCid(cid);
                paylog.setTotalAmount(price);
                paylog.setStatus(1);
                paylog.setPaytype("article");
                paylog.setCreated(Integer.parseInt(timestamp.toString().substring(0, 10)));
                paylog.setUid(Integer.parseInt(userInfo.get("uid").toString()));
                paylog.setSubject("查看文章【" + articleInfo.getTitle() + "】");
                Integer code = paylogService.insert(paylog);
                if (code > 0) {
                    if (isVip) {
                        user.setAssets((int) Math.floor(points - (articleInfo.getPrice() * articleInfo.getPrice() * articleInfo.getDiscount() > Float.valueOf(apiconfig.getVipDiscount()) ? Float.valueOf(apiconfig.getVipDiscount()) : articleInfo.getDiscount())));
                    } else {
                        user.setAssets((int) Math.floor(points - articleInfo.getPrice() * Float.valueOf(articleInfo.getDiscount())));
                    }
                    usersService.update(user);
                    // 完成之后给 文章作者加米
                    Users author = usersService.selectByKey(articleInfo.getAuthorId());
                    Integer authorPoints = author.getAssets();
                    authorPoints += (int) Math.floor(articleInfo.getPrice() * articleInfo.getDiscount());
                    author.setAssets(authorPoints);
                    usersService.update(author);
                    return Result.getResultJson(code, "购买成功", null);

                }
                return Result.getResultJson(0, "接口错误", null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(0, "接口错误", null);
        }

    }




    /***
     * 文章打赏者列表
     */
    @RequestMapping(value = "/rewardList")
    @ResponseBody
    public String rewardList(@RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                             @RequestParam(value = "limit", required = false, defaultValue = "15") Integer limit,
                             @RequestParam(value = "id", required = false) Integer id) {
        if (limit > 50) {
            limit = 50;
        }
        Integer total = 0;

        Userlog query = new Userlog();
        query.setCid(id);
        query.setType("reward");
        total = userlogService.total(query);

        List jsonList = new ArrayList();
        List cacheList = redisHelp.getList(this.dataprefix + "_" + "rewardList_" + page + "_" + limit, redisTemplate);
        try {
            if (cacheList.size() > 0) {
                jsonList = cacheList;
            } else {
                PageList<Userlog> pageList = userlogService.selectPage(query, page, limit);
                List<Userlog> list = pageList.getList();
                if (list.size() < 1) {
                    JSONObject noData = new JSONObject();
                    noData.put("code", 1);
                    noData.put("msg", "");
                    noData.put("data", new ArrayList());
                    noData.put("count", 0);
                    noData.put("total", total);
                    return noData.toString();
                }
                for (int i = 0; i < list.size(); i++) {
                    Integer userid = list.get(i).getUid();
                    Map json = JSONObject.parseObject(JSONObject.toJSONString(list.get(i)), Map.class);
                    //获取用户信息
                    Map userJson = UserStatus.getUserInfo(userid, apiconfigService, usersService);
                    //获取用户等级
                    Comments comments = new Comments();
                    comments.setAuthorId(userid);
                    Integer lv = commentsService.total(comments, null);
                    userJson.put("lv", baseFull.getLv(lv));
                    json.put("userJson", userJson);
                    jsonList.add(json);
                }
                redisHelp.delete(this.dataprefix + "_" + "rewardList_" + page + "_" + limit, redisTemplate);
                redisHelp.setList(this.dataprefix + "_" + "rewardList_" + page + "_" + limit, jsonList, 5, redisTemplate);
            }
        } catch (Exception e) {
            if (cacheList.size() > 0) {
                jsonList = cacheList;
            }
        }
        JSONObject response = new JSONObject();
        response.put("code", 1);
        response.put("msg", "");
        response.put("data", null != jsonList ? jsonList : new JSONArray());
        response.put("count", jsonList.size());
        response.put("total", total);
        return response.toString();

    }

    /***
     * 注册系统配置信息
     */
    @RequestMapping(value = "/contentConfig")
    @ResponseBody
    public String contentConfig() {
        Map contentConfig = new HashMap<String, String>();
        try {
            Map cacheInfo = redisHelp.getMapValue(this.dataprefix + "_contentConfig", redisTemplate);

            if (cacheInfo.size() > 0) {
                contentConfig = cacheInfo;
            } else {
                Apiconfig apiconfig = UStatus.getConfig(this.dataprefix, apiconfigService, redisTemplate);
                contentConfig.put("allowDelete", apiconfig.getAllowDelete());
                redisHelp.delete(this.dataprefix + "_contentConfig", redisTemplate);
                redisHelp.setKey(this.dataprefix + "_contentConfig", contentConfig, 5, redisTemplate);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject response = new JSONObject();
        response.put("code", 1);
        response.put("data", contentConfig);
        response.put("msg", "");
        return response.toString();
    }

    /***
     * 全站统计
     */
    @RequestMapping(value = "/allData")
    @ResponseBody
    public String allData(@RequestParam(value = "token", required = false) String token) {
        Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
        if (uStatus == 0) {
            return Result.getResultJson(0, "用户未登录或Token验证失败", null);
        }
        //String group = (String) redisHelp.getValue("userInfo"+token,"group",redisTemplate);
        Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
        String group = map.get("group").toString();
        if (!group.equals("administrator") && !group.equals("editor")) {
            return Result.getResultJson(0, "你没有操作权限", null);
        }
        JSONObject data = new JSONObject();

        Article contents = new Article();
        contents.setType("post");
        contents.setStatus("publish");
        Integer allContents = service.total(contents, null);

        Comments comments = new Comments();
        Integer allComments = commentsService.total(comments, null);

        Users users = new Users();
        Integer allUsers = usersService.total(users, null);


        Shop shop = new Shop();
        Integer allShop = shopService.total(shop, null);

        Space space = new Space();
        Integer allSpace = spaceService.total(space, null);

        Ads ads = new Ads();
        Integer allAds = adsService.total(ads);


        contents.setType("post");
        contents.setStatus("waiting");
        Integer upcomingContents = service.total(contents, null);

        comments.setStatus("waiting");
        Integer upcomingComments = commentsService.total(comments, null);

        shop.setStatus(0);
        Integer upcomingShop = shopService.total(shop, null);

        space.setStatus(0);
        Integer upcomingSpace = spaceService.total(space, null);


        ads.setStatus(0);
        Integer upcomingAds = adsService.total(ads);

        Userlog userlog = new Userlog();
        userlog.setType("withdraw");
        userlog.setCid(-1);
        Integer upcomingWithdraw = userlogService.total(userlog);


        data.put("allContents", allContents);
        data.put("allComments", allComments);
        data.put("allUsers", allUsers);
        data.put("allShop", allShop);
        data.put("allSpace", allSpace);
        data.put("allAds", allAds);

        data.put("upcomingContents", upcomingContents);
        data.put("upcomingComments", upcomingComments);
        data.put("upcomingShop", upcomingShop);
        data.put("upcomingSpace", upcomingSpace);
        data.put("upcomingAds", upcomingAds);
        data.put("upcomingWithdraw", upcomingWithdraw);

        JSONObject response = new JSONObject();
        response.put("code", 1);
        response.put("msg", "");
        response.put("data", data);

        return response.toString();
    }

    /***
     * 我关注的人的文章
     */
    @RequestMapping(value = "/followContents")
    @ResponseBody
    public String followSpace(@RequestParam(value = "token", required = false) String token,
                              @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                              @RequestParam(value = "limit", required = false, defaultValue = "15") Integer limit) {
        Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
        if (uStatus == 0) {
            return Result.getResultJson(0, "用户未登录或Token验证失败", null);
        }
        page = page - 1;

        Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
        Integer uid = Integer.parseInt(map.get("uid").toString());
        List jsonList = new ArrayList();
        List cacheList = redisHelp.getList(this.dataprefix + "_" + "followContents_" + uid + "_" + page + "_" + limit, redisTemplate);
        try {
            if (cacheList.size() > 0) {
                jsonList = cacheList;
            } else {
                Apiconfig apiconfig = UStatus.getConfig(this.dataprefix, apiconfigService, redisTemplate);
                String sql = "SELECT content.* FROM " + prefix + "_contents AS content JOIN " + prefix + "_fan AS fan ON content.authorId = fan.touid WHERE fan.uid = ? AND content.status = 'publish' ORDER BY content.created DESC LIMIT ?, ?";
                List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, uid, page, limit);
                if (list.size() < 1) {
                    JSONObject noData = new JSONObject();
                    noData.put("code", 1);
                    noData.put("msg", "");
                    noData.put("data", new ArrayList());
                    noData.put("count", 0);
                    return noData.toString();
                }
                for (int i = 0; i < list.size(); i++) {
                    Map json = JSONObject.parseObject(JSONObject.toJSONString(list.get(i)), Map.class);
                    //加入自定义字段信息，这里取消注释即可开启，但是数据库查询会消耗性能
                    String cid = json.get("cid").toString();
                    Fields f = new Fields();
                    f.setCid(Integer.parseInt(cid));
                    List<Fields> fields = fieldsService.selectList(f);
                    json.put("fields", fields);

                    Relationships rs = new Relationships();
                    rs.setCid(Integer.parseInt(cid));
                    List<Relationships> relationships = relationshipsService.selectList(rs);

                    List metas = new ArrayList();
                    List tags = new ArrayList();
                    if (relationships.size() > 0) {
                        for (int j = 0; j < relationships.size(); j++) {
                            Map info = JSONObject.parseObject(JSONObject.toJSONString(relationships.get(j)), Map.class);
                            if (info != null) {
                                String mid = info.get("mid").toString();

                                Category metasList = metasService.selectByKey(mid);
                                if (metasList != null) {
                                    Map metasInfo = JSONObject.parseObject(JSONObject.toJSONString(metasList), Map.class);
                                    String type = metasInfo.get("type").toString();
                                    if (type.equals("category")) {
                                        metas.add(metasInfo);
                                    }
                                    if (type.equals("tag")) {
                                        tags.add(metasInfo);
                                    }
                                }

                            }

                        }
                    }

                    //写入作者详细信息
                    Integer authorId = Integer.parseInt(json.get("authorId").toString());
                    if (uid > 0) {
                        Users author = usersService.selectByKey(authorId);
                        Map authorInfo = new HashMap();
                        if (author != null) {
                            String name = author.getName();
                            if (author.getScreenName() != null && author.getScreenName() != "") {
                                name = author.getScreenName();
                            }
                            String avatar = apiconfig.getWebinfoAvatar() + "null";
                            if (author.getAvatar() != null && author.getAvatar() != "") {
                                avatar = author.getAvatar();
                            } else {
                                if (author.getMail() != null && author.getMail() != "") {
                                    String mail = author.getMail();

                                    if (mail.indexOf("@qq.com") != -1) {
                                        String qq = mail.replace("@qq.com", "");
                                        avatar = "https://q1.qlogo.cn/g?b=qq&nk=" + qq + "&s=640";
                                    } else {
                                        avatar = baseFull.getAvatar(apiconfig.getWebinfoAvatar(), author.getMail());
                                    }
                                    //avatar = baseFull.getAvatar(apiconfig.getWebinfoAvatar(), author.getMail());
                                }
                            }

                            authorInfo.put("name", name);
                            authorInfo.put("avatar", avatar);
                            authorInfo.put("customize", author.getCustomize());
                            authorInfo.put("experience", author.getExperience());
                            //判断是否为VIP
                            authorInfo.put("isvip", 0);
                            Long date = System.currentTimeMillis();
                            String curTime = String.valueOf(date).substring(0, 10);
                            Integer viptime = author.getVip();

                            if (viptime > Integer.parseInt(curTime) || viptime.equals(1)) {
                                authorInfo.put("isvip", 1);
                            }
                            if (viptime.equals(1)) {
                                //永久VIP
                                authorInfo.put("isvip", 2);
                            }
                        } else {
                            authorInfo.put("name", "用户已注销");
                            authorInfo.put("avatar", apiconfig.getWebinfoAvatar() + "null");
                        }


                        json.put("authorInfo", authorInfo);
                    }

                    String text = json.get("text").toString();
                    boolean status = text.contains("<!--markdown-->");
                    if (status) {
                        json.put("markdown", 1);
                    } else {
                        json.put("markdown", 0);
                    }
                    List imgList = baseFull.getImageSrc(text);

                    text = baseFull.toStrByChinese(text);

                    json.put("images", imgList);
                    json.put("text", text.length() > 400 ? text.substring(0, 400) : text);
                    json.put("category", metas);
                    json.put("tag", tags);
                    //获取文章挂载的商品
                    Shop shop = new Shop();
                    shop.setCid(Integer.parseInt(cid));
                    shop.setStatus(1);
                    List<Shop> shopList = shopService.selectList(shop);
                    //去除付费内容显示
                    for (int s = 0; s < shopList.size(); s++) {
                        shopList.get(s).setValue(null);
                    }
                    json.put("shop", shopList);
                    json.remove("password");

                    jsonList.add(json);

                }
                redisHelp.delete(this.dataprefix + "_" + "followContents_" + uid + "_" + page + "_" + limit, redisTemplate);
                redisHelp.setList(this.dataprefix + "_" + "followContents_" + uid + "_" + page + "_" + limit, jsonList, 5, redisTemplate);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (cacheList.size() > 0) {
                jsonList = cacheList;
            }
        }
        JSONObject response = new JSONObject();
        response.put("code", 1);
        response.put("msg", "");
        response.put("data", jsonList);
        response.put("count", jsonList.size());
        return response.toString();

    }
}