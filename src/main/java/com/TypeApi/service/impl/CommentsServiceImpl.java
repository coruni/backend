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
 * TypechoCommentsServiceImpl
 * @author buxia97
 * @date 2021/11/29
 */
@Service
public class CommentsServiceImpl implements CommentsService {

	@Autowired
	CommentsDao dao;

	@Override
	public int insert(Comments comments) {
		return dao.insert(comments);
	}

	@Override
	public int batchInsert(List<Comments> list) {
		return dao.batchInsert(list);
	}

	@Override
	public int update(Comments comments) {
		return dao.update(comments);
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
	public Comments selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<Comments> selectList(Comments comments) {
		return dao.selectList(comments);
	}

	@Override
	public PageList<Comments> selectPage(Comments comments, Integer offset, Integer pageSize, String searchKey, String order) {
		PageList<Comments> pageList = new PageList<>();

		int total = this.total(comments,searchKey);

		int totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<Comments> list = dao.selectPage(comments, page, pageSize,searchKey,order);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(Comments comments, String searchKey) {
		return dao.total(comments,searchKey);
	}
}