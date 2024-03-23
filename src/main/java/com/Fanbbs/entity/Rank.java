package com.Fanbbs.entity;

import java.io.Serializable;

import lombok.Data;

@Data
public class Rank implements Serializable {
    /***
     * id
     */
    private Integer id;
    /***
     * name 名称
     */
    private String name;
    /***
     * type 类型 0文字 1 图片
     */
    private Integer type;

    /***
     * image 图片
     */
    private String image;

    /***
     * color 文字颜色
     */
    private String color;

    /***
     * background 背景颜色
     */
    private String background;


    /***
     * created 创建时间
     */
    private Integer created;
}
