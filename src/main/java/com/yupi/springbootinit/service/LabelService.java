package com.yupi.springbootinit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.springbootinit.model.entity.Label;
import com.yupi.springbootinit.model.vo.LabelVo;

import java.util.List;
import java.util.Map;


/**
 * @author: tangchongjie
 * @creattime: 2023--05--05 14:51
 * @description
 */
public interface LabelService extends IService<Label> {

    /**
     * 获取树状的label数据
     * @return 树状的label数据
     */
    List<LabelVo> getTreeLabelData();

    public Map<String, List<Long>> getLabelNameToIdMap(String list);
}
