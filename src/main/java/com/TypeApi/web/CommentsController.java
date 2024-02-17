package com.TypeApi.web;

import com.TypeApi.common.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.TypeApi.entity.*;
import com.TypeApi.service.*;
import com.auth0.jwt.interfaces.DecodedJWT;
import net.dreamlu.mica.xss.core.XssCleanIgnore;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
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
import java.util.*;

/**
 * 控制层
 * TypechoCommentsController
 *
 * @author buxia97
 * @date 2021/11/29
 */
@Component
@Controller
@RequestMapping(value = "/comments")
public class CommentsController {

    @Autowired
    CommentsService service;

    @Autowired
    private ArticleService contentsService;

    @Autowired
    private UserlogService userlogService;

    @Autowired
    private UsersService usersService;

    @Autowired
    private CategoryService metasService;

    @Autowired
    private CommentlikeService commentlikeService;

    @Autowired
    private RelationshipsService relationshipsService;

    @Autowired
    private HeadpictureService headpictureService;

    @Autowired
    private ApiconfigService apiconfigService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MailService MailService;

    @Autowired
    private PushService pushService;

    @Autowired
    private InboxService inboxService;

    @Value("${webinfo.CommentCache}")
    private Integer CommentCache;

    @Value("${web.prefix}")
    private String dataprefix;

    RedisHelp redisHelp = new RedisHelp();
    ResultAll Result = new ResultAll();
    UserStatus UStatus = new UserStatus();
    baseFull baseFull = new baseFull();
    EditFile editFile = new EditFile();


    /***
     * 评论列表
     *
     */
    @RequestMapping(value = "/list")
    @ResponseBody
    public String list(@RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                       @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
                       @RequestParam(value = "id", required = false) Integer id,
                       @RequestParam(value = "parent", required = false) Integer parent,
                       @RequestParam(value = "all", required = false) Integer all,
                       @RequestParam(value = "params", required = false) String params,
                       @RequestParam(value = "searchKey", required = false) String searchKey,
                       @RequestParam(value = "order", required = false, defaultValue = "created desc") String order,
                       HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
            }
            Comments comments = new Comments();
            if (params != null && !params.isEmpty()) {
                comments = JSONObject.parseObject(params, Comments.class);
            }
            comments.setCid(id);
            comments.setParent(parent);
            comments.setAll(all);
            PageList<Comments> commentsPageList = service.selectPage(comments, page, limit, searchKey, order);
            List<Comments> commentsList = commentsPageList.getList();

            JSONArray dataList = new JSONArray();
            for (Comments _comments : commentsList) {
                // 获取文章信息
                Article article = contentsService.selectByKey(id != null ? id : _comments.getCid());
                Map<String, Object> data = JSONObject.parseObject(JSONObject.toJSONString(_comments));

                // 查询用户信息
                Users commentUser = usersService.selectByKey(_comments.getUid());
                Map<String, Object> dataUser;

                if (commentUser != null && !commentUser.toString().isEmpty()) {
                    dataUser = JSONObject.parseObject(JSONObject.toJSONString(commentUser));
                } else {
                    dataUser = new HashMap<>();
                    dataUser.put("screenName", "账户已注销");
                    dataUser.put("avatar", null);
                    dataUser.put("level", 0);
                    dataUser.put("nextExp", 0);
                    dataUser.put("isFollow", 0);
                }
                JSONObject opt = new JSONObject();
                if (commentUser != null && !commentUser.toString().isEmpty()) {
                    //移除信息
                    dataUser.remove("password");
                    dataUser.remove("address");
                    // 格式化信息
                    opt = commentUser.getOpt() != null && !commentUser.getOpt().toString().isEmpty() ? JSONObject.parseObject(commentUser.getOpt()) : null;
                    // 处理头像框 查询是否存在替换
                    if (opt != null && !opt.isEmpty() && opt.containsKey("head_picture") && opt.get("head_picture")!=null) {
                        Headpicture headPicture = headpictureService.selectByKey(opt.get("head_picture"));
                        if (headPicture != null) {
                            opt.put("head_picture", headPicture.getLink());
                        }
                    }
                    // 加入信息
                    dataUser.put("opt", opt);
                    // 获取等级
                    dataUser.put("level", baseFull.getLevel(commentUser.getExperience()).get(0));
                }

                // 是否点赞
                CommentLike commentLike = new CommentLike();
                Integer isLike = 0;
                if (user != null && !user.toString().isEmpty() && _comments != null) {
                    commentLike.setCid(_comments.getId());
                    commentLike.setUid(user.getUid());
                    List<CommentLike> commentLikeList = commentlikeService.selectList(commentLike);
                    if (commentLikeList.size() > 0) isLike = 1;
                }

                // 格式化images 数组
                List images = new JSONArray();
                try {
                    images = _comments.getImages() != null && !_comments.toString().isEmpty() ? JSONArray.parseArray(_comments.getImages()) : null;
                } catch (JSONException e) {
                    images = null;
                }
                data.put("images", images);

                // 加入文章信息
                Map<String, Object> articleData = new HashMap<>();
                if (article == null || article.toString().isEmpty()) {
                    articleData.put("title", "文章已删除");
                    articleData.put("id", 0);
                    articleData.put("authorId", 0);
                } else {
                    articleData = JSONObject.parseObject(JSONObject.toJSONString(article), Map.class);
                    // 获取文章中的images 如果article的images存在 则优先使用images
                    images = article.getImages() != null ? JSONArray.parseArray(article.getImages()) : baseFull.getImageSrc(article.getText());
                    articleData.put("images", images);
                }
                // 加入信息

                data.put("article", articleData);
                data.put("isLike", isLike);
                data.put("userInfo", dataUser);
                // 查询一次父评论的信息
                if (_comments.getParent() != null && !_comments.getParent().equals(0) && !_comments.getParent().toString().isEmpty()) {
                    Comments parentComment = service.selectByKey(_comments.getParent());
                    Users parentUser = new Users();
                    if (parentComment != null && !parentComment.toString().isEmpty()) {
                        parentUser = usersService.selectByKey(parentComment.getUid());
                    }

                    Map<String, Object> dataParentUser = JSONObject.parseObject(JSONObject.toJSONString(parentUser));

                    // 格式化数据
                    opt = parentUser.getOpt() != null && !parentUser.getOpt().toString().isEmpty() ? JSONObject.parseObject(parentUser.getOpt()) : null;

                    // 移除信息
                    dataParentUser.remove("address");
                    dataParentUser.remove("password");
                    // 加入信息
                    dataParentUser.put("opt", opt);
                    Map<String, Object> dataParentComment = new HashMap<>();
                    if (parentComment != null && !parentComment.toString().isEmpty()) {
                        dataParentComment = JSONObject.parseObject(JSONObject.toJSONString(parentComment));
                    }
                    dataParentComment.put("userInfo", dataParentUser);
                    data.put("parentComment", dataParentComment);
                }
                // 查询用户信息完成
                // 查询子评论
                Comments subComments = new Comments();
                subComments.setAll(_comments.getId());
                PageList<Comments> subCommentsPageList = service.selectPage(subComments, 1, 2, null, "created desc");
                List<Comments> subCommentsList = subCommentsPageList.getList();
                JSONArray subDataList = new JSONArray();
                // 查询全部数量
                Integer total = service.total(subComments, null);
                for (Comments _subComments : subCommentsList) {
                    Map<String, Object> subData = JSONObject.parseObject(JSONObject.toJSONString(_subComments));
                    // 查询子评论用户信息
                    Users subCommentUser = usersService.selectByKey(_subComments.getUid());
                    Map<String, Object> subDataUser = JSONObject.parseObject(JSONObject.toJSONString(subCommentUser));
                    // 移除敏感信息
                    subDataUser.remove("password");
                    subDataUser.remove("address");
                    // 格式化用户信息
                    opt = subCommentUser.getOpt() != null && !subCommentUser.getOpt().isEmpty() ? JSON.parseObject(subCommentUser.getOpt()) : null;
                    images = _subComments.getImages() != null && !_subComments.toString().isEmpty() ? JSONArray.parseArray(_subComments.getImages()) : null;

                    //加入文章信息
                    Map<String, Object> subArticleData = new HashMap<>();
                    if (article == null || article.toString().isEmpty()) {
                        subArticleData.put("title", "文章已删除");
                        subArticleData.put("id", 0);
                        subArticleData.put("authorId", 0);
                    } else {
                        subArticleData = JSONObject.parseObject(JSONObject.toJSONString(article));
                        // 获取文章中的images 如果article的images存在 则优先使用images
                        images = article.getImages() != null ? JSONArray.parseArray(article.getImages()) : baseFull.getImageSrc(article.getText());
                        subArticleData.put("images", images);
                    }


                    // 添加用户信息
                    subData.put("userInfo", subDataUser);
                    subData.put("article", subArticleData); // 将文章信息添加到子评论数据中
                    subDataList.add(subData);
                }
                // 将子评论列表添加到父评论数据中
                Map<String, Object> subDataObject = new HashMap<>();
                subDataObject.put("data", subDataList);
                subDataObject.put("count", total);
                data.put("subComments", subDataObject);
                dataList.add(data);
            }
            Map<String, Object> data = new HashMap<>();
            data.put("page", page);
            data.put("limit", limit);
            data.put("data", dataList);
            data.put("count", dataList.size());
            data.put("total", service.total(comments, searchKey));
            return Result.getResultJson(200, "获取成功", data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }


    /***
     * 添加评论
     */
    @RequestMapping(value = "/add")
    @XssCleanIgnore
    @ResponseBody
    public String add(@RequestParam(value = "id") Integer id,
                      @RequestParam(value = "parent", required = false, defaultValue = "0") Integer parent,
                      @RequestParam(value = "all", required = false, defaultValue = "0") Integer all,
                      @RequestParam(value = "text") String text,
                      @RequestParam(value = "images", required = false) String images,
                      HttpServletRequest request) {
        try {
            Apiconfig apiconfig = UStatus.getConfig(dataprefix, apiconfigService, redisTemplate);
            String token = request.getHeader("Authorization");
            Long timeStamp = System.currentTimeMillis() / 1000;
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user == null || user.toString().isEmpty())
                    return Result.getResultJson(201, "用户不存在,请重新登录", null);
            }

            // 定义一个变量来获取替换掉的内容
            String nonText = null;
            Boolean permission = permission(token);

            if (text != null) {
                nonText = text.replaceAll("style=\"[^\"]*\"", ""); // 使用正则表达式匹配并替换带有 style 属性的部分
            }

            Article article = contentsService.selectByKey(id);
            Integer commentsNum = article.getCommentsNum() + 1;
            article.setCommentsNum(commentsNum);
            contentsService.update(article);
            Users articleUser = usersService.selectByKey(article.getAuthorId());

            Inbox inbox = new Inbox();
            inbox.setText(article.getTitle());
            inbox.setTouid(article.getAuthorId());

            if (article == null || article.toString().isEmpty()) return Result.getResultJson(201, "文章不存在", null);
            Comments comments = new Comments();
            if (all != null && !all.toString().equals("")) comments.setAll(all);
            if (parent != null && !parent.toString().equals("") && !parent.equals(0)) {
                comments.setParent(parent);
                Comments parentComments = service.selectByKey(parent);
                if (parentComments != null && !parentComments.toString().isEmpty()) {
                    //查询父评论的用户
                    Users parentUser = usersService.selectByKey(parentComments.getUid());
                    inbox.setTouid(parentUser.getUid());
                    inbox.setText(nonText);
                    inbox.setValue(parentComments.getId());
                    // push发送
                    if (apiconfig.getIsPush().equals(1)) {
                        pushService.sendPushMsg(parentUser.getClientId(), "有新的评论", text, "payload", "system");
                    }
                }
            }

            if (text == null || text.isEmpty()) return Result.getResultJson(201, "请输入评论", null);
            comments.setText(user.getVip() < timeStamp && !permission ? nonText : text);
            if (images != null && !images.toString().isEmpty()) comments.setImages(images);
            comments.setIp(baseFull.getIpAddr(request));
            comments.setCreated(Math.toIntExact(timeStamp));
            comments.setCid(article.getCid());
            comments.setUid(user.getUid());
            comments.setType(0);

            service.insert(comments);
            // 给用户发消息
            inbox.setCreated(Math.toIntExact(timeStamp));
            inbox.setType("comment");
            inbox.setUid(user.getUid());
            if (parent == null || parent.equals(0)) {
                inbox.setValue(comments.getId());
            }
            inbox.setIsread(0);
            inboxService.insert(inbox);
            // push发送
            if (apiconfig.getIsPush().equals(1)) {
                pushService.sendPushMsg(articleUser.getClientId(), "有新的评论", text, "payload", "system");
            }
            String redisKey = "comments_" + user.getName().toString();
            String redisValue = redisHelp.getRedis(redisKey, redisTemplate);
            int tempNum;
            if (redisValue != null) {
                tempNum = Integer.parseInt(redisValue);
            } else {
                tempNum = 0; // 第一次评论时，评论次数为 0
            }

            // 如果评论次数小于 3，则增加经验值
            if (tempNum < 3) {
                user.setExperience(user.getExperience() + apiconfig.getReviewExp());
                usersService.update(user);
            }

            // 更新评论次数并设置过期时间
            tempNum++; // 增加评论次数
            LocalDateTime endOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
            Duration durationUntilEndOfDay = Duration.between(LocalDateTime.now(), endOfToday);
            long secondsUntilEndOfDay = durationUntilEndOfDay.getSeconds();
            redisHelp.setRedis(redisKey, String.valueOf(tempNum), (int) secondsUntilEndOfDay, redisTemplate);
            return Result.getResultJson(200, "评论成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }

    }

//    测试

    @RequestMapping("/test")
    @ResponseBody
    public String test(HttpServletRequest request, @RequestParam(value = "name") String name) {
        return redisHelp.getRedis("comments_" + name, redisTemplate);
    }

    /***
     * 删除评论
     * @param id
     * @param request
     * @return
     */
    @RequestMapping(value = "/delete")
    @ResponseBody
    public String delete(@RequestParam(value = "id") Integer id,
                         HttpServletRequest request) {
        try {
            Boolean permission = permission(request.getHeader("Authorization"));
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
            }
            Comments comments = service.selectByKey(id);
            if (comments == null || comments.toString().isEmpty()) return Result.getResultJson(201, "评论不存在", null);
            if (!permission || user.getUid().equals(comments.getUid()))
                return Result.getResultJson(201, "无权限", null);

            service.delete(id);

            return Result.getResultJson(200, "删除成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     *
     * @param id
     * @param text
     * @param request
     * @return
     */
    @RequestMapping(value = "/edit")
    @ResponseBody
    public String edit(@RequestParam(value = "id") Integer id,
                       @RequestParam(value = "text") String text,
                       HttpServletRequest request) {
        try {
            Boolean permission = permission(request.getHeader("Authorization"));
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
            }
            Comments comments = service.selectByKey(id);
            if (comments == null || comments.toString().isEmpty()) return Result.getResultJson(201, "评论不存在", null);
            if (!permission || user.getUid().equals(comments.getUid()))
                return Result.getResultJson(201, "无权限", null);
            comments.setText(text);
            comments.setModified((int) (System.currentTimeMillis() / 1000));
            service.update(comments);
            return Result.getResultJson(200, "修改完成", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     * 评论点赞
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
            Comments comments = service.selectByKey(id);
            if (comments == null || comments.toString().isEmpty()) return Result.getResultJson(201, "评论不存在", null);

            // 查询是否已经关注过了
            CommentLike commentLike = new CommentLike();
            commentLike.setCid(id);
            commentLike.setUid(user.getUid());
            List<CommentLike> commentLikeList = commentlikeService.selectList(commentLike);
            commentLike.setCreated((int) (System.currentTimeMillis() / 1000));
            // 获取评论

            Integer likes = comments.getLikes() == null ? 0 : comments.getLikes();
            if (commentLikeList != null && commentLikeList.size() > 0) {
                // 存在就删除
                commentlikeService.delete(commentLikeList.get(0).getId());
                comments.setLikes(likes > 0 ? likes - 1 : 0);
            } else {
                comments.setLikes(likes + 1);
                commentlikeService.insert(commentLike);
            }

            service.update(comments);

            return Result.getResultJson(200, commentLikeList.size() > 0 ? "已取消点赞" : "点赞成功", null);


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
}
