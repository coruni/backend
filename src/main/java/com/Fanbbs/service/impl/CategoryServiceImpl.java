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
 * TypechoMetasServiceImpl
 * @author buxia97
 * @date 2021/11/29
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
	CategoryDao dao;

    @Override
    public int insert(Category category) {
        return dao.insert(category);
    }

    @Override
    public int batchInsert(List<Category> list) {
    	return dao.batchInsert(list);
    }

    @Override
    public int update(Category category) {
    	return dao.update(category);
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
	public Category selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public Category selectBySlug(Object slug) {
		return dao.selectBySlug(slug);
	}

	@Override
	public List<Category> selectList(Category category) {
		return dao.selectList(category);
	}

	@Override
	public PageList<Category> selectPage(Category category, Integer offset, Integer pageSize, String searchKey, String order) {
		PageList<Category> pageList = new PageList<>();

		int total = this.total(category);

		int totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<Category> list = dao.selectPage(category, page, pageSize,searchKey,order);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(Category category) {
		return dao.total(category);
	}
}