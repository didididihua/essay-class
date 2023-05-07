package com.yupi.springbootinit.postclass;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yupi.springbootinit.model.entity.Label;
import com.yupi.springbootinit.service.LabelService;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * @author: tangchongjie
 * @creattime: 2023--05--05 10:34
 * @description
 */
@SpringBootTest
public class LabelTest {

    @Resource
    private LabelService labelService;

    @Test
    public void addLabel(){
        Label label = new Label();
        label.setLabelName("测试2");
        label.setParentId(0l);
        labelService.save(label);
    }


    @Test
    public void labelTest(){

        //标签分类的url
        String url = "https://www.code-nav.cn/api/tag/get/all_select";
        //发起http请求
        String body = HttpRequest.get(url)
                .timeout(20000)//超时，毫秒
                .execute().body();

        Map map = JSONUtil.toBean(body, Map.class);
        JSONObject data = (JSONObject) map.get("data");
        JSONObject categoryGroupTagsList = (JSONObject) data.get("categoryGroupTagsList");

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
     * 给label对象填充数据
     * @param parentId
     * @param labelName
     * @return
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

    public void parseLabel(Long parentId, JSONArray jsonArray){

        jsonArray.forEach(item ->{

            if(item instanceof String){
                Label label = getLabelObject(parentId, item.toString());
                labelService.save(label);
            }
            else {
                JSONObject jsonObject = (JSONObject) item;
                String name = (String) jsonObject.get("name");
                JSONArray labelArray = (JSONArray) jsonObject.get("tagList");

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


    @Test
    public void testTreeLabelData(){
//        System.out.println(url);
        labelService.getTreeLabelData();

    }

    @Autowired
    private RedissonClient redissonClient;

    @Test
    void contextLoads() {
        redissonClient.getBucket("hello").set("bug");
        String test = (String) redissonClient.getBucket("hello").get();
        System.out.println(test);

    }

}
