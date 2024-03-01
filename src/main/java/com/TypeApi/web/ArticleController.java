package com.TypeApi.web;

import com.TypeApi.common.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.TypeApi.entity.*;
import com.TypeApi.service.*;
import com.auth0.jwt.interfaces.DecodedJWT;
import net.dreamlu.mica.xss.core.XssCleanIgnore;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private ArticleService articleService;

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

    /***
     * 文章详情
     */
    @RequestMapping(value = "/info")
    @ResponseBody
    public String info(@RequestParam(value = "id") Integer id,
                       HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Boolean permission = false;
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user.getGroup().equals("administrator") || user.getGroup().equals("editor")) permission = true;
            }
            // 查询文章
            Article article = service.selectByKey(id);
            Integer views = article.getViews() + 1;
            article.setViews(views);
            service.update(article);
            //格式化数据
            JSONObject opt = new JSONObject();
            opt = article.getOpt() != null && !article.getOpt().isEmpty() ? JSONObject.parseObject(article.getOpt()) : null;
            // 删除<!--markdown-->
            String text = article.getText().replace("<!--markdown-->", "");
            // 取出内容中的图片
            List<String> images = baseFull.getImageSrc(text);

            // 用正则表达式匹配并替换[hide type=pay]这是付费查看的内容[/hide]，并根据type值替换成相应的提示
            Boolean isReply = false;
            Boolean isPaid = false;

            Userlog userlog = new Userlog();
            Integer isLike = 0;
            Integer isMark = 0;
            Integer isHide = 1;
            if (user != null && !user.toString().isEmpty()) {
                // 获取评论状态
                Comments replyStatus = new Comments();
                replyStatus.setCid(article.getCid());
                replyStatus.setUid(user.getUid());
                Integer rStatus = commentsService.total(replyStatus, null);
                if (rStatus > 0) {
                    isReply = true;
                }
                // 获取购买状态
                Paylog paylog = new Paylog();
                paylog.setPaytype("article");
                paylog.setUid(user.getUid());
                paylog.setCid(article.getCid());
                Integer pStatus = paylogService.total(paylog);
                if (pStatus > 0) {
                    isPaid = true;
                }

                // 是否点赞或者是否收藏
                userlog.setType("articleLike");
                userlog.setCid(article.getCid());
                userlog.setUid(user.getUid());
                List<Userlog> userlogList = userlogService.selectList(userlog);
                if (userlogList.size() > 0) isLike = 1;
                userlog.setType("articleMark");
                userlogList = userlogService.selectList(userlog);
                if (userlogList.size() > 0) isMark = 1;
            }


            Pattern pattern = Pattern.compile("\\[hide type=(pay|reply)\\](.*?)\\[/hide\\]");
            Matcher matcher = pattern.matcher(text);
            StringBuffer replacedText = new StringBuffer();
            while (matcher.find()) {
                String type = matcher.group(1);
                String content = matcher.group(2);
                String replacement = "";
                if (type.equals("pay") && !isPaid && user.getUid() != article.getAuthorId() && !permission) {
                    replacement = "【付费查看：这是付费内容，付费后可查看】";
                } else if (type.equals("reply") && !isReply && user.getUid() != article.getAuthorId() && !permission) {
                    replacement = "【回复查看：这是回复内容，回复后可查看】";
                } else {
                    replacement = content;  // 如果不需要替换，则保持原样
                }
                matcher.appendReplacement(replacedText, replacement);
            }
            text = matcher.appendTail(replacedText).toString();

            // 获取分类和tag
            Category category = metasService.selectByKey(article.getMid());
            Map<String, Object> cateMap = JSONObject.parseObject(JSONObject.toJSONString(category), Map.class);
            JSONObject cateOpt = new JSONObject();
            if (category != null && category.getOpt() != null && category.getOpt().toString() != null && !category.getOpt().toString().isEmpty()) {
                cateOpt = JSONObject.parseObject(category.getOpt().toString());
                cateMap.put("opt", cateOpt);
            }
            // 标签
            Relationships tagQuery = new Relationships();
            tagQuery.setCid(article.getCid());
            List<Relationships> tagList = relationshipsService.selectList(tagQuery);
            JSONArray tagDataList = new JSONArray();
            for (Relationships tag : tagList) {
                Category tagsQuery = new Category();
                tagsQuery.setMid(tag.getMid());
                tagsQuery.setType("tag");
                List<Category> tagInfo = metasService.selectList(tagsQuery);
                if (tagInfo.size() > 0) {
                    Map<String, Object> tagData = JSONObject.parseObject(JSONObject.toJSONString(tagInfo.get(0)), Map.class);
                    // 移除信息
                    tagData.remove("opt");
                    tagDataList.add(tagData);
                }

            }

            // 加入作者信息
            Users info = usersService.selectByKey(article.getAuthorId());
            Map<String, Object> authorInfo;
            if (info == null || info.toString().isEmpty()) {
                authorInfo = new HashMap<>();
            } else {
                authorInfo = JSONObject.parseObject(JSONObject.toJSONString(info), Map.class);
            }
            if (info != null && !info.toString().isEmpty()) {
                List result = baseFull.getLevel(info.getExperience(), dataprefix, apiconfigService, redisTemplate);
                Integer level = (Integer) result.get(0);
                Integer nextLevel = (Integer) result.get(1);
                JSONObject authorOpt = new JSONObject();
                authorOpt = info.getOpt() != null && !info.getOpt().toString().isEmpty() ? JSONObject.parseObject(info.getOpt().toString()) : null;

                // 是否VIP
                Integer isVip = info.getVip() > System.currentTimeMillis() / 1000 ? 1 : 0;
                Integer isFollow = 0;
                if (user != null && !user.toString().isEmpty()) {
                    // 获取关注
                    Fan fan = new Fan();
                    fan.setUid(user.getUid());
                    fan.setTouid(article.getAuthorId());
                    if (fanService.total(fan) > 0) isFollow = 1;
                }

                // 是否注销
                if (info.getStatus().equals(0)) authorInfo.put("screenName", "用户已注销");

                //加入信息
                authorInfo.put("isFollow", isFollow);
                authorInfo.put("level", level);
                authorInfo.put("nextLevel", nextLevel);
                authorInfo.put("opt", authorOpt);
                authorInfo.put("isVip", isVip);
                // 移除敏感信息
                authorInfo.remove("address");
                authorInfo.remove("assets");
                authorInfo.remove("password");
            } else {
                //加入信息
                authorInfo.put("isFollow", 0);
                authorInfo.put("level", 0);
                authorInfo.put("nextLevel", 0);
                authorInfo.put("isVip", 0);
                authorInfo.put("screenName", "用户已注销");
            }


            // 返回信息
            Map<String, Object> data = JSONObject.parseObject(JSONObject.toJSONString(article), Map.class);

            // 加入信息
            if (article.getImages() != null && !article.getImages().isEmpty())
                data.put("images", JSONArray.parseArray(article.getImages()));
            else data.put("images", images);
            data.put("opt", opt);
            data.put("text", text);
            data.put("category", cateMap);
            data.put("tag", tagDataList);
            data.put("isLike", isLike);
            data.put("isMark", isMark);
            data.put("hide", isHide);
            data.put("authorInfo", authorInfo);
            // 移除信息
            data.remove("passowrd");
            if (user != null && !user.toString().isEmpty()) {
                // 开始写入访问次数至多两次
                Integer endTime = baseFull.endTime();
                // views 存入今天的数据 最多三次
                Integer taskViews = redisHelp.getRedis("views_" + user.getName(), redisTemplate) != null ? Integer.parseInt(redisHelp.getRedis("views_" + user.getName(), redisTemplate)) : 0;
                if (taskViews < 2) {
                    // 点赞送经验和积分
                    user.setAssets((user.getAssets() != null ? user.getAssets() : 0) + 2);
                    user.setExperience((user.getExperience() != null ? user.getExperience() : 0) + 5);
                    redisHelp.delete("views_" + user.getName(), redisTemplate);
                    redisHelp.setRedis("views_" + user.getName(), String.valueOf(taskViews + 1), endTime, redisTemplate);
                    usersService.update(user);
                }
            }

            if(user!=null &&article.getAuthorId().equals(user.getUid()) || isPaid){
                data.put("isHide",0);
            }else{
                data.put("opt",null);
                data.put("isHide",1);
            }
            return Result.getResultJson(200, "获取成功", data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口错误", null);
        }
    }


    /***
     * 文章列表
     */
    @RequestMapping(value = "/articleList")
    @ResponseBody
    public String articleList(@RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                              @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
                              @RequestParam(value = "params", required = false) String params,
                              @RequestParam(value = "random", required = false, defaultValue = "0") Integer random,
                              @RequestParam(value = "searchKey", required = false) String searchKey,
                              @RequestParam(value = "order", required = false, defaultValue = "created desc") String order,
                              HttpServletRequest request) {
        try {
            Boolean permission = false;
            String token = request.getHeader("Authorization");
            Users user = new Users();
            Integer user_id= 0;
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user != null && user.getGroup().equals("administrator") || user.getGroup().equals("editor")) permission = true;
                user_id = user.getUid();
            }
            Article query = new Article();
            if (params != null && !params.isEmpty()) {
                query = JSONObject.parseObject(params, Article.class);
                query.setStatus("publish");
            }
            if (permission) query.setStatus(null);
            PageList<Article> articlePage = service.selectPage(query, page, limit, searchKey, order, random);
            List<Article> articleList = articlePage.getList();
            List dataList = new ArrayList<>();
            for (Article article : articleList) {
                Map<String, Object> data = JSONObject.parseObject(JSONObject.toJSONString(article), Map.class);
                //移除信息
                data.remove("password");
                //格式化数据
                JSONObject opt = new JSONObject();
                opt = article.getOpt() != null && !article.getOpt().toString().isEmpty() ? JSONObject.parseObject(article.getOpt().toString()) : null;

                // 用正则表达式匹配并替换[hide type=pay]这是付费查看的内容[/hide]，并根据type值替换成相应的提示
                Boolean isReply = false;
                Boolean isPaid = false;
                if (user != null && !user.toString().isEmpty()) {
                    // 获取评论状态
                    Comments replyStatus = new Comments();
                    replyStatus.setCid(article.getCid());
                    replyStatus.setUid(user.getUid());
                    Integer rStatus = commentsService.total(replyStatus, null);
                    if (rStatus > 0) {
                        isReply = true;
                    }
                    // 获取购买状态
                    Paylog paylog = new Paylog();
                    paylog.setPaytype("article");
                    paylog.setUid(user.getUid());
                    paylog.setCid(article.getCid());
                    Integer pStatus = paylogService.total(paylog);
                    if (pStatus > 0) {
                        isPaid = true;
                    }
                }
                // 替换隐藏内容
                String text = article.getText();
                Pattern pattern = Pattern.compile("\\[hide type=(pay|reply)\\](.*?)\\[/hide\\]");
                Matcher matcher = pattern.matcher(text);
                StringBuffer replacedText = new StringBuffer();
                while (matcher.find()) {
                    String type = matcher.group(1);
                    String content = matcher.group(2);
                    String replacement = "";
                    if (type.equals("pay") && !isPaid && !article.getAuthorId().equals(user_id) && !permission) {
                        replacement = "【付费查看：这是付费内容，付费后可查看】";
                    } else if (type.equals("reply") && !isReply && !article.getAuthorId().equals(user_id) && !permission) {
                        replacement = "【回复查看：这是回复内容，回复后可查看】";
                    } else {
                        replacement = content;  // 如果不需要替换，则保持原样
                    }
                    matcher.appendReplacement(replacedText, replacement);
                }
                text = matcher.appendTail(replacedText).toString();
                text = text.replace("<!--markdown-->", "");
                text = baseFull.toStrByChinese(text);
                // 获取文章图片
                List images = baseFull.getImageSrc(article.getText());

                // 获取分类和tag
                Category category = metasService.selectByKey(article.getMid());
                Map<String, Object> cateMap = JSONObject.parseObject(JSONObject.toJSONString(category), Map.class);
                JSONObject cateOpt = new JSONObject();
                if (category != null && category.getOpt() != null && category.getOpt().toString() != null && !category.getOpt().toString().isEmpty()) {
                    cateOpt = JSONObject.parseObject(category.getOpt().toString());
                    cateMap.put("opt", cateOpt);
                }
                // 标签
                Relationships tagQuery = new Relationships();
                tagQuery.setCid(article.getCid());
                List<Relationships> tagList = relationshipsService.selectList(tagQuery);
                JSONArray tagDataList = new JSONArray();
                for (Relationships tag : tagList) {
                    Category tagsQuery = new Category();
                    tagsQuery.setMid(tag.getMid());
                    tagsQuery.setType("tag");
                    List<Category> tagInfo = metasService.selectList(tagsQuery);
                    if (tagInfo.size() > 0) {
                        Map<String, Object> tagData = JSONObject.parseObject(JSONObject.toJSONString(tagInfo.get(0)), Map.class);
                        // 移除信息
                        tagData.remove("opt");
                        tagDataList.add(tagData);
                    }

                }

                // 加入作者信息
                Users info = usersService.selectByKey(article.getAuthorId());
                Map<String, Object> authorInfo;
                if (info == null || info.toString().isEmpty()) {
                    authorInfo = new HashMap<>();
                } else {
                    authorInfo = JSONObject.parseObject(JSONObject.toJSONString(info), Map.class);
                }
                if (info != null && !info.toString().isEmpty()) {
                    List result = baseFull.getLevel(info.getExperience(), dataprefix, apiconfigService, redisTemplate);
                    Integer level = (Integer) result.get(0);
                    Integer nextLevel = (Integer) result.get(1);
                    JSONObject authorOpt = new JSONObject();
                    authorOpt = info.getOpt() != null && !info.getOpt().toString().isEmpty() ? JSONObject.parseObject(info.getOpt().toString()) : null;
                    // 是否VIP
                    Integer isVip = 0;
                    if (info.getVip() > System.currentTimeMillis() / 1000) isVip = 1;
                    Integer isFollow = 0;
                    if (user != null && !user.toString().isEmpty()) {
                        // 获取关注
                        Fan fan = new Fan();
                        fan.setUid(user.getUid());
                        fan.setTouid(article.getAuthorId());
                        if (fanService.total(fan) > 0) isFollow = 1;
                    }

                    //

                    // 是否注销
                    if (info.getStatus().equals(0)) authorInfo.put("screenName", "用户已注销");

                    //加入信息
                    authorInfo.put("isFollow", isFollow);
                    authorInfo.put("level", level);
                    authorInfo.put("nextLevel", nextLevel);
                    authorInfo.put("isVip", isVip);
                    authorInfo.put("opt", authorOpt);

                    // 移除敏感信息
                    authorInfo.remove("address");
                    authorInfo.remove("assets");
                    authorInfo.remove("password");
                } else {
                    //加入信息
                    authorInfo.put("isFollow", 0);
                    authorInfo.put("level", 0);
                    authorInfo.put("nextLevel", 0);
                    authorInfo.put("isVip", 0);
                    authorInfo.put("screenName", "账户已注销");
                }

                // 是否点赞或者是否收藏
                Userlog userlog = new Userlog();
                Integer isLike = 0;
                Integer isMark = 0;
                if (user != null && !user.toString().isEmpty()) {
                    userlog.setType("articleLike");
                    userlog.setCid(article.getCid());
                    userlog.setUid(user.getUid());
                    List<Userlog> userlogList = userlogService.selectList(userlog);
                    if (userlogList.size() > 0) isLike = 1;
                    userlog.setType("articleMark");
                    userlogList = userlogService.selectList(userlog);
                    if (userlogList.size() > 0) isMark = 1;
                }

                // 加入信息
                if (article.getImages() != null && !article.getImages().isEmpty())
                    data.put("images", JSONArray.parseArray(article.getImages()));
                else data.put("images", images);
                data.put("opt", opt);
                data.put("text", text);
                data.put("category", cateMap);
                data.put("tag", tagDataList);
                data.put("isLike", isLike);
                data.put("isMark", isMark);
                data.put("authorInfo", authorInfo);
                // 移除信息
                data.remove("passowrd");
                dataList.add(data);
            }
            // 返回信息
            Map<String, Object> data = new HashMap<>();
            data.put("page", page);
            data.put("limit", limit);
            data.put("data", dataList);
            data.put("total", service.total(query, searchKey));
            data.put("count", articleList.size());
            return Result.getResultJson(200, "获取成功", data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }

    }


    /***
     * 发布文章
     */
    @RequestMapping(value = "/articleAdd")
    @ResponseBody
    @XssCleanIgnore
    public String articleAdd(@RequestParam(value = "title") String title,
                             @RequestParam(value = "text") String text,
                             @RequestParam(value = "category") Integer category,
                             @RequestParam(value = "tag", required = false) String tag,
                             @RequestParam(value = "type", defaultValue = "post") String type,
                             @RequestParam(value = "opt", required = false) String opt,
                             @RequestParam(value = "price", required = false, defaultValue = "0") Integer price,
                             @RequestParam(value = "discount", required = false, defaultValue = "1") Float discount,
                             HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Boolean permission = false;
            Integer uid = null;
            Apiconfig apiconfig = UStatus.getConfig(this.dataprefix, apiconfigService, redisTemplate);
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user == null || user.toString().isEmpty())
                    return Result.getResultJson(201, "用户不存在，请重新登录", null);
                uid = user.getUid();
                if (user.getGroup().equals("administrator") || user.getGroup().equals("editor")) {
                    permission = true;
                }
                // 封禁
                if (user.getBantime() != null && user.getBantime() > System.currentTimeMillis() / 1000) {
                    return Result.getResultJson(201, "用户封禁中", null);
                }
            }
            // 判断
            Long timeStamp = System.currentTimeMillis() / 1000;

            if (title == null || title.length() < 3) {
                return Result.getResultJson(201, "标题太短", null);
            }
            if (text == null || text.length() < 15) {
                return Result.getResultJson(201, "内容太少", null);
            }
            if (category == null) {
                return Result.getResultJson(201, "请选择分类", null);
            }
            // 查询分类是否存在
            Category _category = metasService.selectByKey(category);
            if (_category == null || _category.toString().isEmpty()) {
                return Result.getResultJson(201, "分类不存在", null);
            }

            if (_category.getPermission() != null && _category.getPermission().equals(1) && !permission)
                return Result.getResultJson(201, "该分类仅限管理员可用,请重新选择分类", null);

            // 写入文章信息
            Article article = new Article();
            article.setStatus("publish");
            article.setAuthorId(uid);
            article.setText(text);
            article.setTitle(title);
            article.setMid(category);
            article.setType(type);
            article.setPrice(price);
            article.setDiscount(discount);
            article.setOpt(opt);
            article.setCreated(Integer.parseInt(String.valueOf(System.currentTimeMillis() / 1000)));
            if (apiconfig.getContentAuditlevel().equals(1)) article.setStatus("waiting");
            if (apiconfig.getContentAuditlevel().equals(2)) {
                if (!permission) article.setStatus("waiting");
            }

            // 判断redis是否有缓存
            String redisKey = "articleAdd_" + user.getName().toString();
            String redisValue = redisHelp.getRedis(redisKey, redisTemplate);
            int tempNum;
            if (redisValue != null) {
                tempNum = Integer.parseInt(redisValue);
            } else {
                tempNum = 0;
            }
            tempNum++;
            if (tempNum < 3) {
                user.setExperience(user.getExperience() + apiconfig.getPostExp());
                usersService.update(user);
            }

            LocalDateTime endOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
            Duration durationUntilEndOfDay = Duration.between(LocalDateTime.now(), endOfToday);
            long secondsUntilEndOfDay = durationUntilEndOfDay.getSeconds();
            redisHelp.setRedis(redisKey, String.valueOf(tempNum), (int) secondsUntilEndOfDay, redisTemplate);

            // 写入Tag和分类
            Integer articleId = service.insert(article);
            _category.setCount(_category.getCount() + 1);
            Relationships related = new Relationships();
            related.setCid(article.getCid());
            related.setMid(category);
            relationshipsService.insert(related);

            //写入Tag 将字符串分出来
            if (tag != null && !tag.isEmpty()) {
                String[] tags = tag.split(",");
                for (String relateTag : tags) {
                    related.setMid(Integer.parseInt(relateTag));
                    relationshipsService.insert(related);
                }
            }
            if (permission) {
                //如果能直接发布就加经验
                postAddExp(user);
            }
            metasService.update(_category);
            return Result.getResultJson(200, permission ? "发布成功" : "发布成功，请等待审核", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }

    }

    private void postAddExp(Users user) {
        Apiconfig apiconfig = UStatus.getConfig(dataprefix, apiconfigService, redisTemplate);
        Integer exp = user.getExperience() + apiconfig.getPostExp();
        usersService.update(user);
        Userlog log = new Userlog();
        log.setType("postExp");
        log.setToid(user.getUid());
        log.setNum(apiconfig.getPostExp());
        log.setCreated((int) (System.currentTimeMillis() / 1000));
        userlogService.insert(log);
    }

    /***
     * 文章更新
     */

    @RequestMapping(value = "/update")
    @ResponseBody
    @XssCleanIgnore
    public String update(@RequestParam(value = "id") Integer id,
                         @RequestParam(value = "title") String title,
                         @RequestParam(value = "text") String text,
                         @RequestParam(value = "category") Integer category,
                         @RequestParam(value = "tag", required = false) String tag,
                         @RequestParam(value = "opt", required = false) String opt,
                         @RequestParam(value = "price", required = false, defaultValue = "0") Integer price,
                         @RequestParam(value = "discount", required = false, defaultValue = "1") Float discount,
                         HttpServletRequest request) {

        try {
            Boolean permission = false;
            Integer uid = null;
            String token = request.getHeader("Authorization");
            Apiconfig apiconfig = UStatus.getConfig(this.dataprefix, apiconfigService, redisTemplate);
            Article article = service.selectByKey(id);

            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                Users user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                uid = user.getUid();
                if (article.getAuthorId().equals(uid) || user.getGroup().equals("administrator") || user.getGroup().equals("editor"))
                    permission = true;
                else return Result.getResultJson(201, "无权限", null);
            }
            // 更新分类
            relationshipsService.delete(article.getCid());
            Relationships relate = new Relationships();
            relate.setMid(category);
            relate.setCid(article.getCid());
            relationshipsService.insert(relate);
            // 重新设置tag
            if (tag != null && !tag.isEmpty()) {
                String[] tagList = tag.split(",");
                for (String tags : tagList) {
                    relate.setMid(Integer.parseInt(tags));
                    relationshipsService.insert(relate);
                }
            }
            // 设置文章信息
            article.setMid(category);
            article.setText(text);
            article.setTitle(title);
            article.setOpt(opt);
            article.setPrice(price);
            article.setDiscount(discount);
            article.setModified((int) (System.currentTimeMillis() / 1000));
            if (apiconfig.getContentAuditlevel().equals(1)) article.setStatus("waiting");
            if (apiconfig.getContentAuditlevel().equals(2)) {
                if (!permission) article.setStatus("waiting");
            }
            Integer articleUpdate = service.update(article);
            if (articleUpdate == null) {
                return Result.getResultJson(201, "更新失败", null);
            }
            return Result.getResultJson(200, "更新成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }

    }


    @RequestMapping(value = "/delete")
    @ResponseBody
    public String delete(@RequestParam(value = "id") Integer id,
                         HttpServletRequest request) {
        try {
            if (!permission(request.getHeader("Authorization"))) {
                return Result.getResultJson(201, "无权限", null);
            }
            Article article = service.selectByKey(id);
            Apiconfig apiconfig = UStatus.getConfig(dataprefix, apiconfigService, redisTemplate);
            Users user = usersService.selectByKey(article.getAuthorId());
            // 删除
            service.delete(id);
            // 处理链表
            relationshipsService.delete(article.getCid());
            // 更新用户经验
            Integer exp = user.getExperience() - apiconfig.getDeleteExp();
            user.setExperience(exp);
            usersService.update(user);

            // inbox信箱
            Inbox inbox = new Inbox();
            inbox.setTouid(article.getAuthorId());
            inbox.setText("你的文章[" + article.getTitle() + "]已被删除，扣除" + apiconfig.getDeleteExp() + "经验");
            inbox.setCreated((int) (System.currentTimeMillis() / 1000));
            inbox.setType("system");
            inboxService.insert(inbox);
            return Result.getResultJson(200, "删除成功", null);
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
     * 文章审核
     */
    @RequestMapping(value = "/audit")
    @ResponseBody
    public String audit(@RequestParam(value = "id") Integer id,
                        @RequestParam(value = "type") Integer type,
                        @RequestParam(value = "text") String text,
                        HttpServletRequest request) {
        try {
            if (!permission(request.getHeader("Authorization"))) {
                return Result.getResultJson(201, "无权限", null);
            }

            Apiconfig apiconfig = UStatus.getConfig(dataprefix, apiconfigService, redisTemplate);
            Article article = service.selectByKey(id);
            Users user = usersService.selectByKey(article.getAuthorId());
            Inbox inbox = new Inbox();
            inbox.setTouid(article.getAuthorId());
            inbox.setValue(article.getCid());
            inbox.setType("system");
            if (type.equals(0)) {
                article.setStatus("reject");
                inbox.setText("你的文章[" + article.getTitle() + "]审核不通过;原因" + text);
            }
            if (type.equals(1)) {
                article.setStatus("publish");
                inbox.setText("你的文章[" + article.getTitle() + "]审核已通过！");
            }
            if (apiconfig.getIsPush().equals(1)) {
                try {
                    pushService.sendPushMsg(user.getClientId(), "审核通知", "文章[" + article.getTitle() + "]" + (type.equals(0) ? "审核不通过" : "审核通过"), "payload", article.getCid().toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println(e);
                }
            }
            service.update(article);
            inboxService.insert(inbox);
            // 添加经验
            postAddExp(user);
            return Result.getResultJson(200, "操作成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }


    /***
     * 文章操作
     */
    @RequestMapping(value = "/action")
    @ResponseBody
    public String action(@RequestParam(value = "id") Integer id,
                         @RequestParam(value = "type") String type,
                         HttpServletRequest request) {
        try {
            if (!permission(request.getHeader("Authorization"))) {
                return Result.getResultJson(201, "无权限", null);
            }
            Article article = service.selectByKey(id);
            switch (type) {
                case "recommend":
                    article.setIsrecommend(article.getIsrecommend() > 0 ? 0 : 1);
                    break;
                case "top":
                    article.setIstop(article.getIstop() > 0 ? 0 : 1);
                    break;
                case "swiper":
                    article.setIsswiper(article.getIsswiper() > 0 ? 0 : 1);
                    break;
                case "circleTop":
                    article.setIsCircleTop(article.getIsCircleTop() > 0 ? 0 : 1);
                    break;
            }
            service.update(article);
            Map<String, Object> data = new HashMap<>();
            data.put("type", type);
            return Result.getResultJson(200, "操作成功", data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /**
     * 购买文章隐藏内容
     */
    @RequestMapping(value = "/buy")
    @ResponseBody
    public String buy(HttpServletRequest request,
                      @RequestParam(value = "id") Integer id) {
        try {
            String token = request.getHeader("Authorization");
            Users user = new Users();
            Long timeStamp = System.currentTimeMillis() / 1000;
            Boolean vip = false;
            Apiconfig apiconfig = UStatus.getConfig(dataprefix, apiconfigService, redisTemplate);
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user == null || user.toString().isEmpty()) return Result.getResultJson(201, "用户不存在", null);
                if (user.getVip() > timeStamp) vip = true;
            }
            // 文章信息
            Article article = service.selectByKey(id);
            if (article.getAuthorId().equals(user.getUid())) {
                return Result.getResultJson(201, "不可购买自己的文章", null);
            }
            // 查询是否已经购买过
            Paylog buyStatus = new Paylog();
            buyStatus.setUid(user.getUid());
            buyStatus.setCid(id);
            Integer isBuy = paylogService.total(buyStatus);
            if (isBuy > 0) {
                return Result.getResultJson(201, "无需重复购买", null);
            }
            // 开始判断余额
            if (user.getAssets() == null || user.getAssets() < article.getPrice()) {
                return Result.getResultJson(201, "余额不足", null);
            }

            // 购买 减除购买者的资产
            user.setAssets(user.getAssets() - article.getPrice());
            int price = article.getPrice(); // 获取文章原价
            if (vip && article.getDiscount() < 1) {
                price = (int) (price * article.getDiscount()); // 计算折扣后的价格
            } else if (vip) {
                price = (int) (price * Float.parseFloat(apiconfig.getVipDiscount())); // 计算 VIP 折扣后的价格
            }

            user.setAssets(user.getAssets() - price); // 减去购买价格
            //生成订单号
            // 将时间戳转换成日期对象
            Date date = new Date(timeStamp * 1000L);
            SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
            String order = sdf.format(date) + timeStamp + user.getUid();
            Paylog pay = new Paylog();
            pay.setSubject("查看付费文章[" + article.getTitle() + "]");
            pay.setTotalAmount(String.valueOf(price * -1));
            pay.setStatus(1);
            pay.setPaytype("article");
            pay.setUid(user.getUid());
            pay.setCid(article.getCid());
            pay.setOutTradeNo(order);
            paylogService.insert(pay);
            usersService.update(user);

            //给作者写入站内信息
            Inbox inbox = new Inbox();
            inbox.setText("出售文章[" + article.getTitle() + "],获得" + price);
            inbox.setValue(article.getCid());
            inbox.setTouid(article.getAuthorId());
            inbox.setType("finance");
            inbox.setCreated(Math.toIntExact(timeStamp));
            inboxService.insert(inbox);

            // 写入作者获取积分记录
            pay.setSubject("出售文章[" + article.getTitle() + "]");
            pay.setTotalAmount(String.valueOf(price));
            pay.setUid(article.getAuthorId());
            paylogService.insert(pay);

            // 更新作者资产
            Users articleUser = usersService.selectByKey(article.getAuthorId());
            articleUser.setAssets(articleUser.getAssets() + (int) Math.round(0.8 * price));
            usersService.update(articleUser);

            return Result.getResultJson(200, "购买成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }

    }


    @RequestMapping(value = "/rewardList")
    @ResponseBody
    public String rewardList(@RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                             @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
                             @RequestParam(value = "id") Integer id) {
        try {
            // 查询文章是否存在
            Article article = service.selectByKey(id);
            if (article == null || article.toString().isEmpty()) return Result.getResultJson(201, "文章不存在", null);
            // 获取投喂人
            Userlog userlog = new Userlog();
            userlog.setType("reward");
            userlog.setCid(article.getCid());

            PageList<Userlog> userlogPageList = userlogService.selectPage(userlog, page, limit);
            List<Userlog> userlogList = userlogPageList.getList();
            List dataList = new ArrayList<>();
            for (Userlog _userlog : userlogList) {
                Map<String, Object> data = JSONObject.parseObject(JSONObject.toJSONString(usersService.selectByKey(_userlog.getUid())));
                //移除铭感信息
                data.remove("mail");
                data.remove("password");
                data.remove("assets");
                data.remove("opt");

                dataList.add(data);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("data", dataList);
            data.put("count", dataList.size());

            return Result.getResultJson(200, "获取成功", data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    @RequestMapping(value = "/reward")
    @ResponseBody
    public String reward(@RequestParam(value = "id") Integer id,
                         @RequestParam(value = "num") Integer num,
                         HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            // 查询文章是否存在
            Article article = service.selectByKey(id);
            if (article == null || article.toString().isEmpty()) return Result.getResultJson(201, "文章不存在", null);
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(id);
                if (user == null || user.toString().isEmpty())
                    return Result.getResultJson(201, "用户不存在，请重新登录", null);
            }

            if (user.getAssets() < num) return Result.getResultJson(201, "积分不足", null);

            Users author = usersService.selectByKey(article.getAuthorId());

            Integer timeStamp = Math.toIntExact(System.currentTimeMillis() / 1000);
            // 给文章作者爆米
            Paylog paylog = new Paylog();
            paylog.setUid(article.getAuthorId());
            paylog.setSubject("文章奖赏");
            paylog.setTotalAmount(String.valueOf(Math.floor(num * 0.8)));
            paylog.setStatus(1);
            paylog.setPaytype("reward");
            paylog.setCreated(timeStamp);

            // 扣米
            Paylog log = new Paylog();
            paylog.setUid(article.getAuthorId());
            paylog.setSubject("文章打赏");
            paylog.setTotalAmount(String.valueOf(num * -1));
            paylog.setStatus(1);
            paylog.setPaytype("reward");
            paylog.setCreated(timeStamp);

            // 更新用户
            author.setAssets(user.getAssets() > 0 ? user.getAssets() + num : 0 + num);
            user.setAssets(user.getAssets() - num);
            usersService.update(author);
            usersService.update(user);
            paylogService.insert(paylog);

            return Result.getResultJson(200, "打赏成功", null);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     * 全站统计
     */
    @RequestMapping(value = "/allData")
    @ResponseBody
    public String allData(HttpServletRequest request) {
        if (!permission(request.getHeader("Authorization"))) {
            return Result.getResultJson(201, "无权限", null);
        }
        JSONObject data = new JSONObject();
        Article contents = new Article();
        contents.setStatus("publish");
        Integer allContents = service.total(contents, null);

        Comments comments = new Comments();
        Integer allComments = commentsService.total(comments, null);

        Users users = new Users();
        Integer allUsers = usersService.total(users, null);


        Shop shop = new Shop();
        Integer allShop = shopService.total(shop, null);


        Ads ads = new Ads();
        Integer allAds = adsService.total(ads);


        contents.setType("post");
        contents.setStatus("waiting");
        Integer upcomingContents = service.total(contents, null);

        Integer upcomingComments = commentsService.total(comments, null);

        shop.setStatus(0);
        Integer upcomingShop = shopService.total(shop, null);

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
        data.put("allAds", allAds);

        data.put("upcomingContents", upcomingContents);
        data.put("upcomingComments", upcomingComments);
        data.put("upcomingShop", upcomingShop);
        data.put("upcomingAds", upcomingAds);
        data.put("upcomingWithdraw", upcomingWithdraw);

        return Result.getResultJson(200, "获取成功", data);
    }

    /***
     * 关注用户的文章
     */
    @RequestMapping(value = "/follow")
    @ResponseBody
    public String follow(@RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                         @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
                         @RequestParam(value = "order", required = false, defaultValue = "created desc") String order,
                         HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Users user = new Users();
            Boolean permission = permission(request.getHeader("Authorization"));
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
            }
            int offset = (page - 1) * limit; // 计算偏移量
            String sql = "SELECT content.* FROM " + prefix + "_contents AS content JOIN " + prefix + "_fan AS fan ON content.authorId = fan.touid WHERE fan.uid = ? AND content.status = 'publish' ORDER BY content.created DESC LIMIT ?, ?";
            List<Map<String, Object>> articleList = jdbcTemplate.queryForList(sql, user.getUid().toString(), offset, limit);
            JSONArray dataList = new JSONArray();
            for (Map<String, Object> article : articleList) {
                Article articleData = JSONObject.parseObject(JSONObject.toJSONString(article), Article.class);
                Map<String, Object> data = JSONObject.parseObject(JSONObject.toJSONString(article), Map.class);
                //移除信息
                data.remove("password");
                //格式化数据
                JSONObject opt = new JSONObject();
                opt = articleData.getOpt() != null && !articleData.getOpt().toString().isEmpty() ? JSONObject.parseObject(articleData.getOpt().toString()) : null;

                // 用正则表达式匹配并替换[hide type=pay]这是付费查看的内容[/hide]，并根据type值替换成相应的提示
                Integer isReply = 0;
                Integer isPaid = 0;
                if (user.getUid() != null && user.getUid() != 0) {
                    // 获取评论状态
                    Comments replyStatus = new Comments();
                    replyStatus.setCid(articleData.getCid());
                    replyStatus.setUid(user.getUid());
                    Integer rStatus = commentsService.total(replyStatus, null);
                    if (rStatus > 0) {
                        isReply = 1;
                    }
                    // 获取购买状态
                    Paylog paylog = new Paylog();
                    paylog.setPaytype("article");
                    paylog.setUid(user.getUid());
                    paylog.setCid(articleData.getCid());
                    Integer pStatus = paylogService.total(paylog);
                    if (pStatus > 0) {
                        isPaid = 1;
                    }
                }
                // 替换隐藏内容
                String text = articleData.getText();
                Pattern pattern = Pattern.compile("\\[hide type=(pay|reply)\\](.*?)\\[/hide\\]");
                Matcher matcher = pattern.matcher(text);
                StringBuffer replacedText = new StringBuffer();
                while (matcher.find()) {
                    String type = matcher.group(1);
                    String content = matcher.group(2);
                    String replacement = "";
                    if ("pay".equals(type) && isPaid == 0 && user.getUid() != articleData.getAuthorId() && !permission) {
                        replacement = "【付费查看：这是付费内容，付费后可查看】";
                    } else if ("reply".equals(type) && isReply == 0 && user.getUid() != articleData.getAuthorId() && !permission) {
                        replacement = "【回复查看：这是回复内容，回复后可查看】";
                    } else {
                        replacement = content;  // 如果不需要替换，则保持原样
                    }
                    matcher.appendReplacement(replacedText, replacement);
                }
                text = matcher.appendTail(replacedText).toString();
                text = text.replace("<!--markdown-->", "");
                text = baseFull.toStrByChinese(text);
                // 获取文章图片
                List images = baseFull.getImageSrc(articleData.getText());

                // 获取分类和tag
                Category category = metasService.selectByKey(articleData.getMid());
                Map<String, Object> cateMap = JSONObject.parseObject(JSONObject.toJSONString(category), Map.class);
                JSONObject cateOpt = new JSONObject();
                if (category != null && category.getOpt() != null && category.getOpt().toString() != null && !category.getOpt().toString().isEmpty()) {
                    cateOpt = JSONObject.parseObject(category.getOpt().toString());
                    cateMap.put("opt", cateOpt);
                }
                // 标签
                Relationships tagQuery = new Relationships();
                tagQuery.setCid(articleData.getCid());
                List<Relationships> tagList = relationshipsService.selectList(tagQuery);
                JSONArray tagDataList = new JSONArray();
                for (Relationships tag : tagList) {
                    Category tagsQuery = new Category();
                    tagsQuery.setMid(tag.getMid());
                    tagsQuery.setType("tag");
                    List<Category> tagInfo = metasService.selectList(tagsQuery);
                    if (tagInfo.size() > 0) {
                        Map<String, Object> tagData = JSONObject.parseObject(JSONObject.toJSONString(tagInfo.get(0)), Map.class);
                        // 移除信息
                        tagData.remove("opt");
                        tagDataList.add(tagData);
                    }

                }

                // 加入作者信息
                Users info = usersService.selectByKey(articleData.getAuthorId());
                Map<String, Object> authorInfo = JSONObject.parseObject(JSONObject.toJSONString(info), Map.class);
                List result = baseFull.getLevel(info.getExperience(), dataprefix, apiconfigService, redisTemplate);
                Integer level = (Integer) result.get(0);
                Integer nextLevel = (Integer) result.get(1);
                JSONObject authorOpt = new JSONObject();
                authorOpt = info.getOpt() != null && !info.getOpt().toString().isEmpty() ? JSONObject.parseObject(info.getOpt().toString()) : null;
                // 是否VIP
                Integer isVip = 0;
                if (info.getVip() > System.currentTimeMillis() / 1000) isVip = 1;
                // 获取关注
                Fan fan = new Fan();
                fan.setUid(user.getUid());
                fan.setTouid(articleData.getAuthorId());
                Integer isFollow = fanService.total(fan);

                //加入信息
                authorInfo.put("isFollow", isFollow);
                authorInfo.put("level", level);
                authorInfo.put("nextLevel", nextLevel);
                authorInfo.put("isVip", isVip);
                authorInfo.put("opt", authorOpt);
                // 移除敏感信息
                authorInfo.remove("address");
                authorInfo.remove("assets");
                authorInfo.remove("password");
                // 加入信息
                if (articleData.getImages() != null && !articleData.getImages().isEmpty())
                    data.put("images", JSONArray.parseArray(articleData.getImages()));
                else data.put("images", images);
                data.put("opt", opt);
                data.put("text", text);
                data.put("category", cateMap);
                data.put("tag", tagDataList);
                data.put("authorInfo", authorInfo);
                // 移除信息
                data.remove("passowrd");
                dataList.add(data);
            }
            Map<String, Object> data = new HashMap<>();
            data.put("page", page);
            data.put("limit", limit);
            data.put("data", dataList);
            data.put("count", articleList.size());
            return Result.getResultJson(200, "获取成功", data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     * 点赞
     * @param id
     * @param request
     * @return
     */

    @RequestMapping(value = "/like")
    @ResponseBody
    public String like(@RequestParam(value = "id") Integer id,
                       HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user == null || user.toString().isEmpty()) return Result.getResultJson(201, "用户不存在", null);
            }

            Article article = service.selectByKey(id);
            if (article == null || article.toString().isEmpty()) return Result.getResultJson(201, "文章不存在", null);

            //查询是否点过赞
            Userlog userlog = new Userlog();
            userlog.setType("articleLike");
            userlog.setCid(article.getCid());
            userlog.setUid(user.getUid());
            Integer likes = article.getLikes();

            // 存在就删除
            List<Userlog> userlogList = userlogService.selectList(userlog);
            if (userlogList.size() > 0) {
                article.setLikes(likes > 0 ? likes - 1 : 0);
                userlogService.delete(userlogList.get(0).getId());
            } else {
                userlog.setCreated((int) (System.currentTimeMillis() / 1000));
                article.setLikes(likes + 1);
                userlogService.insert(userlog);
                // 获取结束时间
                Integer endTime = baseFull.endTime();
                // likes 存入今天的数据 最多三次
                Integer taskLike = redisHelp.getRedis("likes_" + user.getName(), redisTemplate) != null ? Integer.parseInt(redisHelp.getRedis("likes_" + user.getName(), redisTemplate)) : 0;
                if (taskLike < 3) {
                    // 点赞送经验和积分
                    user.setAssets(user.getAssets() + 2);
                    user.setExperience(user.getExperience() + 5);
                    redisHelp.delete("likes_" + user.getName(), redisTemplate);
                    redisHelp.setRedis("likes_" + user.getName(), String.valueOf(taskLike + 1), endTime, redisTemplate);
                    usersService.update(user);
                }
            }
            service.update(article);
            return Result.getResultJson(200, userlogList.size() > 0 ? "已取消点赞" : "点赞成功", null);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    @RequestMapping(value = "/mark")
    @ResponseBody
    public String mark(@RequestParam(value = "id") Integer id,
                       HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user == null || user.toString().isEmpty()) return Result.getResultJson(201, "用户不存在", null);
            }

            Article article = service.selectByKey(id);
            if (article == null || article.toString().isEmpty()) return Result.getResultJson(201, "文章不存在", null);

            //查询是否收藏
            Userlog userlog = new Userlog();
            userlog.setType("articleMark");
            userlog.setCid(article.getCid());
            userlog.setUid(user.getUid());
            // 存在就删除
            List<Userlog> userlogList = userlogService.selectList(userlog);
            if (userlogList.size() > 0) {
                article.setMarks(article.getMarks() > 0 ? article.getMarks() - 1 : 0);
                userlogService.delete(userlogList.get(0).getId());
            } else {
                article.setMarks(article.getMarks() + 1);
                userlog.setCreated((int) (System.currentTimeMillis() / 1000));
                userlogService.insert(userlog);
                // 获取结束时间
                Integer endTime = baseFull.endTime();
                // likes 存入今天的数据 最多三次
                Integer taskLike = redisHelp.getRedis("marks_" + user.getName(), redisTemplate) != null ? Integer.parseInt(redisHelp.getRedis("marks_" + user.getName(), redisTemplate)) : 0;
                if (taskLike < 3) {
                    // 点赞送经验和积分
                    user.setAssets(user.getAssets() + 2);
                    user.setExperience(user.getExperience() + 5);
                    redisHelp.delete("marks_" + user.getName(), redisTemplate);
                    redisHelp.setRedis("marks_" + user.getName(), String.valueOf(taskLike + 1), endTime, redisTemplate);
                    usersService.update(user);
                }
            }
            service.update(article);
            return Result.getResultJson(200, userlogList.size() > 0 ? "已取消收藏" : "收藏成功", null);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    @RequestMapping(value = "/markList")
    @ResponseBody
    public String markList(@RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                           @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
                           @RequestParam(value = "order", required = false, defaultValue = "created desc") String order,
                           HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user == null || user.toString().isEmpty()) return Result.getResultJson(201, "用户不存在", null);
            }
            // 查询出收藏列表
            Userlog userlog = new Userlog();
            userlog.setType("articleMark");
            userlog.setUid(user.getUid());
            PageList<Userlog> userlogPageList = userlogService.selectPage(userlog, page, limit);
            List<Userlog> userlogList = userlogPageList.getList();
            JSONArray dataList = new JSONArray();
            for (Userlog _userlog : userlogList) {
                Article article = articleService.selectByKey(_userlog.getCid());
                Map<String, Object> data = JSONObject.parseObject(JSONObject.toJSONString(article), Map.class);
                if (article == null || article.toString().isEmpty()) {
                    data.put("title", "文章已删除");
                    data.put("authorId", 0);
                    data.put("id", _userlog.getCid());
                }
                // 格式化文章信息
                JSONObject opt = new JSONObject();
                List images = new ArrayList<>();

                opt = article.getOpt() != null && !article.getOpt().toString().isEmpty() ? JSONObject.parseObject(article.getOpt()) : null;
                if (article.getImages() != null && !article.getImages().toString().isEmpty()) {
                    images = JSONArray.parseArray(article.getImages());
                } else {
                    images = baseFull.getImageSrc(article.getText());
                }
                data.put("opt", opt);

                // 查询作者
                Users articleUser = usersService.selectByKey(article.getAuthorId());
                Map<String, Object> dataArticleUser = JSONObject.parseObject(JSONObject.toJSONString(articleUser));

                // 格式化作者json数据
                JSONArray head_picture = new JSONArray();
                opt = articleUser.getOpt() != null && !articleUser.getOpt().toString().isEmpty() ? JSONObject.parseObject(articleUser.getOpt()) : null;
                dataArticleUser.put("opt", opt);
                dataArticleUser.remove("head_picture");
                dataArticleUser.remove("address");
                dataArticleUser.remove("password");
                data.put("authorInfo", dataArticleUser);
                data.put("images", images);
                data.put("text", baseFull.toStrByChinese(article.getText()));
                data.remove("password");

                dataList.add(data);
            }
            Map<String, Object> data = new HashMap<>();
            data.put("page", page);
            data.put("limit", limit);
            data.put("data", dataList);
            data.put("count", dataList.size());
            data.put("total", userlogService.total(userlog));

            return Result.getResultJson(200, "获取成功", data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }

    }
}