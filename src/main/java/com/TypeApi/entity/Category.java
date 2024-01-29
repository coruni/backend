package com.TypeApi.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * TypechoMetas
 * @author buxia97 2021-11-29
 */
@Data
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * mid  
     */
    private Integer mid;

    /**
     * name  
     */
    private String name;

    /**
     * slug  
     */
    private String slug;

    /**
     * type  
     */
    private String type;

    /**
     * description  
     */
    private String description;

    /**
     * count  
     */
    private Integer count;

    /**
     * order  
     */
    private Integer orderKey;

    /**
     * parent  
     */
    private Integer parent;

    /**
     * imgurl
     */
    private String imgurl;

    /**
     * opt
     */
    private String opt;

    /**
     * iswaterfall
     */
    private Integer iswaterfall;

    /**
     * isrecommend
     */
    private Integer isrecommend;

    /***
     * 关注
     */
    private Integer follows;
}