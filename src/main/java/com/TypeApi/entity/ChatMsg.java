package com.TypeApi.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * TypechoChatMsg
 * @author buxia97 2023-01-11
 */
@Data
public class ChatMsg implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id  
     */
    private Integer id;

    /**
     * sender_id  发送人
     */
    private Integer sender_id;

    /**
     * text  消息内容
     */
    private String text;

    /**
     * created  发送时间
     */
    private Integer created;

    /**
     * type  0是私聊 1是群聊
     */
    private Integer type;

    /***
     * chat_id 聊天室id
     */
    private Integer chat_id;

}