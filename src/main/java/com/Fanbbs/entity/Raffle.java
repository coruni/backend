package com.Fanbbs.entity;

import lombok.Data;
import java.io.Serializable;

/**
 * TypechoRaffle
 * @author raffle 2022-09-06
 */
@Data
public class Raffle implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Integer id;

    /**
     * name  奖励名称
     */
    private String name;

    /**
     * type  奖励类型
     */
    private String type;

    /**
     * chance 中奖概率
     */
    private Float chance;

    /**
     * image 奖品图片
     */
    private String image;

    /**
     * description 奖品介绍
     */
    private String description;

    /**
     * quantity  奖品数量
     */
    private Integer quantity;

    /**
     * value  奖品价值
     */
    private Integer value;

    /**
     * expiry_date 奖品过期时间
     */
    private Integer expiry_date;

    /**
     * created_at  创建时间
     */
    private Integer created_at;
}