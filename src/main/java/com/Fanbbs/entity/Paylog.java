package com.Fanbbs.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * TypechoPaylog
 * @author buxia97 2022-02-07
 */
@Data
public class Paylog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * pid  
     */
    private Integer pid;

    /**
     * subject  
     */
    private String subject;

    /**
     * total_amount  
     */
    private String totalAmount;

    /**
     * out_trade_no  
     */
    private String outTradeNo;

    /**
     * trade_no  
     */
    private String tradeNo;

    /**
     * paytype  支付类型
     */
    private String paytype;

    /**
     * uid  充值人ID
     */
    private Integer uid;

    /**
     * created  
     */
    private Integer created;

    /**
     * cid
     */
    private Integer cid;

    /**
     * status  支付状态（0未支付，1已支付）
     */
    private Integer status;

}