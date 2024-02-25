package com.TypeApi.web;

import com.TypeApi.common.*;
import com.alibaba.fastjson.*;
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

@Component
@Controller

@RequestMapping(value = "/exchange")
public class ExchangeController {
    @Autowired
    ExchangeService service;

    @Autowired
    private UsersService usersService;


    @Autowired
    RankService rankService;

    @Autowired
    private HeadpictureService headpictureService;

    @Autowired
    private ApiconfigService apiconfigService;

    @Autowired
    private PaylogService paylogService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${web.prefix}")
    private String dataprefix;

    RedisHelp redisHelp = new RedisHelp();
    ResultAll Result = new ResultAll();
    UserStatus UStatus = new UserStatus();
    baseFull baseFull = new baseFull();
    EditFile editFile = new EditFile();

    @RequestMapping(value = "/list")
    @ResponseBody
    public String list(@RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                       @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
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
            // 开始查询列表
            Exchange exchange = new Exchange();
            PageList<Exchange> exchangePageList = service.selectPage(exchange, page, limit, searchKey, order);
            List<Exchange> exchangeList = exchangePageList.getList();
            List dataList = new ArrayList<>();
            for (Exchange _exchange : exchangeList) {
                Map<String, Object> data = JSONObject.parseObject(JSONObject.toJSONString(_exchange), Map.class);
                Map<String, Object> detail = new HashMap<>();
                // 根据type查询对应的表数据
                if (_exchange.getType().equals("avatar")) {
                    detail = JSONObject.parseObject(JSONObject.toJSONString(headpictureService.selectByKey(_exchange.getExchange_id())));
                    data.put("detail", detail);
                }
                if (_exchange.getType().equals("rank")) {
                    detail = JSONObject.parseObject(JSONObject.toJSONString(rankService.selectByKey(_exchange.getExchange_id())));
                    data.put("detail",detail);
                }
                dataList.add(data);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("page", page);
            data.put("limit", limit);
            data.put("count", dataList.size());
            data.put("data", dataList);
            return Result.getResultJson(200, "获取成功", data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    @RequestMapping(value = "/exchange")
    @ResponseBody
    public String exchange(@RequestParam(value = "id") Integer id,
                           HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Users user = new Users();
            Boolean permission = false;
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user == null || user.toString().isEmpty()) return Result.getResultJson(201, "用户不存在", null);
                if (user != null && user.getGroup().equals("administrator") && user.getGroup().equals("editor"))
                    permission = true;
            }
            // 查询id 对应的数据是否存在
            Exchange exchange = service.selectByKey(id);
            if (exchange == null || exchange.toString().isEmpty()) return Result.getResultJson(201, "数据不存在", null);
            // 根据type查询对应数据
            Integer price = 0;
            String type = null;
            if (exchange.getType().equals("avatar")) {
                Headpicture headpicture = headpictureService.selectByKey(exchange.getExchange_id());
                if (headpicture == null || headpicture.toString().isEmpty())
                    return Result.getResultJson(201, "头像框不存在", null);

                // 查询是否已拥有该头像框
                JSONArray head_picture = JSONArray.parseArray(user.getHead_picture());
                if (head_picture == null) head_picture = new JSONArray();
                if (head_picture.contains(id)) return Result.getResultJson(201, "已拥有该头像框", null);
                price = exchange.getPrice();
                type = exchange.getType();
            }


            if (exchange.getType().equals("rank")) {
                Rank rank = rankService.selectByKey(exchange.getExchange_id());
                if (rank == null || rank.toString().isEmpty())
                    return Result.getResultJson(201, "头衔不存在", null);
                // 查询是否已拥有该头衔
                JSONArray rank_list = JSONArray.parseArray(user.getRank());
                if (rank_list == null) rank_list = new JSONArray();
                if (rank_list.contains(id)) return Result.getResultJson(201, "已拥有该头衔", null);
                price = exchange.getPrice();
                type = exchange.getType();
            }

            if (user.getAssets() < price) return Result.getResultJson(201, "积分不足", null);
            // 更新用户的积分
            user.setAssets(user.getAssets() - price);


            // 根据Type把id写入用户中
            List rank = user.getRank() != null ? JSONArray.parseArray(user.getRank()) : new ArrayList<>();
            List headpicture = user.getHead_picture() != null ? JSONArray.parseArray(user.getHead_picture()) : new ArrayList<>();
            if (type.equals("avatar")) headpicture.add(exchange.getExchange_id());
            if (type.equals("rank")) rank.add(exchange.getExchange_id());
            user.setHead_picture(headpicture.toString());
            user.setRank(rank.toString());
            usersService.update(user);

            // 写入paylog
            Paylog paylog = new Paylog();
            paylog.setUid(user.getUid());
            paylog.setSubject("兑换【" + exchange.getName() + "】");
            paylog.setStatus(1);
            paylog.setTotalAmount(String.valueOf(price * -1));
            paylog.setCreated((int) (System.currentTimeMillis() / 1000));
            paylogService.insert(paylog);

            return Result.getResultJson(200, "兑换成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    @RequestMapping(value = "/add")
    @ResponseBody
    public String add(@RequestParam(value = "name") String name,
                      @RequestParam(value = "type") String type,
                      @RequestParam(value = "id") Integer id,
                      HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user == null || user.toString().isEmpty()) return Result.getResultJson(201, "用户不存在", null);
                if (!user.getGroup().equals("administrator") && !user.getGroup().equals("editor"))
                    return Result.getResultJson(201, "无权限", null);
            }

            // 查询传入的id是否存在
            Exchange query = new Exchange();
            query.setExchange_id(id);
            query.setType(type);

            List<Exchange> exchangeList = service.selectList(query);
            if (exchangeList.size() > 0) return Result.getResultJson(201, "已存在相同数据", null);

            Integer created = Math.toIntExact(System.currentTimeMillis() / 1000);
            Exchange exchange = new Exchange();
            exchange.setName(name);
            exchange.setExchange_id(id);
            exchange.setType(type);
            exchange.setCreated(created);
            return Result.getResultJson(200, "添加完成", null);

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
            if (!permission(request.getHeader("Authorization"))) return Result.getResultJson(201, "无权限", null);
            Exchange exchange = service.selectByKey(id);
            if (exchange == null || exchange.toString().isEmpty()) return Result.getResultJson(201, "数据不存在", null);
            service.delete(id);
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
}
