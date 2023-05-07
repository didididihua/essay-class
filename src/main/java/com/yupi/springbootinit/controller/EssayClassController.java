package com.yupi.springbootinit.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.springbootinit.common.BaseResponse;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.common.ResultUtils;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.exception.label.NullLabelDataException;
import com.yupi.springbootinit.model.dto.Label.PostLabelQueryRequest;
import com.yupi.springbootinit.model.entity.Post;
import com.yupi.springbootinit.model.vo.LabelVo;
import com.yupi.springbootinit.model.vo.PostVO;
import com.yupi.springbootinit.service.LabelService;
import com.yupi.springbootinit.service.PostLabelService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author: tangchongjie
 * @creattime: 2023--05--05 1:04
 * @description 对象从编程导航爬取的文章的相关数据的请求controller
 */
@RestController("/essay")
public class EssayClassController {

    @Resource
    private LabelService labelService;

    @Resource
    private PostLabelService postLabelService;

    /**
     * 返回层级结构（树状）的标签数据
     * @return 树状结构的label数据
     */
    @GetMapping("/labelTree")
    public BaseResponse<List<LabelVo>> getTreeLabelData(){
        List<LabelVo> treeLabelData = labelService.getTreeLabelData();

        //数据为空则返回异常
        if(ObjectUtil.isEmpty(treeLabelData)){
            throw new NullLabelDataException(ErrorCode.NO_LABEL_DATA);
        }
        return ResultUtils.success(treeLabelData);
    }

    /**
     * 用于开始从编程导航中爬取文章数据以及归档数据
     * @return
     */
    @GetMapping("/getPostData")
    public BaseResponse<String> getPostData(){
        postLabelService.getPostData();
        return ResultUtils.success("数据爬取成功");
    }

    /**
     * 通过标签获取到对应的post
     * @param postLabelQueryRequest 分页请求需要的数据
     * @return
     */
    @PostMapping("/getPostVOByLabelId")
    public BaseResponse<Page<Post>> getPostVOByLabelId(@RequestBody PostLabelQueryRequest postLabelQueryRequest){

        if (postLabelQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = postLabelQueryRequest.getCurrent();
        long size = postLabelQueryRequest.getPageSize();
        Long id = postLabelQueryRequest.getId();
        Page<Post> postPage = postLabelService.getPostVOByLabelId(current, size, id);

        return null;
    }


}
