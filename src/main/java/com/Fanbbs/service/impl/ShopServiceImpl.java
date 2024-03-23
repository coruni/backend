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
 * TypechoShopServiceImpl
 * @author buxia97
 * @date 2022/01/27
 */
@Service
public class ShopServiceImpl implements ShopService {

	@Autowired
	ShopDao dao;

	@Override
	public int insert(Shop shop) {
		return dao.insert(shop);
	}

	@Override
	public int batchInsert(List<Shop> list) {
		return dao.batchInsert(list);
	}

	@Override
	public int update(Shop shop) {
		return dao.update(shop);
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
	public Shop selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<Shop> selectList(Shop shop) {
		return dao.selectList(shop);
	}

	@Override
	public PageList<Shop> selectPage(Shop shop, Integer offset, Integer pageSize, String searchKey, String order) {
		PageList<Shop> pageList = new PageList<>();

		int total = this.total(shop,searchKey);

		int totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<Shop> list = dao.selectPage(shop, page, pageSize,searchKey,order);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(Shop shop, String searchKey) {
		return dao.total(shop,searchKey);
	}
}