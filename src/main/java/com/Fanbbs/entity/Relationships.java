package com.Fanbbs.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * TypechoRelationships
 * @author buxia97 2021-11-29
 */
@Data
public class Relationships implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * cid  
     */
    private Integer cid;

    /**
     * mid  
     */
    private Integer mid;

    private Article contents;
}