package com.Fanbbs.service;

import com.Fanbbs.entity.*;

/**
 * 业务层
 * TypechoApiconfigService
 * @author apiconfig
 * @date 2022/04/28
 */
public interface ApiconfigService {

    /**
     * [新增]
     **/
    int insert(Apiconfig apiconfig);


    /**
     * [更新]
     **/
    int update(Apiconfig apiconfig);


    /**
     * [主键查询]
     **/
    Apiconfig selectByKey(Object key);

}
