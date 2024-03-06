package com.TypeApi.entity;

import lombok.Data;
import java.util.Date;
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
     * expired  过期时间
     */
    private Date expired;

    /**
     * created  创建时间
     */
    private Integer created;

}