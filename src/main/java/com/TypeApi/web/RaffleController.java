package com.TypeApi.web;

import com.TypeApi.common.*;
import com.TypeApi.entity.*;
import com.TypeApi.service.*;
import com.alibaba.fastjson.JSONObject;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

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

    @Value("${web.prefix}")
    private String dataprefix;

    @Value("${mybatis.configuration.variables.prefix}")
    private String prefix;

    RedisHelp redisHelp = new RedisHelp();
    ResultAll Result = new ResultAll();
    UserStatus UStatus = new UserStatus();
    baseFull baseFull = new baseFull();

    // 添加奖品
    @RequestMapping(value = "/add")
    @ResponseBody
    public String raffleAdd(@RequestParam(value = "name") String name,
                            @RequestParam(value = "type") String type,
                            @RequestParam(value = "chance", required = false) Float chance,
                            @RequestParam(value = "image", required = false) String image,
                            @RequestParam(value = "description", required = false) String description,
                            @RequestParam(value = "quantity", required = false) Integer quantity,
                            @RequestParam(value = "value", required = false) Integer value,
                            @RequestParam(value = "expiry_date", required = false) Integer expiry_date,
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
    @RequestMapping(value = "/list")
    @ResponseBody
    public String list(@RequestParam(value = "id", required = false) Integer id,
                       @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                       @RequestParam(value = "limit", required = false, defaultValue = "16 ") Integer limit,
                       HttpServletRequest request) {
        try {
            PageList<Raffle> rafflePageList = raffleService.selectPage(new Raffle(), page, limit, "");
            List<Raffle> raffleList = rafflePageList.getList();
            int total = raffleService.total(new Raffle());
            Map<String, Object> data = new HashMap<>();
            data.put("count", raffleList.size());
            data.put("total", total);
            data.put("data", raffleList);
            return Result.getResultJson(200, "获取成功", data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }

    }

    // 修改奖品
    @RequestMapping(value = "/update")
    @ResponseBody
    public String raffleUpdate(@RequestParam(value = "id") Integer id,
                               @RequestParam(value = "name", required = false) String name,
                               @RequestParam(value = "type", required = false) String type,
                               @RequestParam(value = "chance", required = false) Float chance,
                               @RequestParam(value = "image", required = false) String image,
                               @RequestParam(value = "description", required = false) String description,
                               @RequestParam(value = "quantity", required = false) Integer quantity,
                               @RequestParam(value = "value", required = false) Integer value,
                               @RequestParam(value = "expiry_date", required = false) Integer expiry_date,
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
            Apiconfig apiconfig = UStatus.getConfig(this.dataprefix, apiconfigService, redisTemplate);
            String token = request.getHeader("Authorization");
            Users user = getUser(token);

            if (user.getUid() == null || user.toString().isEmpty())
                return Result.getResultJson(201, "用户不存在", null);

            // 最多每天抽多少次
            String temp = redisHelp.getRedis("raffle_" + user.getName(), redisTemplate);
            int num = 1;
            if (temp != null) num = Integer.parseInt(temp);
            if (num > apiconfig.getRaffleNum())
                return Result.getResultJson(201, "抽奖次数已达上限", null);

            // 判断用户积分是否足够
            if (user.getAssets() == null || user.getAssets() < apiconfig.getRaffleCoin())
                return Result.getResultJson(201, "积分不足", null);

            // 开始扣除积分
            user.setAssets(user.getAssets() - apiconfig.getRaffleCoin());
            usersService.update(user);

            // 获取奖品数据库
            long timestamp = System.currentTimeMillis() / 1000;
            List<Raffle> prizes = new ArrayList<>();

            List<Raffle> raffle = raffleService.selectList(new Raffle());
            for (Raffle r : raffle) {
                if (r.getQuantity() != 0 && r.getChance() != 0) {
                    prizes.add(r);
                }
            }
            Raffle winner = new Raffle();
            double rand = new Random().nextDouble() * prizes.stream().mapToDouble(Raffle::getChance).sum();
            double sum = 0.0;

            for (Raffle prize : prizes) {
                sum += prize.getChance();
                if (rand < sum) {
                    winner = prize;
                    break; // 找到中奖奖品后立即结束循环
                }
            }
            winner.setQuantity(winner.getQuantity() - 1);
            raffleService.update(winner);
            Raffle r = raffleService.selectByKey(winner.getId());
            // 积分
            if (Objects.equals(r.getType(), "point") && r.getValue() != 0) {
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
            reward_log.setType(r.getType());
            reward_log.setStatus(r.getType().equals("point") || r.getType().equals("vip")?"issued":"pending");
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

            JSONObject data = new JSONObject();
            data.put("code", 200);
            data.put("msg", r.getValue() > 0 ? "恭喜你抽中" + r.getName() : "谢谢惠顾");
            data.put("data", r);
            redisHelp.delete("raffle_" + user.getName(), redisTemplate);
            redisHelp.setRedis("raffle_" + user.getName(), String.valueOf(num + 1), baseFull.endTime(), redisTemplate);
            return data.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    @RequestMapping(value = "/log")
    @ResponseBody
    public String log(@RequestParam(value = "type", required = false, defaultValue = "point") String type,
                      @RequestParam(value = "status", required = false, defaultValue = "issued") String status,
                      @RequestParam(value = "id", required = false) Integer id,
                      @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                      @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
                      @RequestParam(value = "all",required = false,defaultValue = "0") Integer all,
                      HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Users user = getUser(token);
            Boolean permission = permission(user);
            if (user.getUid() == null) return Result.getResultJson(201, "用户不存在", null);
            if (!permission && id != null) return Result.getResultJson(201, "无权限", null);

            Reward_log reward_log = new Reward_log();
            reward_log.setType(type);
            reward_log.setStatus(status);
            reward_log.setUid(permission? id!=null ? id: user.getUid() : user.getUid());
            if(permission && all.equals(1)) reward_log.setUid(null);

            PageList<Reward_log> rewardLogPageList = reward_logService.selectPage(reward_log, page, limit, "");
            List<Reward_log> rewardLogList = rewardLogPageList.getList();
            List dataList = new ArrayList<>();
            if(permission(user) && all.equals(1)){
                for(Reward_log _rewardLog :rewardLogList){
                    Map<String,Object> data = JSONObject.parseObject(JSONObject.toJSONString(_rewardLog),Map.class);
                    JSONObject address = new JSONObject();
                    // 查询出用户的数据
                    Users rewardUser = usersService.selectByKey(_rewardLog.getUid());
                    if(rewardUser.getAddress()!=null && !rewardUser.getAddress().toString().isEmpty()){
                        address = JSONObject.parseObject(rewardUser.getAddress());
                    }

                    // 取出用户基础信息 screenName name address
                    JSONObject userInfo = new JSONObject();
                    userInfo.put("name",rewardUser.getName());
                    userInfo.put("screenName",rewardUser.getScreenName());
                    userInfo.put("address",address);
                    data.put("userInfo",userInfo);
                    dataList.add(data);
                }
            }else{
                dataList = rewardLogList;
            }
            Map<String, Object> data = new HashMap<>();
            data.put("count", rewardLogList.size());
            data.put("total", reward_logService.total(reward_log));
            data.put("data", dataList);
            data.put("page", page);
            return Result.getResultJson(200, "获取成功", data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    @RequestMapping(value = "/track")
    @ResponseBody
    public String track(@RequestParam(value = "id") Integer id,
                        @RequestParam(value = "tracking_number") String tracking_number,
                        HttpServletRequest request){
        try{
            String token = request.getHeader("Authorization");
            Users user = getUser(token);
            if(!permission(user)) return Result.getResultJson(201,"无权限",null);

            Reward_log reward_log = reward_logService.selectByKey(id);
            if(reward_log.getId()==null) return Result.getResultJson(201,"奖品记录不存在",null);
            reward_log.setTracking_number(tracking_number);
            reward_log.setStatus("issued");
            reward_logService.update(reward_log);
            // 给中奖用户发消息
            Inbox inbox = new Inbox();
            inbox.setText("你的奖品"+ reward_log.getName()+"已发货，请注意物流信息；物流单号："+tracking_number);
            inbox.setTouid(reward_log.getUid());
            inbox.setIsread(0);
            inbox.setCreated((int)System.currentTimeMillis()/1000);
            inbox.setType("system");
            inboxService.insert(inbox);
            return Result.getResultJson(200,"发货成功",null);
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(400,"接口异常",null);
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

            Raffle raffle = raffleService.selectByKey(id);
            if (raffle.getId() == null || raffle.toString().isEmpty())
                return Result.getResultJson(201, "数据不存在", null);

            raffleService.delete(id);
            return Result.getResultJson(200, "删除成功", null);

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