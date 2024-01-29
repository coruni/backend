package com.TypeApi.web;

import com.TypeApi.common.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.TypeApi.entity.*;
import com.TypeApi.service.*;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 控制层
 * TypechoMetasController
 *
 * @author buxia97
 * @date 2021/11/29
 */
@Component
@Controller
@RequestMapping(value = "/category")
public class CategoryController {

    @Autowired
    CategoryService service;

    @Autowired
    private RelationshipsService relationshipsService;

    @Autowired
    private ArticleService contentsService;

    @Autowired
    private FieldsService fieldsService;

    @Autowired
    private FanService fanService;
    @Autowired
    private UsersService usersService;

    @Autowired
    private ApiconfigService apiconfigService;

    @Autowired
    private UserlogService userlogService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${webinfo.contentCache}")
    private Integer contentCache;

    @Value("${web.prefix}")
    private String dataprefix;


    RedisHelp redisHelp = new RedisHelp();
    ResultAll Result = new ResultAll();
    baseFull baseFull = new baseFull();
    UserStatus UStatus = new UserStatus();
    EditFile editFile = new EditFile();


    /***
     * 查询分类和标签
     * @param params Bean对象JSON字符串
     * @param page         页码
     * @param limit        每页显示数量
     */
    @RequestMapping(value = "/list")
    @ResponseBody
    public String categoryList(@RequestParam(value = "params", required = false) String params,
                               @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                               @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
                               @RequestParam(value = "searchKey", required = false) String searchKey,
                               @RequestParam(value = "order", required = false) String order,
                               HttpServletRequest request) {
        try {
            Category query = new Category();
            Users user = new Users();
            String token = request.getHeader("Authorization");
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
            }
            if (params != null && !params.isEmpty()) {
                query = JSON.parseObject(params, Category.class);
            }
            // 查询列表
            PageList<Category> categoryPageList = service.selectPage(query, page, limit, searchKey, order);
            List<Category> categoryList = categoryPageList.getList();
            JSONArray dataList = new JSONArray();
            for (Category category : categoryList) {
                Map<String, Object> data = JSONObject.parseObject(JSONObject.toJSONString(category), Map.class);
                // 格式化信息
                JSONObject opt = new JSONObject();
                opt = category.getOpt() != null && !category.getOpt().toString().isEmpty() ? JSONObject.parseObject(category.getOpt()) : null;
                // 查询是否关注
                Integer isFollow = 0;
                Userlog userlog = new Userlog();
                userlog.setNum(category.getMid());
                userlog.setType("category");
                if (user != null && !user.toString().isEmpty()) {
                    userlog.setUid(user.getUid());
                    List<Userlog> userlogList = userlogService.selectList(userlog);
                    if (userlogList.size() > 0) isFollow = 1;
                }
                // 去除uid查询 查询所有关注数量
                userlog.setUid(null);
                data.put("opt", opt);
                data.put("isFollow", isFollow);

                dataList.add(data);
            }
            Map<String, Object> data = new HashMap<>();
            data.put("page", page);
            data.put("limit", limit);
            data.put("data", dataList);
            data.put("count", dataList.size());
            data.put("total", service.total(query));
            return Result.getResultJson(200, "获取成功", data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     * 查询分类详情
     */
    @RequestMapping(value = "/info")
    @ResponseBody
    public String info(@RequestParam(value = "id") Integer id,
                       HttpServletRequest request) {
        try {
            Users user = new Users();
            String token = request.getHeader("Authorization");
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
            }
            Category category = service.selectByKey(id);
            if (category == null || category.toString().isEmpty()) return Result.getResultJson(201, "分类不存在", null);
            Map<String, Object> data = JSONObject.parseObject(JSONObject.toJSONString(category), Map.class);
            // 格式化opt
            JSONObject opt = new JSONObject();
            opt = category.getOpt() != null && !category.toString().isEmpty() ? JSONObject.parseObject(category.getOpt()) : null;

            // 查询是否关注
            Integer isFollow = 0;
            Userlog userlog = new Userlog();
            userlog.setNum(category.getMid());
            userlog.setType("category");
            if (user != null && !user.toString().isEmpty()) {
                userlog.setUid(user.getUid());
                List<Userlog> userlogList = userlogService.selectList(userlog);
                if (userlogList.size() > 0) isFollow = 1;
            }
            // 去除uid查询 查询所有关注数量
            userlog.setUid(null);
            data.put("opt", opt);
            data.put("isFollow", isFollow);

            return Result.getResultJson(200, "获取成功", data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     * 修改分类和标签
     */
    @RequestMapping(value = "/update")
    @ResponseBody

    public String update(@RequestParam(value = "id") Integer id,
                         @RequestParam(value = "name", required = false) String name,
                         @RequestParam(value = "description", required = false) String description,
                         @RequestParam(value = "avatar", required = false) String avatar,
                         @RequestParam(value = "opt", required = false) String opt,
                         HttpServletRequest request) {
        try {
            if (!permission(request.getHeader("Authorization"))) return Result.getResultJson(201, "无权限", null);
            Category category = service.selectByKey(id);
            if (category == null || category.toString().isEmpty()) return Result.getResultJson(201, "分类不存在", null);

            if (name != null && !name.isEmpty()) category.setName(name);
            if (description != null && !description.isEmpty()) category.setDescription(description);
            if (avatar != null && !avatar.isEmpty()) category.setImgurl(avatar);
            if (opt != null && !opt.isEmpty()) category.setOpt(opt);
            service.update(category);

            return Result.getResultJson(200, "修改成功", null);
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
     * 添加分类 或 标签
     */
    @RequestMapping(value = "/add")
    @ResponseBody
    public String add(@RequestParam(value = "name") String name,
                      @RequestParam(value = "description") String description,
                      @RequestParam(value = "avatar") String avatar,
                      @RequestParam(value = "opt") String opt,
                      @RequestParam(value = "type") String type,
                      HttpServletRequest request) {
        try {
            if (!permission(request.getHeader("Authorization"))) return Result.getResultJson(201, "无权限", null);
            Category category = new Category();
            category.setName(name);
            category.setDescription(description);
            category.setImgurl(avatar);
            category.setType(type);
            category.setOpt(opt);
            service.insert(category);
            return Result.getResultJson(200, "添加成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     * 操作分类
     */
    @RequestMapping("/action")
    @ResponseBody
    public String action(@RequestParam(value = "id") Integer id,
                         @RequestParam(value = "type") String type,
                         HttpServletRequest request) {
        try {
            if (!permission(request.getHeader("Authorization"))) return Result.getResultJson(201, "无权限", null);
            Category category = service.selectByKey(id);
            if (category == null || category.toString().isEmpty()) return Result.getResultJson(201, "分类不存在", null);
            if (type.equals("recommend")) category.setIsrecommend(category.getIsrecommend() > 0 ? 0 : 1);
            if (type.equals("waterfall")) category.setIswaterfall(category.getIswaterfall() > 0 ? 0 : 1);
            service.update(category);
            return Result.getResultJson(200, "操作成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);

        }
    }

    /***
     * 删除分类和标签
     */
    @RequestMapping(value = "/delete")
    @ResponseBody
    public String delete(@RequestParam(value = "id") Integer id,
                         HttpServletRequest request) {
        try {
            if (!permission(request.getHeader("Authorization"))) return Result.getResultJson(201, "无权限", null);
            Category category = service.selectByKey(id);
            if (category == null || category.toString().isEmpty()) return Result.getResultJson(201, "分类不存在", null);

            service.delete(id);

            return Result.getResultJson(200, "删除成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     * 关注分类
     */
    @RequestMapping(value = "/follow")
    @ResponseBody
    public String follow(@RequestParam(value = "id") Integer id,
                         HttpServletRequest request) {
        try {
            String token = request.getHeader("'Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user == null || user.toString().isEmpty()) return Result.getResultJson(201, "用户不存在", null);
            }
            Category category = service.selectByKey(id);
            if (category == null || category.toString().isEmpty()) return Result.getResultJson(201, "分类不存在", null);
            // 使用userlog 来存储关注分类
            Userlog userlog = new Userlog();

            userlog.setType("category");
            userlog.setUid(user.getUid());
            userlog.setNum(category.getMid());
            // 查询是否存在记录
            List<Userlog> userlogList = userlogService.selectList(userlog);
            category.setFollows(category.getFollows() + 1);
            // 存在就删除记录并返回取消关注成功
            if (userlogList.size() > 0) {
                category.setFollows(category.getFollows() > 0 ? category.getFollows() - 1 : 0);
                userlogService.delete(userlogList.get(0).getId());
                service.update(category);
                return Result.getResultJson(200, "取消关注成功", null);
            }
            // 不存在就继续下一步写入数据库并返回关注成功
            userlog.setCreated((int) (System.currentTimeMillis() / 1000));
            // 写入数据库
            service.update(category);
            userlogService.insert(userlog);
            return Result.getResultJson(200, "关注成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

}