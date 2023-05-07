package com.yupi.springbootinit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yupi.springbootinit.model.entity.PostLabel;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author: tangchongjie
 * @creattime: 2023--05--05 13:11
 * @description 文章与标签的关系操作
 */
@Mapper
public interface PostLabelMapper extends BaseMapper<PostLabel> {
}
