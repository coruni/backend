package com.TypeApi.service.impl;

import com.TypeApi.entity.*;
import com.TypeApi.common.PageList;
import com.TypeApi.dao.*;
import com.TypeApi.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class HomepageServiceImpl implements HomepageService {

    @Autowired
    HomepageDao dao;

    @Override
    public int insert(Homepage homepage) {
        return dao.insert(homepage);
    }

    @Override
    public int batchInsert(List<Homepage> list) {
        return dao.batchInsert(list);
    }

    @Override
    public int update(Homepage homepage) {
        return dao.update(homepage);
    }

    @Override
    public int delete(Object key) {
        return dao.delete(key);
    }

    @Override
    public int batchDelete(List<Object> keys) {
        return dao.batchDelete(keys);
    }

    @Override
    public Homepage selectByKey(Object key) { return dao.selectByKey(key);}

    @Override
    public List<Homepage> selectList(Homepage homepage) {
        return dao.selectList(homepage);
    }

    @Override
    public PageList<Homepage> selectPage(Homepage homepage, Integer offset, Integer pageSize, String order) {
        PageList<Homepage> pageList = new PageList<>();

        int total = this.total(homepage);

        int totalPage;
        if (total % pageSize != 0) {
            totalPage = (total / pageSize) + 1;
        } else {
            totalPage = total / pageSize;
        }

        int page = (offset - 1) * pageSize;

        List<Homepage> list = dao.selectPage(homepage, page, pageSize, order);

        pageList.setList(list);
        pageList.setStartPageNo(offset);
        pageList.setPageSize(pageSize);
        pageList.setTotalCount(total);
        pageList.setTotalPageCount(totalPage);
        return pageList;
    }

    @Override
    public int total(Homepage homepage) {
        return dao.total(homepage);
    }
}
