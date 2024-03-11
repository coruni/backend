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
@RequestMapping(value = "/pay")
public class PayController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UsersService usersService;


    @Autowired
    private PaylogService paylogService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private PaykeyService paykeyService;

    @Autowired
    private InboxService inboxService;


    @Autowired
    private ApiconfigService apiconfigService;

    @Value("${web.prefix}")
    private String dataprefix;

    @Value("${mybatis.configuration.variables.prefix}")
    private String prefix;

    RedisHelp redisHelp = new RedisHelp();
    ResultAll Result = new ResultAll();
    HttpClient HttpClient = new HttpClient();
    UserStatus UStatus = new UserStatus();


    /**
     * 支付宝扫码支付
     *
     * @return 支付宝生成的订单信息
     */
    @RequestMapping(value = "/scancodePay")
    @ResponseBody
    public String scancodepay(@RequestParam(value = "num", required = false) String num, @RequestParam(value = "token", required = false) String token) throws AlipayApiException {

        Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
        if (uStatus == 0) {
            return Result.getResultJson(0, "用户未登录或Token验证失败", null);
        }

        Pattern pattern = Pattern.compile("[0-9]*");
        if (!pattern.matcher(num).matches()) {
            return Result.getResultJson(0, "充值金额必须为正整数", null);
        }
        if (Integer.parseInt(num) <= 0) {
            return Result.getResultJson(0, "充值金额不正确", null);
        }

        Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
        Integer uid = Integer.parseInt(map.get("uid").toString());
        //登录情况下，恶意充值攻击拦截
        String isSilence = redisHelp.getRedis(this.dataprefix + "_" + uid + "_silence", redisTemplate);
        if (isSilence != null) {
            return Result.getResultJson(0, "你的操作太频繁了，请稍后再试", null);
        }
        String isRepeated = redisHelp.getRedis(this.dataprefix + "_" + uid + "_isRepeated", redisTemplate);
        if (isRepeated == null) {
            redisHelp.setRedis(this.dataprefix + "_" + uid + "_isRepeated", "1", 2, redisTemplate);
        } else {
            Integer frequency = Integer.parseInt(isRepeated) + 1;
            if (frequency == 3) {
                securityService.safetyMessage("用户ID：" + uid + "，在微信充值接口疑似存在攻击行为，请及时确认处理。", "system");
                redisHelp.setRedis(this.dataprefix + "_" + uid + "_silence", "1", 900, redisTemplate);
                return Result.getResultJson(0, "你的请求存在恶意行为，15分钟内禁止操作！", null);
            } else {
                redisHelp.setRedis(this.dataprefix + "_" + uid + "_isRepeated", frequency.toString(), 3, redisTemplate);
            }
            return Result.getResultJson(0, "你的操作太频繁了", null);
        }
        //攻击拦截结束

        Apiconfig apiconfig = UStatus.getConfig(this.dataprefix, apiconfigService, redisTemplate);

        final String APPID = apiconfig.getAlipayAppId();
        String RSA2_PRIVATE = apiconfig.getAlipayPrivateKey();
        String ALIPAY_PUBLIC_KEY = apiconfig.getAlipayPublicKey();

        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");//可以方便地修改日期格式
        String timeID = dateFormat.format(now);
        String order_no = timeID + "scancodealipay";
        String body = "";


        String total_fee = num;  //真实金钱

        AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do", APPID, RSA2_PRIVATE, "json",
                "UTF-8", ALIPAY_PUBLIC_KEY, "RSA2"); //获得初始化的AlipayClient
        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();//创建API对应的request类
        request.setBizContent("{" +
                "    \"out_trade_no\":\"" + order_no + "\"," +
                "    \"total_amount\":\"" + total_fee + "\"," +
                "    \"body\":\"" + body + "\"," +
                "    \"subject\":\"商品购买\"," +
                "    \"timeout_express\":\"90m\"}");//设置业务参数
        request.setNotifyUrl(apiconfig.getAlipayNotifyUrl());
        AlipayTradePrecreateResponse response = alipayClient.execute(request);//通过alipayClient调用API，获得对应的response类
        System.out.print(response.getBody());

        //根据response中的结果继续业务逻辑处理
        if (response.getMsg().equals("Success")) {
            //先生成订单
            Long date = System.currentTimeMillis();
            String created = String.valueOf(date).substring(0, 10);
            Paylog paylog = new Paylog();
            Integer TotalAmount = Integer.parseInt(total_fee) * apiconfig.getScale();
            paylog.setStatus(0);
            paylog.setCreated(Integer.parseInt(created));
            paylog.setUid(uid);
            paylog.setOutTradeNo(order_no);
            paylog.setTotalAmount(TotalAmount.toString());
            paylog.setPaytype("scancodePay");
            paylog.setSubject("扫码支付");
            paylogService.insert(paylog);
            //再返回二维码
            String qrcode = response.getQrCode();
            JSONObject toResponse = new JSONObject();
            toResponse.put("code", 1);
            toResponse.put("data", qrcode);
            toResponse.put("msg", "获取成功");
            return toResponse.toString();
        } else {
            JSONObject toResponse = new JSONObject();
            toResponse.put("code", 0);
            toResponse.put("data", "");
            toResponse.put("msg", "请求失败");
            return toResponse.toString();
        }

    }

    @RequestMapping(value = "/notify", method = RequestMethod.POST)
    @ResponseBody
    public String notify(HttpServletRequest request,
                         HttpServletResponse response) throws AlipayApiException {
        Map<String, String> params = new HashMap<String, String>();
        Map requestParams = request.getParameterMap();
        for (Iterator iter = requestParams.keySet().iterator(); iter.hasNext(); ) {
            String name = (String) iter.next();
            String[] values = (String[]) requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            params.put(name, valueStr);
        }
        System.err.println(params);
        Apiconfig apiconfig = UStatus.getConfig(this.dataprefix, apiconfigService, redisTemplate);
        String CHARSET = "UTF-8";
        //支付宝公钥
        String ALIPAY_PUBLIC_KEY = apiconfig.getAlipayPublicKey();

        String tradeStatus = request.getParameter("trade_status");
        boolean flag = AlipaySignature.rsaCheckV1(params, ALIPAY_PUBLIC_KEY, CHARSET, "RSA2");

        if (flag) {//验证成功

            if (tradeStatus.equals("TRADE_FINISHED") || tradeStatus.equals("TRADE_SUCCESS")) {
                //支付完成后，写入充值日志
                String trade_no = params.get("trade_no");
                String out_trade_no = params.get("out_trade_no");
                String total_amount = params.get("total_amount");
                Integer scale = apiconfig.getScale();
                Integer integral = Double.valueOf(total_amount).intValue() * scale;

                Long date = System.currentTimeMillis();
                String created = String.valueOf(date).substring(0, 10);
                Paylog paylog = new Paylog();
                //根据订单和发起人，是否有数据库对应，来是否充值成功
                paylog.setOutTradeNo(out_trade_no);
                paylog.setStatus(0);
                List<Paylog> logList = paylogService.selectList(paylog);
                if (logList.size() > 0) {
                    Integer pid = logList.get(0).getPid();
                    Integer uid = logList.get(0).getUid();
                    paylog.setStatus(1);
                    paylog.setTradeNo(trade_no);
                    paylog.setPid(pid);
                    paylog.setCreated(Integer.parseInt(created));
                    paylogService.update(paylog);
                    //订单修改后，插入用户表
                    Users users = usersService.selectByKey(uid);
                    Integer oldAssets = users.getAssets();
                    Integer assets = oldAssets + integral;
                    users.setAssets(assets);
                    usersService.update(users);
                } else {
                    System.err.println("数据库不存在订单");
                    return "fail";
                }
            }
            return "success";
        } else {//验证失败
            return "fail";
        }
    }

    /**
     * 二维码生成
     */
    @RequestMapping(value = "/qrCode")
    @ResponseBody
    public void getQRCode(String codeContent, @RequestParam(value = "token", required = false) String token, HttpServletResponse response) {
        Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
        if (uStatus == 0) {
            System.err.println("用户未的登陆");
        }
        System.out.println("codeContent=" + codeContent);
        try {
            /*
             * 调用工具类生成二维码并输出到输出流中
             */
            QRCodeUtil.createCodeToOutputStream(codeContent, response.getOutputStream());
            System.out.println("成功生成二维码!");
        } catch (IOException e) {
            System.out.println("发生错误");
        }
    }

    /**
     * 充值记录
     */
    @RequestMapping(value = "/list")
    @ResponseBody
    public String list(@RequestParam(value = "page", defaultValue = "1", required = false) Integer page,
                       @RequestParam(value = "limit", defaultValue = "15", required = false) Integer limit,
                       HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user == null || user.toString().isEmpty()) return Result.getResultJson(201, "用户不存在", null);
            }
            // 查询列表
            Paylog paylog = new Paylog();
            paylog.setUid(user.getUid());
            PageList<Paylog> paylogPageList = paylogService.selectPage(paylog, page, limit);
            List<Paylog> paylogList = paylogPageList.getList();

            Map<String, Object> data = new HashMap<>();
            data.put("page", page);
            data.put("limit", limit);
            data.put("data", paylogList);
            data.put("count", paylogList.size());
            data.put("total", paylogService.total(paylog));

            return Result.getResultJson(200, "获取成功", data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }

    }

    /**
     * 财务记录(管理员)
     */
    @RequestMapping(value = "/financeList")
    @ResponseBody
    public String financeList(@RequestParam(value = "searchParams", required = false) String searchParams,
                              @RequestParam(value = "token", required = false) String token,
                              @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                              @RequestParam(value = "limit", required = false, defaultValue = "15") Integer limit) {

        Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
        if (uStatus == 0) {
            return Result.getResultJson(0, "用户未登录或Token验证失败", null);
        }
        Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
        String group = map.get("group").toString();
        if (!group.equals("administrator")) {
            return Result.getResultJson(0, "你没有操作权限", null);
        }
        Integer total = 0;
        Paylog query = new Paylog();
        if (StringUtils.isNotBlank(searchParams)) {
            JSONObject object = JSON.parseObject(searchParams);
            query = object.toJavaObject(Paylog.class);
            total = paylogService.total(query);
        }
        PageList<Paylog> pageList = paylogService.selectPage(query, page, limit);
        List<Paylog> list = pageList.getList();
        JSONObject response = new JSONObject();
        response.put("code", 1);
        response.put("msg", "");
        response.put("data", null != list ? list : new JSONArray());
        response.put("count", list.size());
        response.put("total", total);
        return response.toString();
    }

    /**
     * 财务统计(管理员)
     */
    @RequestMapping(value = "/financeTotal")
    @ResponseBody
    public String financeTotal(@RequestParam(value = "token", required = false) String token) {
        Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
        if (uStatus == 0) {
            return Result.getResultJson(0, "用户未登录或Token验证失败", null);
        }
        Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
        String group = map.get("group").toString();
        if (!group.equals("administrator")) {
            return Result.getResultJson(0, "你没有操作权限", null);
        }
        Map financeData = new HashMap<String, Integer>();
        Integer recharge = jdbcTemplate.queryForObject("SELECT SUM(total_amount) FROM `" + prefix + "_paylog` where `status` = 1 and (`subject` = '扫码支付' or `subject` = '微信APP支付' or `subject` = '卡密充值' or `subject` = '系统充值');", Integer.class);
        Integer trade = jdbcTemplate.queryForObject("SELECT SUM(total_amount) FROM `" + prefix + "_paylog` where `status` = 1 and (`paytype` = 'buyshop' or `paytype` = 'buyvip' or `paytype` = 'toReward' or `paytype` = 'buyAds');", Integer.class);
        Integer withdraw = jdbcTemplate.queryForObject("SELECT SUM(total_amount) FROM `" + prefix + "_paylog` where `status` = 1 and (`paytype` = 'withdraw' or `subject` = '系统扣款');", Integer.class);
        Integer income = jdbcTemplate.queryForObject("SELECT SUM(total_amount) FROM `" + prefix + "_paylog` where `status` = 1 and (`paytype` = 'clock' or `paytype` = 'sellshop' or `paytype` = 'reward' or `paytype` = 'adsGift');", Integer.class);
        if (trade != null) {
            trade = trade * -1;
        }
        if (withdraw != null) {
            withdraw = withdraw * -1;
        }
        financeData.put("recharge", recharge);
        financeData.put("trade", trade);
        financeData.put("withdraw", withdraw);
        financeData.put("income", income);
        JSONObject response = new JSONObject();
        response.put("code", 1);
        response.put("data", financeData);
        response.put("msg", "");
        return response.toString();
    }

    /**
     * 微信支付
     */
    @RequestMapping(value = "/WxPay")
    @ResponseBody
    public String wxAdd(HttpServletRequest request, @RequestParam(value = "price", required = false) Integer price, @RequestParam(value = "token", required = false) String token) throws Exception {
        Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
        if (uStatus == 0) {
            return Result.getResultJson(0, "用户未登录或Token验证失败", null);
        }
        Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
        Integer uid = Integer.parseInt(map.get("uid").toString());
        //登录情况下，恶意充值攻击拦截
        String isSilence = redisHelp.getRedis(this.dataprefix + "_" + uid + "_silence", redisTemplate);
        if (isSilence != null) {
            return Result.getResultJson(0, "你的操作太频繁了，请稍后再试", null);
        }
        String isRepeated = redisHelp.getRedis(this.dataprefix + "_" + uid + "_isRepeated", redisTemplate);
        if (isRepeated == null) {
            redisHelp.setRedis(this.dataprefix + "_" + uid + "_isRepeated", "1", 2, redisTemplate);
        } else {
            Integer frequency = Integer.parseInt(isRepeated) + 1;
            if (frequency == 3) {
                securityService.safetyMessage("用户ID：" + uid + "，在微信充值接口疑似存在攻击行为，请及时确认处理。", "system");
                redisHelp.setRedis(this.dataprefix + "_" + uid + "_silence", "1", 900, redisTemplate);
                return Result.getResultJson(0, "你的请求存在恶意行为，15分钟内禁止操作！", null);
            } else {
                redisHelp.setRedis(this.dataprefix + "_" + uid + "_isRepeated", frequency.toString(), 3, redisTemplate);
            }
            return Result.getResultJson(0, "你的操作太频繁了", null);
        }
        //攻击拦截结束
        Apiconfig apiconfig = UStatus.getConfig(this.dataprefix, apiconfigService, redisTemplate);
        Integer scale = apiconfig.getScale();
        //商户订单号
        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");//可以方便地修改日期格式
        String timeID = dateFormat.format(now);
        String outTradeNo = timeID + "WxPay";
        Map<String, String> data = WeChatPayUtils.native_payment_order(price.toString(), "微信商品下单", outTradeNo, apiconfig);
        if ("200".equals(data.get("code"))) {
            //先生成订单
            Long date = System.currentTimeMillis();
            String created = String.valueOf(date).substring(0, 10);
            Paylog paylog = new Paylog();

            Integer TotalAmount = price * scale;
            paylog.setStatus(0);
            paylog.setCreated(Integer.parseInt(created));
            paylog.setUid(uid);
            paylog.setOutTradeNo(outTradeNo);
            paylog.setTotalAmount(TotalAmount.toString());
            paylog.setPaytype("WxPay");
            paylog.setSubject("扫码支付");
            paylogService.insert(paylog);
            //再返回二维码
            data.put("outTradeNo", outTradeNo);
            data.put("totalAmount", price.toString());

            JSONObject toResponse = new JSONObject();
            toResponse.put("code", 1);
            toResponse.put("data", data);
            toResponse.put("msg", "获取成功");
            return toResponse.toString();
        } else {
            JSONObject toResponse = new JSONObject();
            toResponse.put("code", 0);
            toResponse.put("data", "");
            toResponse.put("msg", "请求失败");
            return toResponse.toString();
        }

    }

    /**
     * 微信回调
     */
    @RequestMapping(value = "/wxPayNotify")
    @ResponseBody
    public String wxPayNotify(
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Apiconfig apiconfig = UStatus.getConfig(this.dataprefix, apiconfigService, redisTemplate);
        Map<String, Object> map = new ObjectMapper().readValue(request.getInputStream(), Map.class);
        Map<String, Object> dataMap = WeChatPayUtils.paramDecodeForAPIV3(map, apiconfig);
        //判断是否⽀付成功
        if ("SUCCESS".equals(dataMap.get("trade_state"))) {
            //支付完成后，写入充值日志
            String trade_no = dataMap.get("transaction_id").toString();
            String out_trade_no = dataMap.get("out_trade_no").toString();


            Long date = System.currentTimeMillis();
            String created = String.valueOf(date).substring(0, 10);
            Paylog paylog = new Paylog();
            //根据订单和发起人，是否有数据库对应，来是否充值成功
            paylog.setOutTradeNo(out_trade_no);
            paylog.setStatus(0);
            List<Paylog> logList = paylogService.selectList(paylog);
            if (logList.size() > 0) {
                Integer pid = logList.get(0).getPid();
                Integer uid = logList.get(0).getUid();
                paylog.setStatus(1);
                paylog.setTradeNo(trade_no);
                paylog.setPid(pid);
                paylog.setCreated(Integer.parseInt(created));
                paylogService.update(paylog);

                //订单修改后，插入用户表
                String total_amount = logList.get(0).getTotalAmount();
                Integer integral = Double.valueOf(total_amount).intValue();
                Users users = usersService.selectByKey(uid);
                Integer oldAssets = users.getAssets();
                Integer assets = oldAssets + integral;
                users.setAssets(assets);
                usersService.update(users);
            } else {
                System.err.println("数据库不存在订单");
                Map<String, String> returnMap = new HashMap<>();
                returnMap.put("code", "FALL");
                returnMap.put("message", "");
                //将返回微信的对象转换为xml
                String returnXml = WeChatPayUtils.mapToXml(returnMap);
                return returnXml;
            }

            //给微信发送我已接收通知的响应
            //创建给微信响应的对象
            Map<String, String> returnMap = new HashMap<>();
            returnMap.put("code", "SUCCESS");
            returnMap.put("message", "成功");
            //将返回微信的对象转换为xml
            String returnXml = WeChatPayUtils.mapToXml(returnMap);
            return returnXml;
        }
        //支付失败
        System.err.println("微信支付失败");
        //创建给微信响应的对象
        Map<String, String> returnMap = new HashMap<>();
        returnMap.put("code", "FALL");
        returnMap.put("message", "");
        //将返回微信的对象转换为xml
        String returnXml = WeChatPayUtils.mapToXml(returnMap);
        return returnXml;

    }

    /**
     * 创建卡密
     **/
    @RequestMapping(value = "/madePaycard")
    @ResponseBody
    public String madePaycard(@RequestParam(value = "num") int num,
                              @RequestParam(value = "price") int price,
                              @RequestParam(value = "type") String type,
                              HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                Users user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user == null || user.toString().isEmpty())
                    return Result.getResultJson(201, "用户不存在，请重新登录", null);
                if (!user.getGroup().equals("administrator")) return Result.getResultJson(201, "无权限", null);
            }
            // 验证通过 创建卡密
            Long timeStamp = System.currentTimeMillis() / 1000;
            Integer _num = num > 100 ? 100 : num;
            List dataList = new ArrayList<>();
            for (int i = 0; i < _num; i++) {
                Paykey paykey = new Paykey();
                paykey.setStatus(0);
                paykey.setPrice(price);
                paykey.setType(type);
                paykey.setCreated(Math.toIntExact(timeStamp));
                paykey.setValue(RandomStringUtils.random(18, true, true));
                paykeyService.insert(paykey);
                dataList.add(paykey);
            }
            Map<String, Object> data = new HashMap<>();
            data.put("count", _num);
            data.put("data", dataList);
            return Result.getResultJson(200, "生成成功", data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    private Boolean permission(Users user) {
        if (user.getUid() == null || user.getUid().equals(0)) return false;
        if (user.getGroup().equals("editor") || user.getGroup().equals("administrator")) return true;
        return false;
    }

    @RequestMapping(value = "/cardList")
    @ResponseBody
    public String cardList(@RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                           @RequestParam(value = "limit", required = false, defaultValue = "50") Integer limit,
                           @RequestParam(value = "order", required = false, defaultValue = "created desc") String order,
                           @RequestParam(value = "status", required = false, defaultValue = "0") Integer status,
                           HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Boolean permission = false;
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                Users user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user == null || user.toString().isEmpty())
                    return Result.getResultJson(201, "用户不存在，请重新登录", null);
                permission = permission(user);
                if (!permission) return Result.getResultJson(201, "无权限", null);
            }

            // 验证通过开始查询列表
            Paykey paykey = new Paykey();
            PageList<Paykey> paykeyPageList = paykeyService.selectPage(paykey, page, limit, null);
            List<Paykey> paykeyList = paykeyPageList.getList();

            Map<String, Object> data = new HashMap<>();
            data.put("count", paykeyList.size());
            data.put("total", paykeyService.total(paykey));
            data.put("data", paykeyList);
            return Result.getResultJson(200, "获取成功", data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }


    @RequestMapping(value = "/cardExport")
    @ResponseBody
    public String cardExport(@RequestParam(value = "limit", required = false, defaultValue = "50") Integer limit,
                             HttpServletRequest request,
                             HttpServletResponse response) {
        try {
            Boolean permission = false;
            String token = request.getHeader("Authorization");
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                Users user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user == null || user.toString().isEmpty())
                    return Result.getResultJson(201, "用户不存在，请重新登录", null);
                permission = permission(user);
                if (!permission) return Result.getResultJson(201, "无权限", null);
            }

            Paykey paykey = new Paykey();
            List<Paykey> paykeyList = paykeyService.selectList(paykey);
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Card Data");
                String[] headers = {"ID", "卡密", "数值", "类型", "状态", "创建时间", "使用uid"};

                // 写入表头
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < headers.length; i++) {
                    headerRow.createCell(i).setCellValue(headers[i]);
                }

                // 写入数据
                for (int i = 0; i < paykeyList.size(); i++) {
                    Row row = sheet.createRow(i + 1);
                    Paykey paykeyData = paykeyList.get(i);
                    row.createCell(0).setCellValue(paykeyData.getId());
                    row.createCell(1).setCellValue(paykeyData.getValue());
                    row.createCell(2).setCellValue(paykeyData.getPrice());
                    row.createCell(3).setCellValue(paykeyData.getType());
                    row.createCell(4).setCellValue(paykeyData.getStatus() > 0 ? "已使用" : "未使用");
                    row.createCell(5).setCellValue(paykeyData.getCreated());
                    row.createCell(6).setCellValue(paykeyData.getUid());
                }

                response.setContentType("application/octet-stream");
                response.setHeader("Content-Disposition", "attachment; filename=卡密.xlsx");

                workbook.write(response.getOutputStream());
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return Result.getResultJson(500, "导出Excel文件失败", null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }
    @RequestMapping(value = "/delete")
    @ResponseBody
    public String delete(@RequestParam(value = "id") int id,
                         HttpServletRequest request) {
        try {
            Boolean permission = false;
            String token = request.getHeader("Authorization");
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                Users user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user == null || user.toString().isEmpty())
                    return Result.getResultJson(201, "用户不存在，请重新登录", null);
                permission = permission(user);
                if (!permission) return Result.getResultJson(201, "无权限", null);
            }

            Paykey paykey = paykeyService.selectByKey(id);
            if (paykey.getId() == null) return Result.getResultJson(201, "卡密不存在", null);

            paykeyService.delete(paykey.getId());
            return Result.getResultJson(200, "删除成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /**
     * 卡密充值
     **/
    @RequestMapping(value = "/chargeCard")
    @ResponseBody
    public String chargeCard(@RequestParam(value = "card", required = false) String card,
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

            Paykey paykey = paykeyService.selectByCard(card);
            if (paykey==null || paykey.getValue() == null) return Result.getResultJson(201, "卡密不存在", null);
            Integer pirce = paykey.getPrice();
            if (!paykey.getStatus().equals(0)) {
                return Result.getResultJson(201, "卡密已失效", null);
            }

            //修改卡密状态
            paykey.setStatus(1);
            paykey.setUid(user.getUid());
            paykeyService.update(paykey);
            long timeStamp = System.currentTimeMillis() / 1000;
            if (paykey.getType().equals("vip")) {
                int dayTime = paykey.getPrice() * 86400;
                Boolean isVip = user.getVip() > timeStamp;
                if (isVip) user.setVip(user.getVip() + dayTime);
                else user.setVip((int) (timeStamp + dayTime));
                System.out.println(dayTime + "_" + timeStamp);
                // 给用户发消息
                Inbox inbox = new Inbox();
                inbox.setCreated((int) timeStamp);
                inbox.setTouid(user.getUid());
                inbox.setType("system");
                inbox.setValue(paykey.getPrice());
                inbox.setText("使用卡密充值会员" + paykey.getPrice() + "天");
                inboxService.insert(inbox);
            }
            if (paykey.getType().equals("point")) {
                //生成资产日志
                Long date = System.currentTimeMillis();
                String curTime = String.valueOf(date).substring(0, 10);
                Paylog paylog = new Paylog();
                paylog.setStatus(1);
                paylog.setCreated(Integer.parseInt(curTime));
                paylog.setUid(user.getUid());
                paylog.setOutTradeNo(curTime + "tokenPay");
                paylog.setTotalAmount(pirce.toString());
                paylog.setPaytype("tokenPay");
                paylog.setSubject("卡密充值");
                paylogService.insert(paylog);
                //修改用户账户
                user.setAssets((user.getAssets() > 0 ? user.getAssets() : 0) + paykey.getPrice());
            }

            usersService.update(user);

            return Result.getResultJson(200, "充值成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }

    }

    /***
     * 清除无用卡密
     * @param status
     * @param request
     * @return
     */

    @RequestMapping(value = "/clear")
    @ResponseBody
    public String clear(@RequestParam(value = "status") Integer status,
                        HttpServletRequest request){
        try {
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if(token!=null && !token.isEmpty()){
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
            }
            if(!permission(user)) return Result.getResultJson(201,"无权限",null);
            // 根据type 清除对应类型的卡密
            // 0 未使用
            // 1 已使用
            paykeyService.typeDelete(status);
            return Result.getResultJson(200,"已清除",null);
        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(400,"接口异常",null);
        }
    }

    /**
     * 彩虹易支付相关
     **/
    @RequestMapping(value = "/EPay")
    @ResponseBody
    public String EPay(@RequestParam(value = "type", required = false) String type, @RequestParam(value = "money", required = false) Integer money, @RequestParam(value = "device", required = false) String device, @RequestParam(value = "token", required = false) String token, HttpServletRequest request) {
        if (type == null && money == null && money == null && device == null) {
            return Result.getResultJson(0, "参数不正确", null);
        }
        try {
            Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
            if (uStatus == 0) {
                return Result.getResultJson(0, "用户未登录或Token验证失败", null);
            }

            Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
            Integer uid = Integer.parseInt(map.get("uid").toString());
            //登录情况下，恶意充值攻击拦截
            String isSilence = redisHelp.getRedis(this.dataprefix + "_" + uid + "_silence", redisTemplate);
            if (isSilence != null) {
                return Result.getResultJson(0, "你的操作太频繁了，请稍后再试", null);
            }
            String isRepeated = redisHelp.getRedis(this.dataprefix + "_" + uid + "_isRepeated", redisTemplate);
            if (isRepeated == null) {
                redisHelp.setRedis(this.dataprefix + "_" + uid + "_isRepeated", "1", 2, redisTemplate);
            } else {
                Integer frequency = Integer.parseInt(isRepeated) + 1;
                if (frequency == 3) {
                    securityService.safetyMessage("用户ID：" + uid + "，在微信充值接口疑似存在攻击行为，请及时确认处理。", "system");
                    redisHelp.setRedis(this.dataprefix + "_" + uid + "_silence", "1", 900, redisTemplate);
                    return Result.getResultJson(0, "你的请求存在恶意行为，15分钟内禁止操作！", null);
                } else {
                    redisHelp.setRedis(this.dataprefix + "_" + uid + "_isRepeated", frequency.toString(), 3, redisTemplate);
                }
                return Result.getResultJson(0, "你的操作太频繁了", null);
            }
            //攻击拦截结束

            Apiconfig apiconfig = UStatus.getConfig(this.dataprefix, apiconfigService, redisTemplate);
            String url = apiconfig.getEpayUrl();
            Date now = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");//可以方便地修改日期格式
            String timeID = dateFormat.format(now);
            String outTradeNo = timeID + "Epay_" + type;
            String clientip = baseFull.getIpAddr(request);
            Map<String, String> sign = new HashMap<>();
            sign.put("pid", apiconfig.getEpayPid().toString());
            sign.put("type", type.toString());
            sign.put("out_trade_no", outTradeNo);
            sign.put("notify_url", apiconfig.getEpayNotifyUrl());
            sign.put("clientip", clientip);
            sign.put("name", "在线充值金额");
            sign.put("money", money.toString());
            sign = sortByKey(sign);
            String signStr = "";
            for (Map.Entry<String, String> m : sign.entrySet()) {
                signStr += m.getKey() + "=" + m.getValue() + "&";
            }
            signStr = signStr.substring(0, signStr.length() - 1);
            signStr += apiconfig.getEpayKey();
            signStr = DigestUtils.md5DigestAsHex(signStr.getBytes());
            sign.put("sign_type", "MD5");
            sign.put("sign", signStr);

            String param = "";
            for (Map.Entry<String, String> m : sign.entrySet()) {
                param += m.getKey() + "=" + m.getValue() + "&";
            }
            param = param.substring(0, param.length() - 1);
            String data = HttpClient.doPost(url + "mapi.php", param);
            if (data == null) {
                return Result.getResultJson(0, "易支付接口请求失败，请检查配置", null);
            }
            HashMap jsonMap = JSON.parseObject(data, HashMap.class);
            if (jsonMap.get("code").toString().equals("1")) {
                //先生成订单
                Long date = System.currentTimeMillis();
                String created = String.valueOf(date).substring(0, 10);
                Paylog paylog = new Paylog();
                Integer TotalAmount = money * apiconfig.getScale();
                paylog.setStatus(0);
                paylog.setCreated(Integer.parseInt(created));
                paylog.setUid(uid);
                paylog.setOutTradeNo(outTradeNo);
                paylog.setTotalAmount(TotalAmount.toString());
                paylog.setPaytype("ePay_" + type);
                paylog.setSubject("扫码支付");
                paylogService.insert(paylog);
                //再返回数据
                JSONObject toResponse = new JSONObject();
                toResponse.put("code", 1);
                toResponse.put("payapi", apiconfig.getEpayUrl());
                toResponse.put("data", jsonMap);
                toResponse.put("msg", "获取成功");
                return toResponse.toString();
            } else {
                return Result.getResultJson(0, jsonMap.get("msg").toString(), null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(0, "接口请求异常，请联系管理员", null);
        }


    }

    public static <K extends Comparable<? super K>, V> Map<K, V> sortByKey(Map<K, V> map) {
        Map<K, V> result = new LinkedHashMap<>();
        map.entrySet().stream()
                .sorted(Map.Entry.<K, V>comparingByKey()).forEachOrdered(e -> result.put(e.getKey(), e.getValue()));
        return result;
    }

    @RequestMapping(value = "/EPayNotify")
    @ResponseBody
    public String EPayNotify(HttpServletRequest request,
                             HttpServletResponse response) throws AlipayApiException {
        Map<String, String> params = new HashMap<String, String>();
        Map requestParams = request.getParameterMap();
        for (Iterator iter = requestParams.keySet().iterator(); iter.hasNext(); ) {
            String name = (String) iter.next();
            String[] values = (String[]) requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            params.put(name, valueStr);
        }

        System.err.println(params);
        try {
            if (params.get("trade_status").equals("TRADE_SUCCESS")) {
                Apiconfig apiconfig = UStatus.getConfig(this.dataprefix, apiconfigService, redisTemplate);
                //支付完成后，写入充值日志
                String trade_no = params.get("trade_no");
                String out_trade_no = params.get("out_trade_no");
                String total_amount = params.get("money");
                Integer scale = apiconfig.getScale();
                Integer integral = Double.valueOf(total_amount).intValue() * scale;

                Long date = System.currentTimeMillis();
                String created = String.valueOf(date).substring(0, 10);
                Paylog paylog = new Paylog();
                //根据订单和发起人，是否有数据库对应，来是否充值成功
                paylog.setOutTradeNo(out_trade_no);
                paylog.setStatus(0);
                List<Paylog> logList = paylogService.selectList(paylog);
                if (logList.size() > 0) {
                    Integer pid = logList.get(0).getPid();
                    Integer uid = logList.get(0).getUid();
                    paylog.setStatus(1);
                    paylog.setTradeNo(trade_no);
                    paylog.setPid(pid);
                    paylog.setCreated(Integer.parseInt(created));
                    paylogService.update(paylog);
                    //订单修改后，插入用户表
                    Users users = usersService.selectByKey(uid);
                    Integer oldAssets = users.getAssets();
                    Integer assets = oldAssets + integral;
                    users.setAssets(assets);
                    usersService.update(users);
                    return "success";
                } else {
                    System.err.println("数据库不存在订单");
                    return "fail";
                }
            } else {
                return "fail";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "fail";
        }

    }

}
