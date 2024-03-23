package com.Fanbbs.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * TypechoUserapi
 * @author buxia97 2022-01-10
 */
@Data
public class Userapi implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id  
     */
    private Integer id;

    /**
     * headImgUrl  头像，可能用不上
     */
    private String headImgUrl;

    /**
     * openId  开放平台ID
     */
    private String openId;

    /**
     * access_token  唯一值
     */
    private String accessToken;

    /**
     * appLoginType  渠道类型
     */
    private String appLoginType;

    /**
     * uid  用户ID
     */
    private Integer uid;

    /**
     * uid  用户ID
     */
    private String nickName;
}