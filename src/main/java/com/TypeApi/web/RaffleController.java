package com.TypeApi.web;

import com.TypeApi.common.*;
import com.TypeApi.entity.*;
import com.TypeApi.service.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

@Controller
@RequestMapping(value = "/raffle")
public class RaffleController {

    @Autowired
    private UserlogService userlogService;

    @Autowired
    private RaffleService raffleService;

    @Autowired
    private PaylogService paylogService;

    @Autowired
    private UsersService usersService;

    @Autowired
    private ApiconfigService apiconfigService;

    @Autowired
    private Reward_logService reward_logService;

    @Autowired
    private InboxService inboxService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    RedisHelp redisHelp = new RedisHelp();
    ResultAll Result = new ResultAll();

    // 添加奖品
    @RequestMapping(value = "/raffleAdd")
    @ResponseBody
    public String raffleAdd(@RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "chance", required = false) Float chance,
            @RequestParam(value = "image", required = false) String image,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "quantity", required = false) Integer quantity,
            @RequestParam(value = "value", required = false) Integer value,
            @RequestParam(value = "expiry_date", required = false) Date expiry_date,
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
                return Result.getResultJson(201, "请输入奖品名称", null);
            if (quantity == null || quantity == 0)
                return Result.getResultJson(201, "请输入奖品数量", null);
            if (value == null || value == 0)
                return Result.getResultJson(201, "请输入奖品价值", null);
            long timestamp = System.currentTimeMillis();
            Raffle raffle = new Raffle();
            raffle.setName(name);
            raffle.setType(type);
            raffle.setChance(chance);
            raffle.setImage(image);
            raffle.setDescription(description);
            raffle.setQuantity(quantity);
            raffle.setValue(value);
            raffle.setExpiry_date(expiry_date);
            raffle.setCreated_at((int) (timestamp / 1000));
            raffleService.insert(raffle);
            return Result.getResultJson(200, "添加成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }

    }

    // 查询奖品
    @RequestMapping(value = "/raffleQuery")
    @ResponseBody
    public String raffleQuery(@RequestParam(value = "id", required = false) Integer id,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
            HttpServletRequest request) {
        try {

            String token = request.getHeader("Authorization");
            Users user = getUser(token);
            if (user.getUid() == null || user.toString().isEmpty())
                return Result.getResultJson(201, "用户不存在", null);
            if (!permission(user)) {
                return Result.getResultJson(201, "无权限", null);
            }
            JSONObject query = new JSONObject();
            Raffle info = new Raffle();
            info.setId(id);
            if (id != null) {
                info.setId(id);
                System.out.println(info);
            }
            List<Raffle> raffle = raffleService.selectList(info);
            int total = raffleService.total(info);
            JSONObject noData = new JSONObject();
            noData.put("code", 200);
            noData.put("msg", "");
            noData.put("data", raffle);
            noData.put("total", total);
            return noData.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }

    }

    // 修改奖品
    @RequestMapping(value = "/raffleUpdate")
    @ResponseBody
    public String raffleUpdate(@RequestParam(value = "id", required = false) Integer id,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "chance", required = false) Float chance,
            @RequestParam(value = "image", required = false) String image,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "quantity", required = false) Integer quantity,
            @RequestParam(value = "value", required = false) Integer value,
            @RequestParam(value = "expiry_date", required = false) Date expiry_date,
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
                return Result.getResultJson(201, "请输入奖品名称", null);
            if (quantity == null)
                return Result.getResultJson(201, "请输入奖品数量", null);
            if (value == null)
                return Result.getResultJson(201, "请输入奖品价值", null);
            long timestamp = System.currentTimeMillis();
            Raffle raffle = new Raffle();
            raffle.setId(id);
            raffle.setName(name);
            raffle.setType(type);
            raffle.setChance(chance);
            raffle.setImage(image);
            raffle.setDescription(description);
            raffle.setQuantity(quantity);
            raffle.setValue(value);
            raffle.setExpiry_date(expiry_date); 
            raffleService.update(raffle);
            return Result.getResultJson(200, "修改成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    // 抽奖
    @RequestMapping(value = "/raffle")
    @ResponseBody
    public String raffle(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Users user = getUser(token);

            if (user.getUid() == null || user.toString().isEmpty())
                return Result.getResultJson(201, "用户不存在", null);

            // 获取奖品数据库
            long timestamp = System.currentTimeMillis() / 1000;
            List<Raffle> prizes = new ArrayList<>();

            List<Raffle> raffle = raffleService.selectList(new Raffle());
            JSONObject noData = new JSONObject();
            for (Raffle r : raffle) {
                if (r.getQuantity() != 0 && r.getChance() != 0) {
                    prizes.add(r);
                }
            }
            Raffle winner = new Raffle();
            double rand = new Random().nextDouble() * 100;
            double sum = 0.0;

            for (Raffle prize : prizes) {
                sum += prize.getChance();
                if (rand < sum) {
                    winner = prize;
                }
            }
            if (winner.getId() != null && winner.getId() != 0) {
                // 扣除奖品数量
                winner.setQuantity(winner.getQuantity() - 1);
                raffleService.update(winner);
                Raffle r = raffleService.selectByKey(winner.getId());
                // 积分
                if (Objects.equals(r.getType(), "point")) {
                    // 添加日志
                    Paylog paylog = new Paylog();
                    paylog.setUid(user.getUid());
                    paylog.setCreated((int) timestamp);
                    paylog.setPaytype("raffle");
                    paylog.setSubject("抽奖奖励");
                    paylog.setTotalAmount(String.valueOf(r.getValue()));
                    paylogService.insert(paylog);
                    // 添加积分
                    user.setAssets(user.getAssets() + r.getValue());
                    usersService.update(user);
                    // vip
                } else if (Objects.equals(r.getType(), "vip")) {
                    if (user.getVip() > System.currentTimeMillis() / 1000) {
                        user.setVip(user.getVip() + (86400 * r.getValue()));
                    } else {
                        user.setVip((int) (System.currentTimeMillis() / 1000 + (86400 * r.getValue())));
                    }
                    usersService.update(user);
                    // 实物
                } else if (Objects.equals(r.getType(), "product")) {
                    // 发送消息给管理员
                    Users users = new Users();
                    users.setGroup("administrator");
                    List<Users> userList = usersService.selectList(users);
                    for (int i = 0; i < userList.size(); i++) {
                        Integer uid = userList.get(i).getUid();
                        Inbox insert = new Inbox();
                        insert.setUid(uid);
                        insert.setTouid(uid);
                        insert.setType("system");
                        insert.setText("用户" + user.getName() + "抽中了" + r.getName() + "，请尽快发货");
                        insert.setCreated((int) timestamp);
                        inboxService.insert(insert);
                    }
                }
                // 添加中奖记录
                Reward_log reward_log = new Reward_log();
                reward_log.setUid(user.getUid());
                reward_log.setName(r.getName());
                reward_log.setReward_id(r.getId());
                reward_log.setDescription(r.getDescription());
                reward_log.setExpired(r.getExpiry_date());
                reward_log.setCreated((int) (timestamp));
                reward_logService.insert(reward_log);

                // 添加消息
                Inbox inbox = new Inbox();
                inbox.setText("恭喜您抽中了" + r.getName());
                inbox.setUid(1);
                inbox.setTouid(user.getUid());
                inbox.setType("system");
                inbox.setIsread(0);
                inbox.setValue(0);
                inboxService.insert(inbox);

                noData.put("code", 200);
                noData.put("msg", "恭喜您抽中了" + r.getName());
                noData.put("data", r);
                return noData.toString();
            } else {
                noData.put("code", 201);
                noData.put("msg", "很遗憾，您没有抽中奖品");
                noData.put("data", null);
                return noData.toString();
            }
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