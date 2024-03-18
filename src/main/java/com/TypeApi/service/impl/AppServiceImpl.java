package com.TypeApi.service.impl;

import com.TypeApi.common.PageList;
import com.TypeApi.dao.AppDao;
import com.TypeApi.entity.App;
import com.TypeApi.service.AppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 业务层实现类
 * TypechoAppServiceImpl
 * @author vips
 * @date 2023/06/09
 */
@Service
public class AppServiceImpl implements AppService {

    @Autowired
	AppDao dao;

    @Override
    public int insert(App app) {
        return dao.insert(app);
    }

    @Override
    public int batchInsert(List<App> list) {
    	return dao.batchInsert(list);
    }

    @Override
    public int update(App app) {
    	return dao.update(app);
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
	public App selectByKey(Object key) {
		return dao.selectByKey(key);
	}

	@Override
	public List<App> selectList(App app) {
		return dao.selectList(app);
	}

	@Override
	public PageList<App> selectPage(App app, Integer offset, Integer pageSize) {
		PageList<App> pageList = new PageList<>();

		int total = this.total(app);

		int totalPage;
		if (total % pageSize != 0) {
			totalPage = (total /pageSize) + 1;
		} else {
			totalPage = total /pageSize;
		}

		int page = (offset - 1) * pageSize;

		List<App> list = dao.selectPage(app, page, pageSize);

		pageList.setList(list);
		pageList.setStartPageNo(offset);
		pageList.setPageSize(pageSize);
		pageList.setTotalCount(total);
		pageList.setTotalPageCount(totalPage);
		return pageList;
	}

	@Override
	public int total(App app) {
		return dao.total(app);
	}
}