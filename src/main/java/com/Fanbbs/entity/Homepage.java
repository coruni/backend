package com.Fanbbs.entity;
import java.io.Serializable;

import lombok.Data;
@Data
public class Homepage implements Serializable {
    private static final long serialVersionUID = 1L;
    /***
     * id
     */
    private Integer id;
    /***
     * page 路径
     */
    private String page;

    /***
     * name 名称
     */
    private String name;

    /***
     * type 类型 0 page 1url
     */
    private Integer type;
    /***
     * image 图片
     */
    private String image;
    /***
     * enable 是否开启
     */
    private Integer enable;
    /***
     * created 创建时间
     */
    private Integer created;
}
