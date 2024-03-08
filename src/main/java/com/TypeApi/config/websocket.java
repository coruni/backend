package com.TypeApi.config;

import com.TypeApi.common.*;
import com.alibaba.fastjson.JSONObject;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


@ServerEndpoint("/websocket")
@Component
public class websocket {

    private static int onlineCount = 0;     // 统计在线人数，粗略统计，未涉及并发
    private static ConcurrentHashMap<String, websocket> webSocketMap = new ConcurrentHashMap<>();   // session管理map
    private Session session;    // 会话session
    private String userId = "";   // 当前用户

    /**
     * 初始化连接
     *
     * @param session 会话session
     */
    @OnOpen
    public void onOpen(Session session) throws IOException {
        // 校验token是否正确
        DecodedJWT verify = null;
        try {
            verify = JWT.verify(session.getRequestParameterMap().get("token").get(0));
        } catch (Exception e) {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "token错误"));
        }

        this.session = session;
        this.userId = verify.getClaim("aud").asString();
        // 保存各用户的会话session
        if (webSocketMap.containsKey(userId)) {
            webSocketMap.remove(userId);
            webSocketMap.put(userId, this);
        } else {
            webSocketMap.put(userId, this);
            addOnlineCount();
        }

        try {
            sendMessage("连接成功");
//            WebSocketPingScheduler.startPingScheduler(session);
        } catch (IOException e) {
//            WebSocketPingScheduler.stopPingScheduler();
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        if (webSocketMap.containsKey(userId)) {
            webSocketMap.remove(userId);
            subOnlineCount();
        }
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        JSONObject messageObject = JSONObject.parseObject(message);
        if (messageObject!=null && messageObject.get("type").equals("chat")) {
            String receiveId = messageObject.getString("receive_id");
            websocket receiverWebSocketController = webSocketMap.get(receiveId);
            if (receiverWebSocketController != null) {
                Session receiveSession = receiverWebSocketController.session;
                receiveSession.getAsyncRemote().sendText(messageObject.getString("text"));
            }
        }
        if(messageObject!=null && messageObject.get("type").toString().toLowerCase().equals("ping")){
            session.getAsyncRemote().sendText("PONG");
        }
    }

    /**
     * 异常处理
     *
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        // 添加更多异常处理逻辑，例如重新连接或通知管理员
    }

    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }

    /**
     * 发送自定义消息
     *
     * @param message 消息体
     */
    public void sendInfo(String message) throws IOException {

        List<String> keys = new ArrayList<>(webSocketMap.keySet());
        if (CollectionUtils.isNotEmpty(keys)) {
            for (String key : keys) {
                webSocketMap.get(key).sendMessage(message);
            }
        }
    }


    /***
     * 发送用户聊天消息
     * @param Message
     * @param user_id
     * @throws IOException
     */
    public void sendChatText(String Message,Integer user_id) throws IOException{
        webSocketMap.get(user_id.toString()).sendMessage(Message);
        System.out.println(webSocketMap.get(user_id.toString()));
    }

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        websocket.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        websocket.onlineCount--;
    }
}
