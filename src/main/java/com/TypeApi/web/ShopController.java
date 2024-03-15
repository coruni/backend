package com.TypeApi.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.TypeApi.entity.*;
import com.TypeApi.common.*;
import com.TypeApi.service.*;
import com.auth0.jwt.interfaces.DecodedJWT;
import net.dreamlu.mica.xss.core.XssCleanIgnore;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 控制层
 * TypechoShopController
 *
 * @author maplene
 * @date 2024/01/06
 */
@Controller
@RequestMapping(value = "/shop")
public class ShopController {

    @Autowired
    ShopService service;

    @Autowired
    private ShoptypeService shoptypeService;

    @Autowired
    private UsersService usersService;

    @Autowired
    private UserlogService userlogService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MailService MailService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private PaylogService paylogService;

    @Autowired
    private ApiconfigService apiconfigService;

    @Autowired
    private InboxService inboxService;


    @Autowired
    private OrderService orderService;


    @Autowired
    private PushService pushService;

    @Value("${web.prefix}")
    private String dataprefix;

    RedisHelp redisHelp = new RedisHelp();
    ResultAll Result = new ResultAll();
    UserStatus UStatus = new UserStatus();
    EditFile editFile = new EditFile();
    baseFull baseFull = new baseFull();

    /***
     * 商品列表
     */
    @RequestMapping(value = "/list")
    @ResponseBody
    public String list(@RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                       @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
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

            // 查询商品
            Shop shop = new Shop();
            if (params != null && !params.isEmpty())
                shop = JSONObject.parseObject(params, Shop.class);

            PageList<Shop> shopPageList = service.selectPage(shop, page, limit, searchKey, order);
            List<Shop> shopList = shopPageList.getList();
            JSONArray dataList = new JSONArray();
            for (Shop _shop : shopList) {
                Map<String, Object> data = JSONObject.parseObject(JSONObject.toJSONString(_shop), Map.class);
                // 格式化商品图片和 specs
                JSONArray imgurl = new JSONArray();
                JSONArray specs = new JSONArray();
                imgurl = _shop.getImgurl() != null && !_shop.getImgurl().toString().isEmpty() ? JSONArray.parseArray(_shop.getImgurl().toString()) : null;
                specs = _shop.getSpecs() != null && !_shop.getSpecs().toString().isEmpty() ? JSONArray.parseArray(_shop.getSpecs().toString()) : null;
                // 加入用户信息
                Users bossInfo = usersService.selectByKey(_shop.getUid());
                Map<String, Object> dataBossInfo = JSONObject.parseObject(JSONObject.toJSONString(bossInfo), Map.class);
                dataBossInfo.remove("address");
                dataBossInfo.remove("password");
                dataBossInfo.remove("head_picture");
                dataBossInfo.remove("opt");
                data.put("imgurl", imgurl);
                data.put("specs", specs);
                data.put("bossInfo", dataBossInfo);
                dataList.add(data);
            }
            Map<String, Object> data = new HashMap<>();
            data.put("page", page);
            data.put("limit", limit);
            data.put("data", dataList);
            data.put("count", dataList.size());
            data.put("total", service.total(shop, searchKey));
            return Result.getResultJson(200, "获取成功", data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /**
     * 查询商品详情
     */
    @RequestMapping(value = "/info")
    @ResponseBody
    public String shopInfo(@RequestParam(value = "id", required = false) String id,
                           HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
            }

            Shop shop = service.selectByKey(id);
            if (shop == null || shop.toString().isEmpty()) return Result.getResultJson(201, "商品不存在", null);

            // 格式化
            Map<String, Object> data = JSONObject.parseObject(JSONObject.toJSONString(shop), Map.class);
            // 格式化商品图片和 specs
            JSONArray imgurl = new JSONArray();
            JSONArray specs = new JSONArray();
            imgurl = shop.getImgurl() != null && !shop.getImgurl().toString().isEmpty() ? JSONArray.parseArray(shop.getImgurl().toString()) : null;
            specs = shop.getSpecs() != null && !shop.getSpecs().toString().isEmpty() ? JSONArray.parseArray(shop.getSpecs().toString()) : null;

            // 加入用户信息
            Users bossInfo = usersService.selectByKey(shop.getUid());
            Map<String, Object> dataBossInfo = JSONObject.parseObject(JSONObject.toJSONString(bossInfo), Map.class);
            dataBossInfo.remove("address");
            dataBossInfo.remove("password");
            dataBossInfo.remove("head_picture");
            dataBossInfo.remove("opt");
            data.put("imgurl", imgurl);
            data.put("specs", specs);
            data.put("bossInfo", dataBossInfo);

            return Result.getResultJson(200, "获取成功", data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     * 添加商品
     */
    @XssCleanIgnore
    @RequestMapping(value = "/add")
    @ResponseBody
    public String add(@RequestParam(value = "title") String title,
                      @RequestParam(value = "text") String text,
                      @RequestParam(value = "specs") String specs,
                      @RequestParam(value = "images") String images,
                      @RequestParam(value = "freight", required = false, defaultValue = "0") Integer freight,
                      @RequestParam(value = "sort") Integer sort,
                      @RequestParam(value = "price") Integer price,
                      @RequestParam(value = "num") Integer num,
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

            Shop shop = new Shop();
            Boolean permission = permission(request.getHeader("Authorization"));
            shop.setStatus(1);
            if (!permission) shop.setStatus(0);
            shop.setCreated(Math.toIntExact(timeStamp));
            shop.setTitle(title);
            shop.setText(text);
            shop.setImgurl(images);
            shop.setFreight(freight);
            shop.setSort(sort);
            shop.setPrice(price);
            shop.setNum(num);
            shop.setSpecs(specs);
            shop.setUid(user.getUid());
            service.insert(shop);
            return Result.getResultJson(200, permission ? "添加成功" : "请等待审核", null);
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

    @XssCleanIgnore
    @RequestMapping(value = "/update")
    @ResponseBody
    public String add(@RequestParam(value = "id") Integer id,
                      @RequestParam(value = "title", required = false) String title,
                      @RequestParam(value = "text", required = false) String text,
                      @RequestParam(value = "specs", required = false) String specs,
                      @RequestParam(value = "images", required = false) String images,
                      @RequestParam(value = "freight", required = false, defaultValue = "0") Integer freight,
                      @RequestParam(value = "sort", required = false) Integer sort,
                      @RequestParam(value = "price", required = false) Integer price,
                      @RequestParam(value = "num", required = false) Integer num,
                      HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user == null || user.toString().isEmpty()) return Result.getResultJson(201, "用户不存在", null);
            }
            // 查询商品
            Shop shop = service.selectByKey(id);
            if (shop == null || shop.toString().isEmpty()) return Result.getResultJson(201, "商品不存在", null);
            if (!permission(request.getHeader("Authorization")) && !shop.getUid().equals(user.getUid()))
                return Result.getResultJson(201, "无权限", null);

            // 更新信息
            shop.setTitle(title);
            shop.setText(text);
            shop.setImgurl(images);
            shop.setFreight(freight);
            shop.setSort(sort);
            shop.setPrice(price);
            shop.setNum(num);
            shop.setSpecs(specs);

            service.update(shop);

            return Result.getResultJson(200, "更新成功", null);
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
    public String delete(@RequestParam(value = "id") Integer id,
                         HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user == null || user.toString().isEmpty()) return Result.getResultJson(201, "用户不存在", null);
            }
            Shop shop = service.selectByKey(id);
            if (shop == null || shop.toString().isEmpty()) return Result.getResultJson(201, "商品不存在", null);
            if (!permission(request.getHeader("Authorization")) && !shop.getUid().equals(user.getUid()))
                return Result.getResultJson(201, "无权限", null);

            service.delete(id);
            return Result.getResultJson(200, "删除成功", null);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     * 生成的订单号
     */
    @RequestMapping(value = "/genOrder")
    @ResponseBody
    public String genOrder(@RequestParam(value = "product") Integer product,
                           @RequestParam(value = "specs") Integer specs,
                           @RequestParam(value = "address") String address,
                           HttpServletRequest request) {
        try {
            Apiconfig apiconfig = UStatus.getConfig(dataprefix, apiconfigService, redisTemplate);
            // 验证用户登录状态
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user == null || user.toString().isEmpty()) return Result.getResultJson(201, "用户不存在", null);
            }
            // 获取到商品信息

            Shop userOrder = service.selectByKey(product);
            JSONObject spcesInfo = new JSONObject();
            // 不允许店主本人购买
            if (userOrder.getUid().equals(user.getUid())) {
                return Result.getResultJson(0, "不允许购买自己的商品", null);
            }

            // 将 specs 的 JSON 字符串转换为对象列表

            JSONArray spcesList = JSONArray.parseArray(userOrder.getSpecs());
            for (int i = 0; i < spcesList.size(); i++) {
                JSONObject obj = spcesList.getJSONObject(i);
                if (obj.getInteger("id") != null && obj.getInteger("id") == specs) {
                    spcesInfo = obj;
                }
            }

            // 通过验证 写入商品信息
            Order newData = new Order();
            // 先计算价格，VIP价格以及商品打折
            Boolean isVip = false;
            Integer price = 0;
            long timeStamp = System.currentTimeMillis();
            long currentTime = timeStamp / 1000;
            if (user.getVip() > currentTime) isVip = true;
            // 如果是VIP就开始计算折扣 商品折扣大于系统全局折扣 否则就不打折
            if (isVip) {
                // 商品折扣优先级大于系统折扣 先判断商品折扣是否小于1
                if (Float.parseFloat(userOrder.getVipDiscount()) < 1) {
                    // 先判断specs中有没有设置价格
                    if (spcesInfo.getInteger("price") > 0) {
                        price = (int) (spcesInfo.getInteger("price") * Float.parseFloat(userOrder.getVipDiscount()) + userOrder.getFreight());
                    } else {
                        price = (int) (userOrder.getPrice() * Float.parseFloat(userOrder.getVipDiscount()) + userOrder.getFreight());
                    }
                } else {
                    if (spcesInfo.getInteger("price") > 0) {
                        price = (int) (spcesInfo.getInteger("price") * Float.parseFloat(apiconfig.getVipDiscount()) + userOrder.getFreight());
                    } else {
                        price = (int) (userOrder.getPrice() * Float.parseFloat(apiconfig.getVipDiscount()) + userOrder.getFreight());
                    }
                }
            } else {
                if (spcesInfo.getInteger("price") > 0) {
                    price = spcesInfo.getInteger("price") + userOrder.getFreight();

                } else {
                    price = userOrder.getPrice() + userOrder.getFreight();
                }
            }

            // 处理商品订单 使用系统时间戳+格式化时间
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            String orders = dateFormat.format(timeStamp) + timeStamp + user.getUid();

            // 整合信息准备新增数据
            newData.setOrders(orders);
            newData.setPrice(price);
            newData.setPaid(0);
            newData.setUser_id(user.getUid());
            newData.setBoss_id(userOrder.getUid());
            newData.setProduct(product);
            newData.setFreight(userOrder.getFreight());
            newData.setProduct_name(userOrder.getTitle());
            newData.setSpecs(spcesInfo.toString());
            newData.setCreated((int) (currentTime));
            newData.setAddress(address);

            orderService.insert(newData);
            Map<String, Object> data = new HashMap<>();
            data.put("orderId", newData.getId());
            return Result.getResultJson(200, "生成成功", data);
        } catch (Exception err) {
            err.printStackTrace();
            return Result.getResultJson(0, "接口异常", null);
        }
    }

    /***
     * 查询订单
     */
    @RequestMapping(value = "/order")
    @ResponseBody
    public String order(@RequestParam(value = "id") Integer id,
                        HttpServletRequest request) {
        try {
            Apiconfig apiconfig = UStatus.getConfig(this.dataprefix, apiconfigService, redisTemplate);
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user == null || user.toString().isEmpty()) return Result.getResultJson(201, "用户不存在", null);
            }
            // 验证订单所属
            Order orderInfo = orderService.selectByKey(id);
            if (orderInfo.toString().isEmpty()) {
                return Result.getResultJson(0, "订单不存在", null);
            }
            if (!user.getUid().equals(orderInfo.getUser_id()) &&!user.getUid().equals(orderInfo.getBoss_id())) {
                return Result.getResultJson(0, "你没有权限查看该订单", null);
            }

            Map<String, Object> data = new HashMap<>();
            Class<?> orderClass = orderInfo.getClass();
            Field[] fields = orderClass.getDeclaredFields();

            for (Field field : fields) {
                try {
                    field.setAccessible(true);
                    String fieldName = field.getName();
                    Object value = field.get(orderInfo);
                    data.put(fieldName, value);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            // 查找该订单商品的主图片
            Shop product = service.selectByKey(orderInfo.getProduct());
            // 格式化Array

            JSONArray product_image = JSONArray.parseArray(product.getImgurl());

            // 格式化Object
            JSONObject specs = JSONObject.parseObject(orderInfo.getSpecs());
            JSONObject address = JSONObject.parseObject(orderInfo.getAddress());
            // 加入店主信息
            Users bossUser = usersService.selectByKey(orderInfo.getBoss_id());
            Map<String, Object> bossInfo = JSONObject.parseObject(JSONObject.toJSONString(bossUser));
            bossInfo.remove("address");
            bossInfo.remove("opt");
            bossInfo.remove("head_picture");
            // 返回信息
            data.put("bossInfo", bossInfo);
            data.put("product_image", product_image);
            data.put("address", address);
            data.put("specs", specs);
            return Result.getResultJson(200, "获取成功", data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     * 订单列表
     * @param type 0是购买订单 1是商家订单
     */
    @RequestMapping(value = "/orderList")
    @ResponseBody
    public String orderList(@RequestParam(value = "page", defaultValue = "1") Integer page,
                            @RequestParam(value = "limit", defaultValue = "15") Integer limit,
                            @RequestParam(value = "type", defaultValue = "0") Integer type,
                            @RequestParam(value = "params", required = false) String params,
                            @RequestParam(value = "seachKey", required = false) String searchKey,
                            @RequestParam(value = "order", defaultValue = "created desc") String order,
                            HttpServletRequest request) {
        try {
            Map userInfo = new HashMap<>();
            Apiconfig apiconfig = new Apiconfig();
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user == null || user.toString().isEmpty()) return Result.getResultJson(201, "用户不存在", null);
            }
            // 开始查询
            // 先将searchParams 格式化
            Order query = new Order();
            if (StringUtils.isNotBlank(params)) {
                query = JSONObject.parseObject(params).toJavaObject(Order.class);
                // query设置bossid和userid无效 只能在token获取
            }
            if (type.equals(1)) query.setBoss_id(user.getUid());
            else query.setUser_id(user.getUid());
            PageList<Order> orderList = orderService.selectPage(query, page, limit, searchKey, order);
            List<Order> list = orderList.getList();
            JSONArray arrayList = new JSONArray();
            for (Order _order : list) {
                Map info = JSONObject.parseObject(JSONObject.toJSONString(_order), Map.class);
                // 格式化Obecjt
                JSONObject address = JSONObject.parseObject(info.get("address").toString());
                JSONObject specs = JSONObject.parseObject(info.get("specs").toString());
                JSONArray product_image = JSONArray.parseArray(service.selectByKey(info.get("product").toString()).getImgurl());
                Users bossUser = usersService.selectByKey(Integer.parseInt(info.get("boss_id").toString()));
                Map<String, Object> bossInfo = JSONObject.parseObject(JSONObject.toJSONString(bossUser));
                bossInfo.remove("address");
                bossInfo.remove("opt");
                bossInfo.remove("password");
                bossInfo.remove("head_picture");
                // 如果是商家订单就查询购买者信息
                Users buyUser = usersService.selectByKey(_order.getUser_id());
                Map<String, Object> buyerInfo = JSONObject.parseObject(JSONObject.toJSONString(buyUser));
                buyerInfo.remove("address");
                buyerInfo.remove("opt");
                buyerInfo.remove("password");
                buyerInfo.remove("head_picture");

                info.put("buyerInfo",buyerInfo);
                info.put("bossInfo", bossInfo);
                info.put("address", address);
                info.put("specs", specs);
                info.put("product_image", product_image);
                arrayList.add(info);
            }

            Map data = new HashMap();
            data.put("data", arrayList);
            data.put("count", list.size());
            data.put("total", orderService.total(query));
            return Result.getResultJson(200, "获取成功", data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }

    }

    /***
     * 审核商品
     */
    @RequestMapping(value = "/auditShop")
    @ResponseBody
    public String auditShop(@RequestParam(value = "key", required = false) String key,
                            @RequestParam(value = "token", required = false) String token,
                            @RequestParam(value = "type", required = false) Integer type,
                            @RequestParam(value = "reason", required = false) String reason) {
        if (type == null) {
            type = 0;
        }
        try {
            Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
            if (uStatus == 0) {
                return Result.getResultJson(0, "用户未登录或Token验证失败", null);
            }
            // 查询发布者是不是自己，如果是管理员则跳过
            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            Integer uid = Integer.parseInt(map.get("uid").toString());
            String group = map.get("group").toString();
            Integer sid = Integer.parseInt(key);
            Shop info = service.selectByKey(sid);
            if (!group.equals("administrator") && !group.equals("editor")) {

                Integer aid = info.getUid();
                if (!aid.equals(uid)) {
                    return Result.getResultJson(0, "你无权进行此操作", null);
                }
            }
            Shop shop = new Shop();
            shop.setId(Integer.parseInt(key));
            if (type.equals(0)) {
                shop.setStatus(1);
            } else {
                if (reason == "" || reason == null) {
                    return Result.getResultJson(0, "请输入拒绝理由", null);
                }
                shop.setStatus(2);
            }
            Integer rows = service.update(shop);
            //根据过审状态发送不同的内容
            if (type.equals(0)) {
                //发送消息
                Long date = System.currentTimeMillis();
                String created = String.valueOf(date).substring(0, 10);
                Inbox insert = new Inbox();
                insert.setUid(uid);
                insert.setTouid(info.getUid());
                insert.setType("system");
                insert.setText("你的商品【" + info.getTitle() + "】已审核通过");
                insert.setCreated(Integer.parseInt(created));
                inboxService.insert(insert);
            } else {
                //发送消息
                Long date = System.currentTimeMillis();
                String created = String.valueOf(date).substring(0, 10);
                Inbox insert = new Inbox();
                insert.setUid(uid);
                insert.setTouid(info.getUid());
                insert.setType("system");
                insert.setText("你的商品【" + info.getTitle() + "】未审核通过。理由如下：" + reason);
                insert.setCreated(Integer.parseInt(created));
                inboxService.insert(insert);
            }


            editFile.setLog("管理员" + uid + "请求审核商品" + key);
            JSONObject response = new JSONObject();
            response.put("code", rows);
            response.put("msg", rows > 0 ? "操作成功" : "操作失败");
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口请求异常，请联系管理员", null);
        }

    }

    /***
     * 购买商品
     */
    @RequestMapping(value = "/buy")
    @ResponseBody
    public String buy(@RequestParam(value = "id", required = true) String id,
                      HttpServletRequest request) {

        try {
            String token = request.getHeader("Authorization");
            Users userInfo = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                userInfo = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (userInfo == null || userInfo.toString().isEmpty())
                    return Result.getResultJson(201, "用户不存在", null);
            }
            Apiconfig apiconfig = UStatus.getConfig(this.dataprefix, apiconfigService, redisTemplate);
            // 验证通过 获取用户信息
            Integer money = userInfo.getAssets();
            // 获取订单信息
            Order orderInfo = orderService.selectByKey(id);
            // 获取商品信息

            Shop product = service.selectByKey(orderInfo.getProduct());

            // 判断库存
            if (product.getNum() < 1) {
                return Result.getResultJson(201, "商品库存不足", null);
            }

            // 检测订单属于权限
            if (!userInfo.getUid().equals(orderInfo.getUser_id())) {
                return Result.getResultJson(202, "订单错误", null);
            }

            if (orderInfo.getPaid().equals(1)) {
                return Result.getResultJson(203, "该订单已支付", null);
            }

            // 获取OK之后判断用户余额是否足够
            Integer payStatus = 0;
            if (money >= orderInfo.getPrice()) {
                money -= orderInfo.getPrice();
                // 设置订单状态
                orderInfo.setPaid(1);
                // 更新用户财产
                userInfo.setAssets(money);
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
                String orders = dateFormat.format(System.currentTimeMillis()) + System.currentTimeMillis() / 1000 + userInfo.getUid();
                Integer orderStatus = orderService.update(orderInfo);
                Integer userStatus = usersService.update(userInfo);
                if (orderStatus > 0 && userStatus > 0) {
                    // 写入购买信息
                    Paylog payInfo = new Paylog();
                    payInfo.setStatus(1);
                    payInfo.setUid(userInfo.getUid());
                    payInfo.setTotalAmount(String.valueOf(orderInfo.getPrice() * -1));
                    payInfo.setSubject("购买" + orderInfo.getProduct_name());
                    payInfo.setPaytype("product");
                    payInfo.setOutTradeNo(orders + userInfo.getUid());
                    payInfo.setCreated((int) (System.currentTimeMillis()));
                    payStatus = paylogService.insert(payInfo);

                    // 更新出售数量

                    Integer sellNum = product.getSellNum() + 1;
                    Integer productNum = product.getNum() - 1;
                    product.setNum(productNum);
                    product.setSellNum(sellNum);
                    service.update(product);


                    // 给卖家加米
                    Users bossInfo = usersService.selectByKey(orderInfo.getBoss_id());
                    Integer bossMoney = bossInfo.getAssets() + orderInfo.getPrice();
                    bossInfo.setAssets(bossMoney);
                    Integer bossStatus = usersService.update(bossInfo);

                    if (bossStatus > 0) {
                        // 给商家发送消息 然后添加记录
                        Paylog bossLog = new Paylog();
                        bossLog.setOutTradeNo(orders + bossInfo.getUid());
                        bossLog.setUid(bossInfo.getUid());
                        bossLog.setStatus(1);
                        bossLog.setCreated((int) (System.currentTimeMillis() / 1000));
                        bossLog.setSubject("出售" + orderInfo.getProduct_name());
                        bossLog.setTotalAmount(String.valueOf(orderInfo.getPrice()));
                        paylogService.insert(bossLog);
                        // 站内通知
                        Inbox inbox = new Inbox();
                        inbox.setText("你的商品【" + orderInfo.getProduct_name() + "】有新订单");
                        inbox.setUid(userInfo.getUid());
                        inbox.setTouid(orderInfo.getBoss_id());
                        inbox.setType("finance");

                        // 通知栏消息
                        if (apiconfig.getIsPush().equals(1)) {
                            if (!bossInfo.getClientId().isEmpty()) {
                                try {
                                    pushService.sendPushMsg(bossInfo.getClientId(), "消息通知", "你有新的商品订单！", "payload", "finance");
                                } catch (Exception e) {
                                    System.err.println("通知发送失败：" + e);
                                }
                            }
                        }
                    }

                }

            } else {
                return Result.getResultJson(201, "余额不足", null);
            }
            return Result.getResultJson(200, "购买成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     * 商家修改订单号和地址
     */

    @RequestMapping(value = "/tracking")
    @ResponseBody
    public String tracking(@RequestParam(value = "id") Integer id,
                           @RequestParam(value = "price", required = false) Integer price,
                           @RequestParam(value = "address", required = false) String address,
                           @RequestParam(value = "tracking_number", required = false) String tracking_number,
                           @RequestParam(value = "isTracking", required = false) Integer isTracking,
                           HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Users userInfo = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                userInfo = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (userInfo == null || userInfo.toString().isEmpty())
                    return Result.getResultJson(201, "用户不存在", null);
            }
            // 判断订单boss_id是否为infoID
            Order orderInfo = orderService.selectByKey(id);
            if (!permission(request.getHeader("Authorization")) && !orderInfo.getBoss_id().equals(userInfo.getUid()) && !orderInfo.getUser_id().equals(userInfo.getUid())) {
                return Result.getResultJson(201, "你没有权限修改该订单", null);
            }
            Boolean isUser = false;
            if (orderInfo.getUser_id().equals(userInfo.getUid())) isUser = true;
            // 修改数值
            if (price != null && !isUser) {
                orderInfo.setPrice(price);
            }
            if (address != null && !address.isEmpty()) {
                //判断用户 如果已支付则不可修改地址
                if (isUser && orderInfo.getPaid().equals(1)) {
                    return Result.getResultJson(201, "已支付订单，不可修改地址，请联系卖家", null);
                }
                orderInfo.setAddress(address);
            }
            if (tracking_number != null && !tracking_number.isEmpty()) {
                orderInfo.setTracking_number(tracking_number);
            }
            if (isTracking != null && !isUser) {
                orderInfo.setIsTracking(isTracking);
            }

            // 状态返回信息
            orderService.update(orderInfo);

            return Result.getResultJson(200, "修改成功", null);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }


    @RequestMapping(value = "/vip")
    @ResponseBody
    public String vip(@RequestParam(value = "days") Integer days,
                      HttpServletRequest request) {
        try {
            Apiconfig apiconfig = UStatus.getConfig(dataprefix, apiconfigService, redisTemplate);
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user == null || user.toString().isEmpty())
                    return Result.getResultJson(201, "用户不存在", null);
            }
            Long timeStamp = System.currentTimeMillis() / 1000;
            // 是否是vip
            Boolean isVip = user.getVip() > timeStamp;

            // 计算价格
            Integer price = apiconfig.getVipPrice() * days;
            if (user.getAssets() < price) return Result.getResultJson(201, "余额不足", null);

            // 如果是会员就续期 如果不是就从当前时间开始计算
            if (isVip) user.setVip(user.getVip() + (86400 * days));
            else user.setVip((int) (timeStamp + (86400 * days)));

            //减除财产
            user.setAssets(user.getAssets() - price);

            // 处理商品订单 使用系统时间戳+格式化时间
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            String orders = dateFormat.format(timeStamp) + timeStamp + user.getUid();
            // 增加paylog
            Paylog pay = new Paylog();
            pay.setSubject("开通VIP");
            pay.setStatus(1);
            pay.setUid(user.getUid());
            pay.setPaytype("buyvip");
            pay.setOutTradeNo(orders);
            pay.setTotalAmount(String.valueOf(price * -1));
            pay.setCreated(Math.toIntExact(timeStamp));
            paylogService.insert(pay);
            usersService.update(user);
            return Result.getResultJson(200, "开通成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }


    /***
     *
     * @param name
     * @param intro
     * @param pic
     * @param parent
     * @param request
     * @return
     */
    @RequestMapping("/typeAdd")
    @ResponseBody
    public String typeAdd(@RequestParam(value = "name") String name,
                          @RequestParam(value = "intro") String intro,
                          @RequestParam(value = "pic") String pic,
                          @RequestParam(value = "parent") Integer parent,
                          HttpServletRequest request) {
        try {
            if (!permission(request.getHeader("Authorization"))) return Result.getResultJson(201, "无权限", null);
            // 查询parent是否存在
            Shoptype parentType = new Shoptype();
            if (parent != null && !parent.equals(0)) {
                parentType = shoptypeService.selectByKey(parent);
                if (parentType == null || parentType.toString().isEmpty())
                    return Result.getResultJson(201, "分类不存在", null);
            }

            Shoptype shoptype = new Shoptype();
            shoptype.setPic(pic);
            shoptype.setName(name);
            shoptype.setIntro(intro);
            shoptype.setParent(parentType.getId());

            shoptypeService.insert(shoptype);
            return Result.getResultJson(200, "添加成功", null);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);

        }
    }

    /***
     * 修改商品分类
     */
    @RequestMapping(value = "/typeEdit")
    @ResponseBody
    public String typeEdit(@RequestParam(value = "id") Integer id,
                           @RequestParam(value = "name", required = false) String name,
                           @RequestParam(value = "intro", required = false) String intro,
                           @RequestParam(value = "pic", required = false) String pic,
                           @RequestParam(value = "parent", required = false) Integer parent,
                           HttpServletRequest request) {
        try {
            if (!permission(request.getHeader("Authorization"))) return Result.getResultJson(201, "无权限", null);
            Shoptype shoptype = shoptypeService.selectByKey(id);
            if (shoptype == null || shoptype.toString().isEmpty()) return Result.getResultJson(201, "分类不存在", null);

            // 查询parent是否存在
            Shoptype parentType = new Shoptype();
            if (parent != null && !parent.equals(0)) {
                parentType = shoptypeService.selectByKey(parent);
                if (parentType == null || parentType.toString().isEmpty())
                    return Result.getResultJson(201, "分类不存在", null);
                shoptype.setParent(parentType.getParent());
            }
            shoptype.setName(name);
            shoptype.setIntro(intro);
            shoptype.setPic(pic);

            shoptypeService.update(shoptype);
            return Result.getResultJson(200, "编辑成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }

    }

    /***
     * 删除分类
     */
    @RequestMapping(value = "/typeDelete")
    @ResponseBody
    public String typeDelete(@RequestParam(value = "id") Integer id,
                             HttpServletRequest request) {
        try {
            if (!permission(request.getHeader("Authorization"))) return Result.getResultJson(201, "无权限", null);
            Shoptype shoptype = shoptypeService.selectByKey(id);
            if (shoptype == null || shoptype.toString().isEmpty()) return Result.getResultJson(201, "分类不存在", null);
            shoptypeService.delete(shoptype.getId());
            return Result.getResultJson(200, "删除成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }


    /***
     *
     * @param page
     * @param limit
     * @param request
     * @return
     */
    @RequestMapping(value = "/typeList")
    @ResponseBody
    public String typeList(@RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                           @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
                           @RequestParam(value = "order", required = false, defaultValue = "created desc") String order,
                           HttpServletRequest request) {
        try {
            Shoptype query = new Shoptype();
            PageList<Shoptype> shoptypePageList = shoptypeService.selectPage(query, page, limit, null, order);
            List<Shoptype> shoptypeList = shoptypePageList.getList();
            Map<String, Object> data = new HashMap<>();
            data.put("page", page);
            data.put("limit", limit);
            data.put("data", shoptypeList);
            data.put("count", shoptypeList.size());
            data.put("total", shoptypeService.total(query));
            return Result.getResultJson(200, "获取成功", data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     * 商品分类信息
     */
    @RequestMapping(value = "/typeInfo")
    @ResponseBody
    public String typeInfo(@RequestParam(value = "id") Integer id,
                           HttpServletRequest request) {
        try {
            Shoptype shoptype = shoptypeService.selectByKey(id);
            if (shoptype == null || shoptype.toString().isEmpty()) return Result.getResultJson(201, "分类不存在", null);

            Map<String, Object> data = JSONObject.parseObject(JSONObject.toJSONString(shoptype), Map.class);
            return Result.getResultJson(200, "获取成功", data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

}
