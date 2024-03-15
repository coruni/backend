package com.TypeApi.web;

import com.TypeApi.common.*;
import com.TypeApi.entity.*;
import com.TypeApi.service.*;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Controller
@RequestMapping(value = "/rank")
public class RankController {


    @Autowired
    private UsersService usersService;

    @Autowired
    private RankService rankService;



    ResultAll Result = new ResultAll();

    // 添加头衔
    @RequestMapping(value = "/add")
    @ResponseBody
    public String rankAdd(@RequestParam(value = "name") String name,
                            @RequestParam(value = "type") Integer type,
                            @RequestParam(value = "image", required = false) String image,
                            @RequestParam(value = "color", required = false) String color,
                            @RequestParam(value = "background", required = false) String background,
                            @RequestParam(value = "permission", required = false) Integer permission,
                            HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Users user = getUser(token);
            if (user.getUid() == null || user.toString().isEmpty())
                return Result.getResultJson(201, "用户不存在", null);
            if (!permission(user)) {
                return Result.getResultJson(201, "无权限", null);
            }
            if (name == null || name.isEmpty())
                return Result.getResultJson(201, "请输入头衔名称", null);
            if(type != 0&&type != 1){
                return Result.getResultJson(201, "头衔类型错误", null);
            }
            if(type == 1 && image == null){
                return Result.getResultJson(201, "请设置图片", null);
            }
            long timestamp = System.currentTimeMillis();
            Rank rank = new Rank();
            rank.setName(name);
            rank.setType(type);
            rank.setImage(image);
            rank.setColor(color);
            rank.setBackground(background);
            rank.setCreated((int) (timestamp / 1000));
            rankService.insert(rank);
            return Result.getResultJson(200, "添加成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }

    }

    // 查询头衔
    @RequestMapping(value = "/list")
    @ResponseBody
    public String list(@RequestParam(value = "id", required = false) Integer id,
                       @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                       @RequestParam(value = "limit", required = false, defaultValue = "16 ") Integer limit,
                       HttpServletRequest request) {
        try {
            PageList<Rank> rankPageList = rankService.selectPage(new Rank(), page, limit, "", "");
            List<Rank> rankList = rankPageList.getList();
            int total = rankService.total(new Rank(), "");
            Map<String, Object> data = new HashMap<>();
            data.put("count", rankList.size());
            data.put("total", total);
            data.put("data", rankList);
            return Result.getResultJson(200, "获取成功", data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }

    }

    // 修改头衔
    @RequestMapping(value = "/update")
    @ResponseBody
    public String rankUpdate(@RequestParam(value = "id") Integer id,
                               @RequestParam(value = "name", required = false) String name,
                               @RequestParam(value = "type", required = false) Integer type,
                               @RequestParam(value = "image", required = false) String image,
                               @RequestParam(value = "color", required = false) String color,
                               @RequestParam(value = "background", required = false) String background,
                               HttpServletRequest request) {
        try {

            String token = request.getHeader("Authorization");
            Users user = getUser(token);
            if (user.getUid() == null || user.toString().isEmpty())
                return Result.getResultJson(201, "用户不存在", null);
            if (!permission(user)) {
                return Result.getResultJson(201, "无权限", null);
            }
            if (id == null || id == 0)
                return Result.getResultJson(201, "ID不能为空", null);
            if (name == null || name.isEmpty())
                return Result.getResultJson(201, "请输入头衔名称", null);
            if (type !=0 && type != 1)
                return Result.getResultJson(201, "头衔类型错误", null);
            if(type == 1 && image == null){
                return Result.getResultJson(201, "请设置图片", null);
            }
            long timestamp = System.currentTimeMillis();
            Rank rank = new Rank();
            rank.setId(id);
            rank.setName(name);
            rank.setType(type);
            rank.setImage(image);
            rank.setColor(color);
            rank.setBackground(background);
            rankService.update(rank);
            return Result.getResultJson(200, "修改成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }


    /***
     * 删除
     * @param id
     * @param request
     * @return
     */
    @RequestMapping(value = "/delete")
    @ResponseBody
    public String delete(@RequestParam(value = "id") Integer id,
                         HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Users user = getUser(token);
            if (!permission(user)) return Result.getResultJson(201, "无权限", null);

            Rank rank = rankService.selectByKey(id);
            if (rank.getId() == null || rank.toString().isEmpty())
                return Result.getResultJson(201, "数据不存在", null);

            rankService.delete(id);
            return Result.getResultJson(200, "删除成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    // 设置头衔
    @RequestMapping(value = "/set")
    @ResponseBody
    public String set(HttpServletRequest request, @RequestParam(value = "id") Integer id) {
        try {
            String token = request.getHeader("Authorization");
            Users user = getUser(token);
            Boolean permission = permission(user);
            if (user.getUid() == null || user.toString().isEmpty())
                return Result.getResultJson(201, "用户不存在", null);
            // 检查传入id是否存在
            Rank rank = rankService.selectByKey(id);
            if (rank == null || rank.toString().isEmpty())
                return Result.getResultJson(201, "头衔不存在", null);

            JSONArray ranklist = user.getRank() != null ? JSONArray.parseArray(user.getRank()) : null;
            JSONObject opt = user.getOpt() != null ? JSONObject.parseObject(user.getOpt()) : null;
            if (opt == null) opt = new JSONObject();

            if (ranklist != null && ranklist.contains(rank.getId()) || permission || rank.getType().equals(1)) {
                opt.put("rank", rank);
            } else {
                return Result.getResultJson(201, "你没有获得这个头衔", null);
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
     * 清除头衔
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
                // 清空opt中的rank数据
                opt.put("rank", null);

                // 写入数据库
                user.setOpt(opt.toString());
                usersService.update(user);
            }

            return Result.getResultJson(200, "已取消头衔", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }


    /***
     * 权限判断
     *
     * @param user
     * @return
     */
    private boolean permission(Users user) {
        if (user.getUid() == null || user.getUid().equals(0))
            return false;
        if (user.getGroup().equals("administrator") || user.getGroup().equals("editor"))
            return true;
        return false;
    }

    /***
     * 获取用户信息
     *
     * @param token
     * @return
     */
    private Users getUser(String token) {
        if (token == null || token.isEmpty())
            return new Users();
        // 获取用户信息
        DecodedJWT verify = JWT.verify(token);
        Users user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
        return user;
    }

}