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
 * TypechoRelationshipsServiceImpl
 * @author buxia97
 * @date 2021/11/29
 */
@Service
public class RelationshipsServiceImpl implements RelationshipsService {

    @Autowired
	RelationshipsDao dao;

    @Override
    public int insert(Relationships relationships) {
        return dao.insert(relationships);
    }

    @Override
    public int batchInsert(List<Relationships> list) {
    	return dao.batchInsert(list);
    }

    @Override
    public int update(Relationships relationships) {
    	return dao.update(relationships);
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
	public List<Relationships> selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<Relationships> selectList(Relationships relationships) {
		return dao.selectList(relationships);
	}

	@Override
	public PageList<Relationships> selectPage(Relationships relationships, Integer offset, Integer pageSize) {
		PageList<Relationships> pageList = new PageList<>();

		int total = this.total(relationships);

		int totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<Relationships> list = dao.selectPage(relationships, page, pageSize);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(Relationships relationships) {
		return dao.total(relationships);
	}
}