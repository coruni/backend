package com.Fanbbs.service;
import java.util.List;
import com.Fanbbs.entity.*;
import com.Fanbbs.common.PageList;

/**
 * 业务层
 * TypechoHeadpictureSerice
 * @author Maplene
 * @date 2023/11/20
 */

public interface HeadpictureService {

    int insert(Headpicture Headpicture);

    int batchInsert(List<Headpicture> list);

    int update(Headpicture Headpicture);

    int delete(Object key);

    int batchDelete(List<Object> keys);

    Headpicture selectByKey(Object key);

    List<Headpicture> selectList(Headpicture Headpicture);

    PageList<Headpicture> selectPage(Headpicture Headpicture, Integer offset, Integer pageSize, String order);

    int total(Headpicture Headpicture);
}
