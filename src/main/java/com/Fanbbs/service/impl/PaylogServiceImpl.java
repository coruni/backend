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
 * TypechoPaylogServiceImpl
 * @author buxia97
 * @date 2022/02/07
 */
@Service
public class PaylogServiceImpl implements PaylogService {

    @Autowired
	PaylogDao dao;

    @Override
    public int insert(Paylog paylog) {
        return dao.insert(paylog);
    }

    @Override
    public int batchInsert(List<Paylog> list) {
    	return dao.batchInsert(list);
    }

    @Override
    public int update(Paylog paylog) {
    	return dao.update(paylog);
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
	public Paylog selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<Paylog> selectList(Paylog paylog) {
		return dao.selectList(paylog);
	}

	@Override
	public PageList<Paylog> selectPage(Paylog paylog, Integer offset, Integer pageSize) {
		PageList<Paylog> pageList = new PageList<>();

		int total = this.total(paylog);

		int totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<Paylog> list = dao.selectPage(paylog, page, pageSize);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(Paylog paylog) {
		return dao.total(paylog);
	}
}