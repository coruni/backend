package com.TypeApi.service.impl;

import com.TypeApi.common.PageList;
import com.TypeApi.dao.RaffleDao;
import com.TypeApi.entity.Raffle;
import com.TypeApi.service.RaffleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 业务层实现类
 * TypechoRaffleServiceImpl
 * @author raffle
 * @date 2022/09/06
 */
@Service
public class RaffleServiceImpl implements RaffleService {

    @Autowired
	RaffleDao dao;

    @Override
    public int insert(Raffle raffle) {
        return dao.insert(raffle);
    }


    @Override
    public int update(Raffle raffle) {
    	return dao.update(raffle);
    }

    @Override
    public int delete(Object key) {
    	return dao.delete(key);
    }


	@Override
	public Raffle selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<Raffle> selectList(Raffle raffle) {
		return dao.selectList(raffle);
	}

	@Override
	public PageList<Raffle> selectPage(Raffle raffle, Integer offset, Integer pageSize, String searchKey) {
		PageList<Raffle> pageList = new PageList<>();

		int total = this.total(raffle);

		Integer totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<Raffle> list = dao.selectPage(raffle, page, pageSize,searchKey);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(Raffle raffle) {
		return dao.total(raffle);
	}
}