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
 * TypechoShoptypeServiceImpl
 * @author shoptype
 * @date 2023/07/10
 */
@Service
public class ShoptypeServiceImpl implements ShoptypeService {

	@Autowired
	ShoptypeDao dao;

	@Override
	public int insert(Shoptype shoptype) {
		return dao.insert(shoptype);
	}

	@Override
	public int batchInsert(List<Shoptype> list) {
		return dao.batchInsert(list);
	}

	@Override
	public int update(Shoptype shoptype) {
		return dao.update(shoptype);
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
	public Shoptype selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<Shoptype> selectList(Shoptype shoptype) {
		return dao.selectList(shoptype);
	}

	@Override
	public PageList<Shoptype> selectPage(Shoptype shoptype, Integer offset, Integer pageSize, String searchKey, String order) {
		PageList<Shoptype> pageList = new PageList<>();

		int total = this.total(shoptype);

		int totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<Shoptype> list = dao.selectPage(shoptype, page, pageSize, searchKey,order);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(Shoptype shoptype) {
		return dao.total(shoptype);
	}
}