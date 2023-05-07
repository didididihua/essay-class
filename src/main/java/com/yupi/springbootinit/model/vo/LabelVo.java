package com.yupi.springbootinit.model.vo;

import com.yupi.springbootinit.model.entity.Label;
import lombok.Data;

import java.util.List;

/**
 * @author: tangchongjie
 * @creattime: 2023--05--05 16:05
 * @description 用于展示标签的多级树状结构数据
 */
@Data
public class LabelVo {

    /**
     * 标签id
     */
    private Long id;

    /**
     * 标签名称
     */
    private String labelName;


    /**
     * 父标签id
     */
    private Long parentId;

    /**
     * 子标签数组
     */
    private List<LabelVo> childrenLabel;

}
