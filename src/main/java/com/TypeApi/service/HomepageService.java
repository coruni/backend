package com.TypeApi.service;
import java.util.List;
import com.TypeApi.entity.*;
import com.TypeApi.common.PageList;

public interface HomepageService {
    int insert(Homepage homepage);

    int batchInsert(List<Homepage> list);

    int update(Homepage homepage);

    int delete(Object key);

    int batchDelete(List<Object> keys);

    Homepage selectByKey(Object key);

    List<Homepage> selectList(Homepage homepage);

    PageList<Homepage> selectPage(Homepage homepage, Integer offset, Integer pageSize, String order);

    int total(Homepage homepage);
}
