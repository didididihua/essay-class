package com.yupi.springbootinit.service;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.springbootinit.model.entity.Post;
import com.yupi.springbootinit.model.entity.PostLabel;

import java.util.List;
import java.util.Map;

/**
 * @author: tangchongjie
 * @creattime: 2023--05--06 10:48
 * @description post文章与label标签的关系service
 */
public interface PostLabelService extends IService<PostLabel> {

    /**
     * 多线程用于爬取post数据与构建post与label的归档
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

    /**
     * 发出http请求，获取到最出的响应数据，判断成功与否，转换成map
     * @param json
     * @return
     */
    public Map getJSONDataMap(String json);

    /**
     * 讲json中解析出来的数据对post对象进行填充
     * @param jsonObject json数据
     * @return 填充好的post对象
     */
    public Post getPost(JSONObject jsonObject, List<PostLabel> postLabelList);

}
