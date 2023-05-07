package com.yupi.springbootinit.job.once;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yupi.springbootinit.model.entity.Label;
import com.yupi.springbootinit.service.LabelService;
import org.springframework.boot.CommandLineRunner;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * @author: tangchongjie
 * @creattime: 2023--05--05 15:43
 * @description 在程序启动时启动一次，将编程导航中的标签分类构造成树形结构导入
 */
public class PostLabelData implements CommandLineRunner {

    public static final String LABEL_DATA_URL = "https://www.code-nav.cn/api/tag/get/all_select";

    /**
     * 响应的json中数据的json字段名
     */
    public static String URL_DATA = "data";

    /**
     * json数据中包含标签以及其分层关系的数据的json字段名
     */
    public static String CATEGORY_LIST = "categoryGroupTagsList";

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
        JSONObject data = (JSONObject) map.get(URL_DATA);
        JSONObject categoryGroupTagsList = (JSONObject) data.get(CATEGORY_LIST);

        //获取第一级的key，也是获取分类标签
        Set<String> cateKeys = categoryGroupTagsList.keySet();

        for(String cateKey : cateKeys){
            //第一级标签（分类）
            //todo: 存库
            Label cateLabel = getLabelObject(0l, cateKey);
            labelService.save(cateLabel);

            JSONArray cateValue = (JSONArray) categoryGroupTagsList.get(cateKey);

            //有二级标签是才进行下一级的解析
            if(!ObjectUtil.isEmpty(cateValue) && !ObjectUtil.isEmpty(cateLabel.getId())){
                //递归解析二级标签
                parseLabel(cateLabel.getId(), cateValue);
            }
        }
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
