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
 * TypechoUserapiServiceImpl
 * @author buxia97
 * @date 2022/01/10
 */
@Service
public class UserapiServiceImpl implements UserapiService {

    @Autowired
	UserapiDao dao;

    @Override
    public int insert(Userapi userapi) {
        return dao.insert(userapi);
    }

    @Override
    public int batchInsert(List<Userapi> list) {
    	return dao.batchInsert(list);
    }

    @Override
    public int update(Userapi userapi) {
    	return dao.update(userapi);
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
	public Userapi selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<Userapi> selectList(Userapi userapi) {
		return dao.selectList(userapi);
	}

	@Override
	public PageList<Userapi> selectPage(Userapi userapi, Integer offset, Integer pageSize) {
		PageList<Userapi> pageList = new PageList<>();

		int total = this.total(userapi);

		int totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<Userapi> list = dao.selectPage(userapi, page, pageSize);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(Userapi userapi) {
		return dao.total(userapi);
	}
}