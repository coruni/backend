package com.Fanbbs.dao;

import com.Fanbbs.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoUserlogDao
 * @author buxia97
 * @date 2022/01/06
 */
@Mapper
public interface UserlogDao {

    /**
     * [新增]
     **/
    int insert(Userlog userlog);

    /**
     * [批量新增]
     **/
    int batchInsert(List<Userlog> list);

    /**
     * [更新]
     **/
    int update(Userlog userlog);

    /**
     * [删除]
     **/
    int delete(Object key);

    /**
     * [批量删除]
     **/
    int batchDelete(List<Object> list);

    /**
     * [主键查询]
     **/
    Userlog selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<Userlog> selectList (Userlog userlog);

    /**
     * [分页条件查询]
     **/
    List<Userlog> selectPage (@Param("userlog") Userlog userlog, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(Userlog userlog);
}
