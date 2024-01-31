package com.TypeApi.web;

import com.TypeApi.common.*;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.TypeApi.entity.*;
import com.TypeApi.service.*;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 控制层
 * TypechoChatController
 *
 * @author buxia97
 * @date 2023/01/10
 */
@Controller
@RequestMapping(value = "/chat")
public class ChatController {

    @Autowired
    ChatService service;

    @Autowired
    ChatMsgService chatMsgService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private ApiconfigService apiconfigService;

    @Autowired
    private HeadpictureService headpictureService;

    @Autowired
    private UsersService usersService;

    @Value("${web.prefix}")
    private String dataprefix;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private PushService pushService;

    RedisHelp redisHelp = new RedisHelp();
    ResultAll Result = new ResultAll();
    UserStatus UStatus = new UserStatus();
    baseFull baseFull = new baseFull();
    EditFile editFile = new EditFile();

    /***
     * 用户聊天记录
     * @param id 接收者用户id
     */
    @RequestMapping(value = "/chatRecord")
    @ResponseBody
    public String chatRecord(@RequestParam(value = "id") Integer id,
                             @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                             @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit,
                             HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user == null || user.toString().isEmpty()) return Result.getResultJson(201, "用户不存在", null);
            }
            Users receiver = usersService.selectByKey(id);
            if (receiver == null || receiver.toString().isEmpty()) return Result.getResultJson(201, "用户不存在", null);
            ChatMsg chatMsg = new ChatMsg();
            chatMsg.setSender_id(user.getUid());
            chatMsg.setReceiver_id(receiver.getUid());
            PageList<ChatMsg> chatMsgPageList = chatMsgService.selectPage(chatMsg, page, limit);
            List<ChatMsg> chatMsgList = chatMsgPageList.getList();
            // 只获取一次接收者用户信息 防止太多查询
            Users receiverUser = usersService.selectByKey(receiver.getUid());
            Map<String, Object> data = JSONObject.parseObject(JSONObject.toJSONString(receiverUser));
            // 格式化opt
            JSONObject opt = new JSONObject();
            JSONArray head_picture = new JSONArray();
            opt = receiverUser.getOpt() != null && !receiverUser.getOpt().toString().isEmpty() ? JSONObject.parseObject(receiverUser.getOpt()) : null;
            head_picture = receiverUser.getHead_picture() != null && !receiverUser.getHead_picture().toString().isEmpty() ? JSONArray.parseArray(receiverUser.getHead_picture()) : null;
            // 处理头像框
            if (head_picture != null && opt != null && !head_picture.isEmpty()) {
                opt.put("head_picture", headpictureService.selectByKey(opt.get("head_picture")).getLink().toString());
            }
            data.put("opt", opt);
            data.remove("head_picture");
            data.remove("password");
            data.remove("mail");
            data.remove("address");
            JSONArray dataList = new JSONArray();
            for (ChatMsg _chatMsg : chatMsgList) {
                Map<String, Object> msgData = JSONObject.parseObject(JSONObject.toJSONString(_chatMsg), Map.class);
                msgData.put("userInfo", data);
                dataList.add(msgData);
            }
            Map<String, Object> result = new HashMap<>();
            result.put("page", page);
            result.put("limit", limit);
            result.put("data", dataList);
            result.put("count", dataList.size());
            result.put("total", chatMsgService.total(chatMsg));

            return Result.getResultJson(200, "获取成功", result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     * 发送消息
     */
    @RequestMapping(value = "/sendMsg")
    @ResponseBody
    public String sendMsg(@RequestParam(value = "id") Integer id,
                          @RequestParam(value = "text") String text,
                          @RequestParam(value = "type", defaultValue = "0") Integer type,
                          HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user == null && user.toString().isEmpty()) return Result.getResultJson(201, "用户不存在，请重新登录", null);
            }
            // 查询接收者信息
            Users recevierUser = usersService.selectByKey(id);
            if (recevierUser == null || recevierUser.toString().isEmpty())
                return Result.getResultJson(201, "用户不存在", null);
            // 查询列表是否存在
            Long timeStamp = System.currentTimeMillis() / 1000;
            Chat chat = new Chat();
            chat.setSender_id(user.getUid());
            chat.setReceiver_id(recevierUser.getUid());
            chat.setType(type);
            List<Chat> chatList = service.selectList(chat);
            // 如果不存在就新增一条
            if (chatList.size() < 1) {
                chat.setCreated(Math.toIntExact(timeStamp));
                service.insert(chat);
            } else {
                chat = chatList.get(0);
                chat.setLastTime(Math.toIntExact(timeStamp));
                // 如果存在就更新最后发送时间
                service.update(chat);
            }
            if (text == null || text.equals("") || text.isEmpty()) return Result.getResultJson(201, "请输入消息", null);

            // 写入信息
            ChatMsg chatMsg = new ChatMsg();
            chatMsg.setType(type);
            chatMsg.setSender_id(user.getUid());
            chatMsg.setReceiver_id(recevierUser.getUid());
            chatMsg.setText(text);
            chatMsg.setCreated((int) (System.currentTimeMillis() / 1000));
            chatMsgService.insert(chatMsg);

            // 将信息返回
            Map<String, Object> data = JSONObject.parseObject(JSONObject.toJSONString(chatMsg), Map.class);

            return Result.getResultJson(200, "发送成功", data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }

    }

    /***
     * 获取聊天列表
     */

    @RequestMapping("/chatList")
    @ResponseBody
    public String chatList(@RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                           @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit,
                           @RequestParam(value = "order", required = false, defaultValue = "lastTime desc") String order,
                           HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user == null && user.toString().isEmpty()) return Result.getResultJson(201, "用户不存在", null);
            }
            Chat chat = new Chat();
            chat.setSender_id(user.getUid());
            PageList<Chat> chatPageList = service.selectPage(chat, page, limit, order, null);
            List<Chat> chatList = chatPageList.getList();
            JSONArray dataList = new JSONArray();
            for (Chat _chat : chatList) {
                Map<String, Object> data = JSONObject.parseObject(JSONObject.toJSONString(_chat), Map.class);
                // 如果type为0查询接收者的信息
                if (_chat.getType().equals(0)) {
                    Users chatUser = usersService.selectByKey(_chat.getReceiver_id());
                    Map<String, Object> userInfo = JSONObject.parseObject(JSONObject.toJSONString(chatUser), Map.class);
                    userInfo.remove("password");
                    userInfo.remove("address");
                    userInfo.remove("mail");
                    userInfo.remove("opt");
                    data.put("userInfo", userInfo);
                }
                dataList.add(data);
            }
            Map<String, Object> data = new HashMap<>();
            data.put("page", page);
            data.put("limit", limit);
            data.put("data", dataList);
            data.put("count", dataList.size());
            data.put("total", service.total(chat));
            return Result.getResultJson(200, "获取成功", data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     * 创建群
     */

}
