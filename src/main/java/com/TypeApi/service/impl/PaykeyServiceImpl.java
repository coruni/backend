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
 * TypechoPaykeyServiceImpl
 * @author paykey
 * @date 2022/04/20
 */
@Service
public class PaykeyServiceImpl implements PaykeyService {

    @Autowired
	PaykeyDao dao;

    @Override
    public int insert(Paykey paykey) {
        return dao.insert(paykey);
    }

    @Override
    public int batchInsert(List<Paykey> list) {
    	return dao.batchInsert(list);
    }

    @Override
    public int update(Paykey paykey) {
    	return dao.update(paykey);
    }

    @Override
    public int delete(Object key) {
    	return dao.delete(key);
    }

	@Override
	public int typeDelete(Object type) {return dao.typeDelete(type);}
    @Override
    public int batchDelete(List<Object> keys) {
        return dao.batchDelete(keys);
    }

	@Override
	public Paykey selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public Paykey selectByCard(Object card) {
		return dao.selectByCard(card);
	}

	@Override
	public List<Paykey> selectList(Paykey paykey) {
		return dao.selectList(paykey);
	}

	@Override
	public PageList<Paykey> selectPage(Paykey paykey, Integer offset, Integer pageSize, String searchKey) {
		PageList<Paykey> pageList = new PageList<>();

		int total = this.total(paykey);

		Integer totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<Paykey> list = dao.selectPage(paykey, page, pageSize,searchKey);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(Paykey paykey) {
		return dao.total(paykey);
	}
}