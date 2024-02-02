package com.TypeApi.entity;

import java.io.Serializable;

import lombok.Data;

/**
 * Homepage
 *
 * @author Maplene 2024/02/02
 */
@Data
public class Headpicture implements Serializable {
    private static final long serialVersionUID = 1L;

    /***
     * Integer id
     ***/
    private Integer id;

    /***
     * String name
     ***/
    private String name;

    /***
     * String link
     ***/
    private String link;

    /***
     * Integer Status
     ***/
    private Integer Status;

    /***
     * Integer type
     ***/
    private Integer type;

    /***
     * String permission
     ***/
    private Integer permission;

    /***
     * Integer creator
     ***/
    private Integer creator;
}
