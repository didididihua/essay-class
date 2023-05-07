package com.yupi.springbootinit.job.once;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.model.entity.Label;
import com.yupi.springbootinit.model.vo.LabelVo;
import com.yupi.springbootinit.service.LabelService;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.similarities.LambdaDF;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: tangchongjie
 * @creattime: 2023--05--05 15:43
 * @description 在程序启动时启动一次，将编程导航中的标签分类构造成树形结构导入
 */
@Component
@Slf4j
public class PostLabelData implements CommandLineRunner {

    public static final String LABEL_DATA_URL = "https://www.code-nav.cn/api/tag/get/all_select";

    /**
     * 响应的json中状态码的json字段名
     */
    public static String CODE = "code";

    /**
     * 向编程导航请求数据后得成功状态码
     */
    public static Integer SUCCESS_CODE = 0;

    /**
     * 根标签得父id
     */
    public static Long PARENT_ID = 0l;

    /**
     * 响应的json中数据的json字段名
     */
    public static String URL_DATA = "data";

    /**
     * json数据中包含标签以及其分层关系的数据的json字段名
     */
    public static String CATEGORY_LIST = "categoryGroupTagsList";

    /**
     * 热门标签
     */
    public static String COMMON_GROUP_TAGS_LIST = "commonGroupTagsList";

    /**
     * 所有标签
     */
    public static String ALL_TAG_LIST = "allTagList";

    /**
     * 标签的标签名的json字段名,用于获取除了最后一层标签（String数组）的标签名称
     */
    public static String LABEL_NAME = "name";

    public static String TAG_LIST = "tagList";

    @Resource
    private LabelService labelService;

    @Override
    public void run(String... args){

        //发起http请求
        String body = HttpRequest.get(LABEL_DATA_URL)
                .timeout(20000)//超时，毫秒
                .execute().body();

        Map map = JSONUtil.toBean(body, Map.class);

        if(ObjectUtil.isEmpty(map.get(CODE)) || !SUCCESS_CODE.equals(map.get(CODE))){
            System.out.println("向编程导航网站请求标签数据失败");
            throw new BusinessException(ErrorCode.HTTP_REQUEST_FAIL, "爬取标签数据失败");
        }

        //获取数据
        JSONObject data = (JSONObject) map.get(URL_DATA);

        //解析标签数据
        JSONObject categoryGroupTagsList = (JSONObject) data.get(CATEGORY_LIST);
        JSONArray commonGroupTagsList = (JSONArray) data.get(COMMON_GROUP_TAGS_LIST);
        JSONArray allTagsList = (JSONArray) data.get(ALL_TAG_LIST);

        //入库分层的标签
        addLabelToDB(categoryGroupTagsList);
        //入库热门标签
        addLabelToDB(commonGroupTagsList);
        //入库其他的标签
        addOtherTags(allTagsList);
        log.info("标签数据解析、入库成功");

    }

    /**
     * 排除一下已存在的标签后添加一下其他的标签
     * @param allTagsList
     */
    private void addOtherTags(JSONArray allTagsList) {

        //从数据库中查出labelName数据，放入set
        LambdaQueryWrapper<Label> wrapper = new LambdaQueryWrapper();
        wrapper.select(Label::getLabelName);
        List<Label> list = labelService.list(wrapper);
        //转成set
        Set<String> LabelNameSet = list.stream()
                .map(item -> item.getLabelName())
                .collect(Collectors.toSet());


        List<Label> labelList = new ArrayList<>();
        allTagsList.forEach(item -> {
            String labelName = item.toString();
            //排除一下，数据库中已经存在得就不要再存了
            if (!LabelNameSet.contains(labelName)) {
                //存入
                Label label = getLabelObject(PARENT_ID, labelName);
                labelList.add(label);
            }
        });

        labelService.saveBatch(labelList);
        log.info("其他的标签入库成功");
    }


    /**
     *
     * @param list
     */
    public void addLabelToDB(JSONObject list){
        //获取第一级的key，也是获取分类标签
        Set<String> cateKeys = list.keySet();

        for(String cateKey : cateKeys){
            //第一级标签（分类）
            //todo: 存库
            Label cateLabel = getLabelObject(0l, cateKey);
            labelService.save(cateLabel);

            JSONArray cateValue = (JSONArray) list.get(cateKey);

            //有二级标签是才进行下一级的解析
            if(!ObjectUtil.isEmpty(cateValue) && !ObjectUtil.isEmpty(cateLabel.getId())){
                //递归解析二级标签
                parseLabel(cateLabel.getId(), cateValue);
            }
        }
    }

    public void addLabelToDB(JSONArray array){
        parseLabel(0l, array);
    }


    /**
     * 创建label对象
     * @param parentId label对象的父id
     * @param labelName label对象的名称
     * @return label对象
     */
    public Label getLabelObject(Long parentId, String labelName){
        Label label = new Label();
        label.setLabelName(labelName);
        label.setParentId(parentId);
        label.setCreateTime(new Date());
        label.setUpdateTime(new Date());
        label.setIsDelete(0);

        return label;
    }

    /**
     * 解析标签数据
     * @param parentId label对象的父id
     * @param jsonArray 下一层的json数组
     */
    public void parseLabel(Long parentId, JSONArray jsonArray){

        jsonArray.forEach(item ->{

            //是String的话表示到了最后一层，直接创建label对象入库
            if(item instanceof String){
                Label label = getLabelObject(parentId, item.toString());
                labelService.save(label);
            }
            else {

                JSONObject jsonObject = (JSONObject) item;
                String name = (String) jsonObject.get(LABEL_NAME);
                JSONArray labelArray = (JSONArray) jsonObject.get(TAG_LIST);

                //todo: 存库
                Label label = getLabelObject(parentId, name);
                labelService.save(label);

                //开始递归创建子标签
                if(!ObjectUtil.isEmpty(labelArray) && !ObjectUtil.isEmpty(label.getId())){
                    parseLabel(label.getId(), labelArray);
                }
            }
        });
    }
}
