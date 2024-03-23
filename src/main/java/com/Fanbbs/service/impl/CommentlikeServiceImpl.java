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
 * TypechoCommentLikeServiceImpl
 * @author commentLike
 * @date 2024/01/06
 */
@Service
public class CommentlikeServiceImpl implements CommentlikeService {

    @Autowired
    CommentLikeDao dao;

    @Override
    public int insert(CommentLike commentLike) {
        return dao.insert(commentLike);
    }


    @Override
    public int update(CommentLike commentLike) {
        return dao.update(commentLike);
    }

    @Override
    public int delete(Object key) {
        return dao.delete(key);
    }


    @Override
    public CommentLike selectByKey(Object key) {
        return dao.selectByKey(key);
    }

    @Override
    public List<CommentLike> selectList(CommentLike commentLike) {
        return dao.selectList(commentLike);
    }

    @Override
    public PageList<CommentLike> selectPage(CommentLike commentLike, Integer offset, Integer pageSize) {
        PageList<CommentLike> pageList = new PageList<>();

        int total = this.total(commentLike);

        int totalPage;
        if (total % pageSize != 0) {
            totalPage = (total /pageSize) + 1;
        } else {
            totalPage = total /pageSize;
        }

        int page = (offset - 1) * pageSize;

        List<CommentLike> list = dao.selectPage(commentLike, page, pageSize);

        pageList.setList(list);
        pageList.setStartPageNo(offset);
        pageList.setPageSize(pageSize);
        pageList.setTotalCount(total);
        pageList.setTotalPageCount(totalPage);
        return pageList;
    }

    @Override
    public int total(CommentLike commentLike) {
        return dao.total(commentLike);
    }
}