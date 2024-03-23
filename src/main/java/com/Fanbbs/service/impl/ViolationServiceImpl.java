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
 * TypechoViolationServiceImpl
 * @author buxia97
 * @date 2023/01/03
 */
@Service
public class ViolationServiceImpl implements ViolationService {

    @Autowired
	ViolationDao dao;

    @Override
    public int insert(Violation violation) {
        return dao.insert(violation);
    }

    @Override
    public int batchInsert(List<Violation> list) {
    	return dao.batchInsert(list);
    }

    @Override
    public int update(Violation violation) {
    	return dao.update(violation);
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
	public Violation selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<Violation> selectList(Violation violation) {
		return dao.selectList(violation);
	}

	@Override
	public PageList<Violation> selectPage(Violation violation, Integer offset, Integer pageSize) {
		PageList<Violation> pageList = new PageList<>();

		int total = this.total(violation);

		int totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<Violation> list = dao.selectPage(violation, page, pageSize);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(Violation violation) {
		return dao.total(violation);
	}
}