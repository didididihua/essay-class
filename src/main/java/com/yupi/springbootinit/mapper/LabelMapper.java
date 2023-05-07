package com.yupi.springbootinit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yupi.springbootinit.model.entity.Label;
import org.apache.ibatis.annotations.Mapper;


/**
 * @author: tangchongjie
 * @creattime: 2023--05--05 13:10
 * @description 文章标签的操作
 */
@Mapper
public interface LabelMapper  extends BaseMapper<Label> {
}
