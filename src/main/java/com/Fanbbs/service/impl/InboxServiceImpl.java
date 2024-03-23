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
 * TypechoInboxServiceImpl
 * @author inbox
 * @date 2022/12/29
 */
@Service
public class InboxServiceImpl implements InboxService {

    @Autowired
	InboxDao dao;

    @Override
    public int insert(Inbox inbox) {
        return dao.insert(inbox);
    }

    @Override
    public int update(Inbox inbox) {
    	return dao.update(inbox);
    }

    @Override
    public int delete(Object key) {
    	return dao.delete(key);
    }

	@Override
	public Inbox selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<Inbox> selectList(Inbox inbox) {
		return dao.selectList(inbox);
	}

	@Override
	public PageList<Inbox> selectPage(Inbox inbox, Integer offset, Integer pageSize) {
		PageList<Inbox> pageList = new PageList<>();

		int total = this.total(inbox);

		int totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<Inbox> list = dao.selectPage(inbox, page, pageSize);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(Inbox inbox) {
		return dao.total(inbox);
	}
}