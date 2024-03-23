package com.Fanbbs.service;

public interface PushService {
    /**
     * [新增]
     **/
    void sendPushMsg(String cid, String title, String content, String ClickType, String pushText);
}
