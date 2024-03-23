package com.Fanbbs.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * TypechoComments
 * @author buxia97 2021-11-29
 */
@Data
public class Comments implements Serializable {

    private static final long serialVersionUID = 1L;

    /***
     * id
     */
    private Integer id;

    /**
     * cid  
     */
    private Integer cid;

    /**
     * uid
     */
    private Integer uid;

    /**
     * text
     */
    private String text;

    /**
     * images
     */
    private String images;

    /**
     * ip
     */
    private String ip;

    /**
     * parent
     */
    private Integer parent;

    /**
     * likes
     */
    private Integer likes;

    /**
     * all
     */
    private Integer all;

    /**
     * type
     */
    private Integer type;

    /**
     * created
     */
    private Integer created;

    /**
     * created
     */
    private Integer modified;

}