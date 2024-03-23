package com.Fanbbs.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * TypechoReward_log
 * @author reward_log 2022-09-06
 */
@Data
public class Reward_log implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Integer id;

    /**
     * name  奖品名称
     */
    private String name;

    /**
     * reward_id  奖品ID
     */
    private Integer reward_id;

    /**
     * description  奖品描述
     */
    private String description;

    /**
     * uid  用户ID
     */
    private Integer uid;

    /**
     * type  类型
     */
    private String type;

    /**
     * status  状态
     */
    private String status;

    /**
     * expired  过期时间
     */
    private Integer expired;

    /**
     * created  创建时间
     */
    private Integer created;

    /***
     * tracking_number 运单号
     */
    private String tracking_number;

}