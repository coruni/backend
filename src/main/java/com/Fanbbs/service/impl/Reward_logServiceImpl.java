package com.Fanbbs.service.impl;

import com.Fanbbs.common.PageList;
import com.Fanbbs.dao.Reward_logDao;
import com.Fanbbs.entity.Reward_log;
import com.Fanbbs.service.Reward_logService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 业务层实现类
 * TypechoReward_logServiceImpl
 * @author reward_log
 * @date 2022/09/06
 */
@Service
public class Reward_logServiceImpl implements Reward_logService {

    @Autowired
	Reward_logDao dao;

    @Override
    public int insert(Reward_log reward_log) {
        return dao.insert(reward_log);
    }


    @Override
    public int update(Reward_log reward_log) {
    	return dao.update(reward_log);
    }

    @Override
    public int delete(Object key) {
    	return dao.delete(key);
    }


	@Override
	public Reward_log selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<Reward_log> selectList(Reward_log reward_log) {
		return dao.selectList(reward_log);
	}

	@Override
	public PageList<Reward_log> selectPage(Reward_log reward_log, Integer offset, Integer pageSize, String searchKey) {
		PageList<Reward_log> pageList = new PageList<>();

		int total = this.total(reward_log);

		int totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<Reward_log> list = dao.selectPage(reward_log, page, pageSize,searchKey);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(Reward_log reward_log) {
		return dao.total(reward_log);
	}
}