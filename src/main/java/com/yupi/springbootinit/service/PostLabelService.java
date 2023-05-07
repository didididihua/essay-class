package com.yupi.springbootinit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.springbootinit.model.entity.Post;
import com.yupi.springbootinit.model.entity.PostLabel;

/**
 * @author: tangchongjie
 * @creattime: 2023--05--06 10:48
 * @description post文章与label标签的关系service
 */
public interface PostLabelService extends IService<PostLabel> {

    /**
     * 用于爬取post数据与构建post与label的归档
     */
    void getPostData();

    /**
     * 根据labelId分页获取到带有其标签的post
     * @param current 当前页
     * @param size 页面大小
     * @param id labelId
     * @return
     */
    Page<Post> getPostVOByLabelId(long current, long size, Long id);
}
