package com.TypeApi.entity;

import java.io.Serializable;

import lombok.Data;

/**
 * TypechoUsers
 * @author buxia97 2021-11-29
 */
@Data
public class Users implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * uid  
     */
    private Integer uid;

    /**
     * name  
     */
    private String name;

    /**
     * password  
     */
    private String password;

    /**
     * mail  
     */
    private String mail;

    /**
     * url  
     */
    private String url;

    /**
     * screenName  
     */
    private String screenName;

    /**
     * created  
     */
    private Integer created;

    /**
     * activated  
     */
    private Integer activated;

    /**
     * logged  
     */
    private Integer logged;

    /**
     * group  
     */
    private String group;

    /**
     * authCode  
     */
    private String authCode;

    /**
     * introduce
     */
    private String introduce;

    /**
     * logged
     */
    private Integer assets;

    /**
     * address
     */
    private String address;

    /**
     * address
     */
    private String pay;

    /**
     * vip（到期时间）
     */
    private Integer vip;

    /**
     * experience（经验值）
     */
    private Integer experience;

    /**
     * avatar（头像）
     */
    private String avatar;

    /**
     * 客户端id（用于发送状态栏消息）
     */
    private String clientId;

    /**
     * 封禁时间
     */
    private Integer bantime;

    /**
     * 最新发布时间
     */
    private Integer posttime;

    /**
     * 用户主页背景图链接
     */
    private String userBg;

    /**
     * sex
     */
    private String sex;

    /**
     * medal
     */
    private String medal;

    /**
     * head_picture
     */
    private String head_picture;

    /**
     * opt
     */
    private String opt;

    /***
     * rank 头衔
     */
    private String rank;

    /***
     * 用户状态
     */
    private Integer status;

}