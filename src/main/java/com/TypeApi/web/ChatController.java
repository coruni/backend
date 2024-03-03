package com.TypeApi.web;

import com.TypeApi.config.websocket;
import com.TypeApi.common.*;
import com.TypeApi.entity.Chat;
import com.TypeApi.entity.ChatMsg;
import com.TypeApi.entity.Users;
import com.TypeApi.service.*;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    websocket websocket = new websocket();

    RedisHelp redisHelp = new RedisHelp();
    ResultAll Result = new ResultAll();
    UserStatus UStatus = new UserStatus();
    baseFull baseFull = new baseFull();
    EditFile editFile = new EditFile();

    /***
     * 获取聊天室id
     */

    @RequestMapping(value = "/getChatId")
    @ResponseBody
    public String getChatId(@RequestParam(value = "receiver_id") Integer receiver_id,
                            HttpServletRequest request){
        try{
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if(token!=null && !token.isEmpty()){
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if(user==null || user.toString().isEmpty()) return Result.getResultJson(201,"用户不存在，请重新登录！",null);
            }

            // 查询接收用户是否存在
            Users receiveUser = usersService.selectByKey(receiver_id);
            if(receiveUser==null || receiveUser.toString().isEmpty()) return Result.getResultJson(201,"目标用户不存在",null);

            // 验证结束 开始查询聊天列表是否存在
            Chat chat =new Chat();
            chat.setSender_id(user.getUid());
            chat.setReceiver_id(receiveUser.getUid());
            List<Chat> chatList = service.selectList(chat);
            if(chatList.size()>0){
               Map<String,Object> data = JSONObject.parseObject(JSONObject.toJSONString(chatList.get(0)),Map.class);
               return Result.getResultJson(201,"获取成功",data);
            }else{
                chat.setType(0);
                chat.setCreated((int) (System.currentTimeMillis()/1000));
                service.insert(chat);
                Map<String,Object> data = JSONObject.parseObject(JSONObject.toJSONString(chat),Map.class);
                return Result.getResultJson(200,"生成成功",data);
            }

        }catch (Exception e){
            e.printStackTrace();
            return Result.getResultJson(400,"接口异常",null);
        }
    }

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
           // 查找聊天室 是否存在
            Chat chat = service.selectByKey(id);
            if(chat==null || chat.toString().isEmpty()) return Result.getResultJson(201,"聊天室不存在",null);

            Users receiverUser = new Users();
            // 是否是查询对面
            if(chat.getReceiver_id().equals(user.getUid())){
                receiverUser = usersService.selectByKey(chat.getSender_id());
            }else{
                receiverUser = usersService.selectByKey(chat.getReceiver_id());
            }
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

            // 查询聊天室聊天记录
            ChatMsg chatMsg = new ChatMsg();
            chatMsg.setChat_id(chat.getId());
            PageList<ChatMsg> chatMsgPageList = chatMsgService.selectPage(chatMsg,page,limit);
            List<ChatMsg> chatMsgList =  chatMsgPageList.getList();

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
                          HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Users user = new Users();
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                if (user == null && user.toString().isEmpty()) return Result.getResultJson(201, "用户不存在，请重新登录", null);
            }
            // 查询列表是否存在
            if (text == null || text.equals("") || text.isEmpty()) return Result.getResultJson(201, "请输入消息", null);

            // 查询聊天室是否存在
            Chat chat = service.selectByKey(id);
            if(chat==null || chat.toString().isEmpty()) return Result.getResultJson(202,"聊天室不存在",null);
            // 写入信息
            ChatMsg chatMsg = new ChatMsg();
            chatMsg.setType(chat.getType());
            chatMsg.setSender_id(user.getUid());
            chatMsg.setChat_id(chat.getId());
            chatMsg.setText(text);
            chatMsg.setCreated((int) (System.currentTimeMillis() / 1000));
            chatMsgService.insert(chatMsg);

            chat.setLastTime((int) (System.currentTimeMillis() / 1000));
            service.update(chat);
            // 将信息返回
            Map<String, Object> data = JSONObject.parseObject(JSONObject.toJSONString(chatMsg), Map.class);
             // 使用webSocket 给目标用户发消息
            try{
                int user_id = chat.getReceiver_id().equals(user.getUid())?chat.getSender_id():chat.getReceiver_id();
                websocket.sendChatText(text,5);
            }catch (Exception e){
                e.printStackTrace();
            }

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
                    Users chatUser = new Users();
                    if(_chat.getReceiver_id().equals(user.getUid())){
                        chatUser = usersService.selectByKey(_chat.getSender_id());
                    }else{
                        chatUser = usersService.selectByKey(_chat.getReceiver_id());
                    }
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
