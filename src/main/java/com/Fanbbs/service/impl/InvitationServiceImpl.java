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
 * TypechoInvitationServiceImpl
 * @author invitation
 * @date 2022/05/03
 */
@Service
public class InvitationServiceImpl implements InvitationService {

    @Autowired
	InvitationDao dao;

    @Override
    public int insert(Invitation invitation) {
        return dao.insert(invitation);
    }

    @Override
    public int batchInsert(List<Invitation> list) {
    	return dao.batchInsert(list);
    }

    @Override
    public int update(Invitation invitation) {
    	return dao.update(invitation);
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
	public Invitation selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<Invitation> selectList(Invitation invitation) {
		return dao.selectList(invitation);
	}

	@Override
	public PageList<Invitation> selectPage(Invitation invitation, Integer offset, Integer pageSize) {
		PageList<Invitation> pageList = new PageList<>();

		int total = this.total(invitation);

		int totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<Invitation> list = dao.selectPage(invitation, page, pageSize);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(Invitation invitation) {
		return dao.total(invitation);
	}
}