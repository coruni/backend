package com.Fanbbs.service.impl;

import com.Fanbbs.entity.*;
import com.Fanbbs.common.PageList;
import com.Fanbbs.dao.*;
import com.Fanbbs.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExchangeServiceImpl implements ExchangeService {
    @Autowired
    ExchangeDao dao;

    @Override
    public int insert(Exchange exchange) {
        return dao.insert(exchange);
    }

    @Override
    public int batchInsert(List<Exchange> list) {
        return dao.batchInsert(list);
    }

    @Override
    public int update(Exchange exchange) {
        return dao.update(exchange);
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
    public Exchange selectByKey(Object key) {
        return dao.selectByKey(key);
    }

    @Override
    public List<Exchange> selectList(Exchange exchange) {
        return dao.selectList(exchange);
    }

    @Override
    public PageList<Exchange> selectPage(Exchange exchange, Integer offset, Integer pageSize, String searchKey, String order) {
        PageList<Exchange> pageList = new PageList<>();

        int total = this.total(exchange, searchKey);

        int totalPage;
        if (total % pageSize != 0) {
            totalPage = (total / pageSize) + 1;
        } else {
            totalPage = total / pageSize;
        }

        int page = (offset - 1) * pageSize;

        List<Exchange> list = dao.selectPage(exchange, page, pageSize, searchKey, order);

        pageList.setList(list);
        pageList.setStartPageNo(offset);
        pageList.setPageSize(pageSize);
        pageList.setTotalCount(total);
        pageList.setTotalPageCount(totalPage);
        return pageList;
    }

    @Override
    public int total(Exchange exchange, String searchKey) {
        return dao.total(exchange, searchKey);
    }
}
