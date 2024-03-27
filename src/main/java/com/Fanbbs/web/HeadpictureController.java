package com.Fanbbs.web;

import com.Fanbbs.common.*;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.Fanbbs.entity.*;
import com.Fanbbs.service.*;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.mysql.cj.jdbc.ha.BalanceStrategy;
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
import java.util.*;

@Component
@Controller
@RequestMapping(value = "/headpicture")
public class HeadpictureController {
    @Autowired
    private UsersService usersService;

    @Autowired
    private HeadpictureService service;

    @Autowired
    private ApiconfigService apiconfigService;

    @Autowired
    private RedisTemplate redisTemplate;

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
     *
     * @param link 链接
     * @param name 名称
     * @param request
     * @return
     */

    @RequestMapping(value = "/add")
    @ResponseBody
    public String headAdd(@RequestParam(value = "link") String link,
                          @RequestParam(value = "name", required = false) String name,
                          HttpServletRequest request) {

        try {
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user == null || user.toString().isEmpty())
                    return Result.getResultJson(201, "用户不存在，请重新登录", null);
            }
            Long timeStamp = System.currentTimeMillis() / 1000;
            if (!permission(request.getHeader("Authorization")) && user.getVip() < timeStamp)
                return Result.getResultJson(201, "该功能会员可用或权限不足", null);

            // 写入数据 type 0是私人 1 是公开
            Headpicture headpicture = new Headpicture();
            headpicture.setStatus(1);
            headpicture.setLink(link);
            headpicture.setType(0);
            headpicture.setPermission(1);
            headpicture.setName("用户头像框");
            headpicture.setCreator(user.getUid());
            if (permission(request.getHeader("Authorization"))) {
                headpicture.setType(1);
                headpicture.setPermission(0);
                headpicture.setName(name);
            }


            service.insert(headpicture);

            // 更新用户的拥有的头像框
            JSONArray head_picture = new JSONArray();
            head_picture = user.getHead_picture() != null && !user.getHead_picture().toString().isEmpty() ? JSONArray.parseArray(user.getHead_picture().toString()) : null;
            if (head_picture == null) {
                head_picture = new JSONArray();
            }
            head_picture.add(headpicture.getId());
            user.setHead_picture(head_picture.toString());
            usersService.update(user);

            Map<String, Object> data = JSONObject.parseObject(JSONObject.toJSONString(headpicture), Map.class);

            return Result.getResultJson(200, "添加完成", data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     *
     * @param page
     * @param limit
     * @param self
     * @param order
     * @param request
     * @return
     */

    @RequestMapping(value = "/list")
    @ResponseBody
    public String list(@RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                       @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
                       @RequestParam(value = "self", required = false) Integer self,
                       @RequestParam(value = "order", required = false) String order,
                       HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user == null || user.toString().isEmpty()) return Result.getResultJson(201, "用户不存在", null);
            }

            // 如果传入id且是管理员才能查看用户的列表 否则就只能查询 type为0 permission为0的数据 且查询自己
            Headpicture headpicture = new Headpicture();
            headpicture.setStatus(1);
            headpicture.setType(1);
            if (permission(request.getHeader("Authorization"))) {
                headpicture.setType(null);
                headpicture.setPermission(null);
                headpicture.setStatus(null);
                headpicture.setType(null);
            }
            if (self != null && self.equals(1)) {
                headpicture.setCreator(user.getUid());
                headpicture.setType(0);
            }
            PageList<Headpicture> headpicturePageList = service.selectPage(headpicture, page, limit, order);
            List<Headpicture> headpictureList = headpicturePageList.getList();

            Map<String, Object> data = new HashMap<>();
            data.put("page", page);
            data.put("limit", limit);
            data.put("data", headpictureList);
            data.put("count", headpictureList.size());
            data.put("total", service.total(headpicture));

            return Result.getResultJson(200, "获取成功", data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    // 设置头像框
    @RequestMapping(value = "/set")
    @ResponseBody
    public String set(HttpServletRequest request, @RequestParam(value = "id") Integer id) {
        try {
            String token = request.getHeader("Authorization");
            Users user = new Users();
            Boolean permission = permission(token);
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user == null || user.toString().isEmpty()) return Result.getResultJson(201, "用户不存在", null);
            }
            // 检查传入id是否存在
            Headpicture headpicture = service.selectByKey(id);
            if (headpicture == null || headpicture.toString().isEmpty())
                return Result.getResultJson(201, "头像框不存在", null);

            JSONArray head_picture = user.getHead_picture() != null ? JSONArray.parseArray(user.getHead_picture()) : null;
            JSONObject opt = user.getOpt() != null ? JSONObject.parseObject(user.getOpt()) : null;
            if (opt == null) opt = new JSONObject();
            // 先判断头像框权限
            if (headpicture != null && headpicture.getPermission() != null && headpicture.getPermission().equals(0)) {
                if (head_picture != null && head_picture.contains(headpicture.getId()) || permission || headpicture.getType().equals(1)) {
                    opt.put("head_picture", headpicture.getLink().toString());
                } else {
                    return Result.getResultJson(201, "你没有获得这个头像框", null);
                }
            }
            if (headpicture.getPermission().equals(1)) {
                opt.put("head_picture", headpicture.getLink().toString());
            }
            //设置用户opt
            user.setOpt(opt.toString());
            usersService.update(user);

            return Result.getResultJson(200, "设置成功", null);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     * 清除头像框
     * @param request
     * @return
     */

    @RequestMapping(value = "/clear")
    @ResponseBody
    public String clear(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user == null || user.toString().isEmpty()) return Result.getResultJson(201, "用户不存在", null);
            }

            if (user.getOpt() != null && !user.getOpt().toString().isEmpty()) {
                // 将opt格式化成Object
                JSONObject opt = JSONObject.parseObject(user.getOpt());
                // 清空opt中的head_picture数据
                opt.put("head_picture", null);

                // 写入数据库
                user.setOpt(opt.toString());
                usersService.update(user);
            }

            return Result.getResultJson(200, "已取消头像框", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     *
     * @param id
     * @param request
     * @return
     */
    @RequestMapping(value = "/delete")
    @ResponseBody
    public String delete(@RequestParam(value = "id") Integer id, HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user == null || user.toString().isEmpty()) return Result.getResultJson(201, "用户不存在", null);
            }
            // 查询头像框是否存在
            Headpicture headpicture = service.selectByKey(id);
            if (headpicture == null || headpicture.toString().isEmpty())
                return Result.getResultJson(201, "头像框不存在", null);

            if (!permission(request.getHeader("Authorization")) && !headpicture.getCreator().equals(user.getUid()))
                return Result.getResultJson(201, "无权限", null);

            service.delete(headpicture.getId());
            return Result.getResultJson(200, "删除成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    @RequestMapping(value = "/update")
    @ResponseBody
    public String update(@RequestParam(value = "id") Integer id,
                         @RequestParam(value = "link") String link,
                         @RequestParam(value = "name") String name,
                         @RequestParam(value = "permission") Integer permission,
                         @RequestParam(value = "status") Integer status,
                         @RequestParam(value = "type") Integer type,
                         HttpServletRequest request) {
        try {
            if (!permission(request.getHeader("Authorization"))) return Result.getResultJson(201, "无权限", null);
            Headpicture headpicture = service.selectByKey(id);
            if (headpicture.getId() == null) return Result.getResultJson(201, "数据不存在", null);
            headpicture.setStatus(status);
            headpicture.setName(name);
            headpicture.setPermission(permission);
            headpicture.setType(type);

            service.update(headpicture);
            return Result.getResultJson(200, "修改成功", null);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }


    // 检查是否是 Administrator 或者 editor

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

