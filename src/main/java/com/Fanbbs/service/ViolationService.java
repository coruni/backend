package com.Fanbbs.service;

import java.util.List;
import com.Fanbbs.entity.*;
import com.Fanbbs.common.PageList;

/**
 * 业务层
 * TypechoViolationService
 * @author buxia97
 * @date 2023/01/03
 */
public interface ViolationService {

    /**
     * [新增]
     **/
    int insert(Violation violation);

    /**
     * [批量新增]
     **/
    int batchInsert(List<Violation> list);

    /**
     * [更新]
     **/
    int update(Violation violation);

    /**
     * [删除]
     **/
    int delete(Object key);

    /**
     * [批量删除]
     **/
    int batchDelete(List<Object> keys);

    /**
     * [主键查询]
     **/
    Violation selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<Violation> selectList (Violation violation);

    /**
     * [分页条件查询]
     **/
    PageList<Violation> selectPage (Violation violation, Integer page, Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(Violation violation);
}
