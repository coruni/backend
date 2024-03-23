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
 * TypechoFanServiceImpl
 * @author buxia97
 * @date 2023/01/03
 */
@Service
public class FanServiceImpl implements FanService {

    @Autowired
	FanDao dao;

    @Override
    public int insert(Fan fan) {
        return dao.insert(fan);
    }

    @Override
    public int batchInsert(List<Fan> list) {
    	return dao.batchInsert(list);
    }

    @Override
    public int update(Fan fan) {
    	return dao.update(fan);
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
	public Fan selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<Fan> selectList(Fan fan) {
		return dao.selectList(fan);
	}

	@Override
	public PageList<Fan> selectPage(Fan fan, Integer offset, Integer pageSize) {
		PageList<Fan> pageList = new PageList<>();

		int total = this.total(fan);

		int totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<Fan> list = dao.selectPage(fan, page, pageSize);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public PageList<Fan> selectUserPage(Fan fan, Integer offset, Integer pageSize) {
		PageList<Fan> pageList = new PageList<>();

		int total = this.total(fan);

		Integer totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<Fan> list = dao.selectPage(fan, page, pageSize);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(Fan fan) {
		return dao.total(fan);
	}
}