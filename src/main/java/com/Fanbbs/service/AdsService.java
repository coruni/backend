package com.Fanbbs.service;

import java.util.List;
import com.Fanbbs.entity.*;
import com.Fanbbs.common.PageList;

/**
 * 业务层
 * TypechoAdsService
 * @author ads
 * @date 2022/09/06
 */
public interface AdsService {

    /**
     * [新增]
     **/
    int insert(Ads ads);
    /**
     * [更新]
     **/
    int update(Ads ads);

    /**
     * [删除]
     **/
    int delete(Object key);

    /**
     * [主键查询]
     **/
    Ads selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<Ads> selectList (Ads ads);

    /**
     * [分页条件查询]
     **/
    PageList<Ads> selectPage (Ads ads, Integer page, Integer pageSize, String searchKey);

    /**
     * [总量查询]
     **/
    int total(Ads ads);
}
