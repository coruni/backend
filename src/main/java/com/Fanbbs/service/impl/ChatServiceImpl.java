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
 * TypechoChatServiceImpl
 * @author buxia97
 * @date 2023/01/10
 */
@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
	ChatDao dao;

    @Override
    public int insert(Chat chat) {
        return dao.insert(chat);
    }

    @Override
    public int batchInsert(List<Chat> list) {
    	return dao.batchInsert(list);
    }

    @Override
    public int update(Chat chat) {
    	return dao.update(chat);
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
	public Chat selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<Chat> selectList(Chat chat) {
		return dao.selectList(chat);
	}

	@Override
	public PageList<Chat> selectPage(Chat chat, Integer offset, Integer pageSize, String order, String searchKey) {
		PageList<Chat> pageList = new PageList<>();

		int total = this.total(chat);

		int totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<Chat> list = dao.selectPage(chat, page, pageSize,order,searchKey);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(Chat chat) {
		return dao.total(chat);
	}
}