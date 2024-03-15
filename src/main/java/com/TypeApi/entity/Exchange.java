package com.TypeApi.entity;
import java.io.Serializable;
import lombok.Data;

@Data
public class Exchange implements Serializable {
    /***
     * id
     */
    private Integer id;

    /***
     * name 名称
     */
    private String name;

    /***
     * type 类型 rank头衔 avatar 头像框
     */
    private String type;

    /***
     * 根据类型填id
     */
    private Integer exchange_id;


    /***
     * price 价格 整数
     */
    private Integer price;

    /***
     * created 创建时间
     */
    private Integer created;
}
