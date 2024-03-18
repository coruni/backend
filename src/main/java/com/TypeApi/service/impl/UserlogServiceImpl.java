package com.TypeApi.service.impl;

import com.TypeApi.entity.*;
import com.TypeApi.common.PageList;
import com.TypeApi.dao.*;
import com.TypeApi.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 业务层实现类
 * TypechoUserlogServiceImpl
 * @author buxia97
 * @date 2022/01/06
 */
@Service
public class UserlogServiceImpl implements UserlogService {

    @Autowired
	UserlogDao dao;

    @Override
    public int insert(Userlog userlog) {
        return dao.insert(userlog);
    }

    @Override
    public int batchInsert(List<Userlog> list) {
    	return dao.batchInsert(list);
    }

    @Override
    public int update(Userlog userlog) {
    	return dao.update(userlog);
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
	public Userlog selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<Userlog> selectList(Userlog userlog) {
		return dao.selectList(userlog);
	}

	@Override
	public PageList<Userlog> selectPage(Userlog userlog, Integer offset, Integer pageSize) {
		PageList<Userlog> pageList = new PageList<>();

		int total = this.total(userlog);

		int totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<Userlog> list = dao.selectPage(userlog, page, pageSize);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(Userlog userlog) {
		return dao.total(userlog);
	}
}