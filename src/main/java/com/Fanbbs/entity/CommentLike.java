package com.Fanbbs.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * TypechoChatMsg
 * @author buxia97 2023-01-11
 */
@Data
public class CommentLike implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Integer id;

    /**
     * uid  点赞人
     */
    private Integer uid;

    /**
     *   评论id
     */
    private Integer cid;

    /**
     * created  创建时间
     */
    private Integer created;


}