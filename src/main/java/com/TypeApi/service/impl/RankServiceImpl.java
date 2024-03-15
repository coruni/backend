package com.TypeApi.service.impl;
import com.TypeApi.entity.*;
import com.TypeApi.common.PageList;
import com.TypeApi.dao.*;
import com.TypeApi.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RankServiceImpl implements RankService{
    @Autowired
    RankDao dao;

    @Override
    public int insert(Rank rank) {
        return dao.insert(rank);
    }

    @Override
    public int batchInsert(List<Rank> list) {
        return dao.batchInsert(list);
    }

    @Override
    public int update(Rank rank) {
        return dao.update(rank);
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
    public Rank selectByKey(Object key) {
        return dao.selectByKey(key);
    }

    @Override
    public List<Rank> selectList(Rank rank) {
        return dao.selectList(rank);
    }

    @Override
    public PageList<Rank> selectPage(Rank rank, Integer offset, Integer pageSize, String searchKey, String order) {
        PageList<Rank> pageList = new PageList<>();

        int total = this.total(rank, searchKey);

        Integer totalPage;
        if (total % pageSize != 0) {
            totalPage = (total / pageSize) + 1;
        } else {
            totalPage = total / pageSize;
        }

        int page = (offset - 1) * pageSize;

        List<Rank> list = dao.selectPage(rank, page, pageSize, searchKey, order);

        pageList.setList(list);
        pageList.setStartPageNo(offset);
        pageList.setPageSize(pageSize);
        pageList.setTotalCount(total);
        pageList.setTotalPageCount(totalPage);
        return pageList;
    }

    @Override
    public int total(Rank rank, String searchKey) {
        return dao.total(rank, searchKey);
    }
}
