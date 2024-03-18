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
 * TypechoAdsServiceImpl
 * @author ads
 * @date 2022/09/06
 */
@Service
public class AdsServiceImpl implements AdsService {

    @Autowired
	AdsDao dao;

    @Override
    public int insert(Ads ads) {
        return dao.insert(ads);
    }


    @Override
    public int update(Ads ads) {
    	return dao.update(ads);
    }

    @Override
    public int delete(Object key) {
    	return dao.delete(key);
    }


	@Override
	public Ads selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<Ads> selectList(Ads ads) {
		return dao.selectList(ads);
	}

	@Override
	public PageList<Ads> selectPage(Ads ads, Integer offset, Integer pageSize, String searchKey) {
		PageList<Ads> pageList = new PageList<>();

		int total = this.total(ads);

		int totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<Ads> list = dao.selectPage(ads, page, pageSize,searchKey);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(Ads ads) {
		return dao.total(ads);
	}
}