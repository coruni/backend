package com.Fanbbs.service.impl;

import com.Fanbbs.entity.*;
import com.Fanbbs.common.PageList;
import com.Fanbbs.dao.*;
import com.Fanbbs.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    OrderDao dao;

    @Override
    public int insert(Order order) {
        return dao.insert(order);
    }

    @Override
    public int batchInsert(List<Order> list) {
        return dao.batchInsert(list);
    }

    @Override
    public int update(Order order) {
        return dao.update(order);
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
    public Order selectByKey(Object key) {
        return dao.selectByKey(key);
    }

    @Override
    public Order selectByOrder(Object orders) {
        return dao.selectByOrder(orders);
    }

    @Override
    public List<Order> selectList(Order order) {
        return dao.selectList(order);
    }

    @Override
    public PageList<Order> selectPage(Order order, Integer offset, Integer pageSize, String searchKey, String orderKey) {
        PageList<Order> pageList = new PageList<>();

        int total = this.total(order);

        int totalPage;
        if (total % pageSize != 0) {
            totalPage = (total /pageSize) + 1;
        } else {
            totalPage = total /pageSize;
        }

        int page = (offset - 1) * pageSize;

        List<Order> list = dao.selectPage(order, page, pageSize,searchKey,orderKey);

        pageList.setList(list);
        pageList.setStartPageNo(offset);
        pageList.setPageSize(pageSize);
        pageList.setTotalCount(total);
        pageList.setTotalPageCount(totalPage);
        return pageList;
    }

    @Override
    public int total(Order order) {
        return dao.total(order);
    }
}
