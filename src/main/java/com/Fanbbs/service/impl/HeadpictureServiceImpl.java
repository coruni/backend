package com.Fanbbs.service.impl;

import com.Fanbbs.entity.*;
import com.Fanbbs.common.PageList;
import com.Fanbbs.dao.*;
import com.Fanbbs.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 业务层实现类
 * TypechoContentsServiceImpl
 *
 * @author buxia97
 * @date 2021/11/29
 */
@Service
public class HeadpictureServiceImpl implements HeadpictureService {

    @Autowired
    HeadpictureDao dao;

    @Override
    public int insert(Headpicture Headpicture) {
        return dao.insert(Headpicture);
    }

    @Override
    public int batchInsert(List<Headpicture> list) {
        return dao.batchInsert(list);
    }

    @Override
    public int update(Headpicture Headpicture) {
        return dao.update(Headpicture);
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
    public Headpicture selectByKey(Object key) { return dao.selectByKey(key);}

    @Override
    public List<Headpicture> selectList(Headpicture Headpicture) {
        return dao.selectList(Headpicture);
    }

    @Override
    public PageList<Headpicture> selectPage(Headpicture Headpicture, Integer offset, Integer pageSize, String order) {
        PageList<Headpicture> pageList = new PageList<>();

        int total = this.total(Headpicture);

        int totalPage;
        if (total % pageSize != 0) {
            totalPage = (total / pageSize) + 1;
        } else {
            totalPage = total / pageSize;
        }

        int page = (offset - 1) * pageSize;

        List<Headpicture> list = dao.selectPage(Headpicture, page, pageSize, order);

        pageList.setList(list);
        pageList.setStartPageNo(offset);
        pageList.setPageSize(pageSize);
        pageList.setTotalCount(total);
        pageList.setTotalPageCount(totalPage);
        return pageList;
    }

    @Override
    public int total(Headpicture Headpicture) {
        return dao.total(Headpicture);
    }
}