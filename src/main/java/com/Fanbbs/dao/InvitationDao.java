package com.Fanbbs.dao;

import com.Fanbbs.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * dao层接口
 * TypechoInvitationDao
 * @author invitation
 * @date 2022/05/03
 */
@Mapper
public interface InvitationDao {

    /**
     * [新增]
     **/
    int insert(Invitation invitation);

    /**
     * [批量新增]
     **/
    int batchInsert(List<Invitation> list);

    /**
     * [更新]
     **/
    int update(Invitation invitation);

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
    Invitation selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<Invitation> selectList (Invitation invitation);

    /**
     * [分页条件查询]
     **/
    List<Invitation> selectPage (@Param("invitation") Invitation invitation, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(Invitation invitation);
}
