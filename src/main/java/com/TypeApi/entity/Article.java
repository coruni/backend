package com.TypeApi.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * TypechoContents
 * @author buxia97 2021-11-29
 */
@Data
public class Article implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * cid
     */
    private Integer mid;

    /**
     * cid  
     */
    private Integer cid;

    /**
     * title  
     */
    private String title;

    /**
     * slug  
     */
    private String slug;

    /**
     * created  
     */
    private Integer created;

    /**
     * modified  
     */
    private Integer modified;

    /**
     * text  
     */
    private String text;

    /**
     * order  
     */
    private Integer orderKey;

    /**
     * authorId  
     */
    private Integer authorId;

    /**
     * template  
     */
    private String template;

    /**
     * type  
     */
    private String type;

    /**
     * status  
     */
    private String status;

    /**
     * password  
     */
    private String password;

    /**
     * commentsNum  
     */
    private Integer commentsNum;

    /**
     * images
     */
    private String images;

    /**
     * videos
     */
    private String videos;

    /**
     * allowComment  
     */
    private String allowComment;

    /**
     * allowPing  
     */
    private String allowPing;

    /**
     * allowFeed  
     */
    private String allowFeed;

    /**
     * parent  
     */
    private Integer parent;

    /**
     * views
     */
    private Integer views;

    /**
     * likes
     */
    private Integer likes;

    /**
     * isrecommend
     */
    private Integer isrecommend;

    /**
     * istop
     */
    private Integer istop;

    /**
     * isswiper
     */
    private Integer isswiper;

    /**
     * replyTime  回复时间
     */
    private Integer replyTime;

    /**
     * opt  自定义字段
     */
    private String opt;

    /**
     * price 价格
     */
    private Integer price;

    /**
     * discount 折扣
     */
    private Float discount;

    /**
     * discount 折扣
     */
    private Integer marks;

    /***
     * 圈子置顶
     */
    private Integer isCircleTop;

}