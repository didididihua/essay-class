package com.yupi.springbootinit.postclass;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.constant.RedisKeyConstant;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.model.entity.Label;
import com.yupi.springbootinit.model.entity.Post;
import com.yupi.springbootinit.model.entity.PostLabel;
import com.yupi.springbootinit.service.LabelService;
import com.yupi.springbootinit.service.PostLabelService;
import com.yupi.springbootinit.service.PostService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author: tangchongjie
 * @creattime: 2023--05--05 22:08
 * @description
 */
@SpringBootTest
public class PostTest {

    @Resource
    private PostService postService;

    @Resource
    private PostLabelService postLabelService;

    @Resource
    private LabelService labelService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 用于存储labelName与labelId的映射map
     */
    private Map<String, List<Long>> labelNameToIdMap = new HashMap<>();


    @Test
    public void testGetPost(){
        //向编程导航发送请求，获取文章数据
        String json = "{\"current\":1,\"reviewStatus\":1,\"sortField\":\"createTime\",\"sortOrder\":\"descend\"}";
        String url = "https://www.code-nav.cn/api/post/list/page/vo";

        String result2 = HttpRequest.post(url)
                .body(json)//表单内容
                .timeout(20000)//超时，毫秒
                .execute().body();

        Map map = JSONUtil.toBean(result2, Map.class);
        JSONObject data = (JSONObject) map.get("data");
        JSONArray records = (JSONArray) data.get("records");

        //遍历转换成Post
        ArrayList<Post> postList = new ArrayList<Post>();
        ArrayList<PostLabel> postLabelList = new ArrayList<>();

        records.forEach(item -> {

            JSONObject jsonObject = (JSONObject) item;
            //填充数据,获取到一个post对象
            Post post = getPost(jsonObject, postLabelList);

            //向集合中添加post
            postList.add(post);
            postService.save(post);
//            if(list.size() > 20){
//                postService.saveBatch(list);
//                list.clear();
//            }
        });

        if(postList.size() > 0){
            postService.saveBatch(postList);
            postLabelService.saveBatch(postLabelList);
            postList.clear();
        }

        //todo:存入数据库
        //todo:游标
    }

    /**
     * 讲json中解析出来的数据对post对象进行填充
     * @param jsonObject json数据
     * @return 填充好的post对象
     */
    public Post getPost(JSONObject jsonObject, List<PostLabel> postLabelList){

        Post post = new Post();

        //获取文章较为核心的数据
        String title = jsonObject.getStr("title");
        String content = jsonObject.getStr("content");
        String postId = jsonObject.getStr("id");
        JSONArray array = (JSONArray) jsonObject.get("tags");

        //获取发布文章的用户
        JSONObject user = (JSONObject) jsonObject.get("user");
        String userName = user.getStr("userName");
        String userId = jsonObject.getStr("userId");

        //获取标签
        String tagsStr = JSONUtil.toJsonStr(array);
        if(StrUtil.isAllBlank(title, content, tagsStr, userName, userId, postId)){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "初始化Post数据失败，有数据为空");
        }

        //获取其他信息
        String cover = jsonObject.getStr("cover");
        String description = jsonObject.getStr("description");
        String favourNum = jsonObject.getStr("favourNum");
        Object files = jsonObject.get("fileList");
        List<String> fileList = JSONArrayToList(files);

        String hasFavour = jsonObject.getStr("hasFavour");
        String hasThumb = jsonObject.getStr("hasThumb");
        String language = jsonObject.getStr("language");
        String needVip = jsonObject.getStr("needVip");
        Object pictures = jsonObject.get("pictureList");
        List<String> pictureList = JSONArrayToList(pictures);

        String priority = jsonObject.getStr("priority");
        String reviewMessage = jsonObject.getStr("reviewMessage");
        String reviewStatus = jsonObject.getStr("reviewStatus");
        String reviewTime = jsonObject.getStr("reviewTime");
        String reviewerId = jsonObject.getStr("reviewerId");
        String commentNum = jsonObject.getStr("commentNum");
        Object videos = jsonObject.get("videoList");
        List<String> videoList = JSONArrayToList(videos);
        String viewNum = jsonObject.getStr("viewNum");


        post.setTitle(title);
        post.setContent(content);
        post.setTags(tagsStr);
        post.setUserId(1L);
        post.setCreateTime(new Date());
        post.setUpdateTime(new Date());
        long id = Long.parseLong(postId);
        post.setPostId(id);
        post.setUserId(Long.parseLong(userId));
        post.setUserName(userName);
        post.setCover(cover);
        post.setDescription(description);
        post.setFavourNum(Integer.parseInt(favourNum));
        post.setHasFavour(Boolean.parseBoolean(hasFavour));
        post.setHasThumb(Boolean.parseBoolean(hasThumb));
        post.setLanguage(language);
        post.setNeedVip(needVip);
        post.setPriority(Long.parseLong(priority));
        post.setReviewMessage(reviewMessage);
        post.setReviewStatus(Integer.parseInt(reviewStatus));
        post.setReviewTime(reviewTime);
        post.setCommentNum(Long.parseLong(commentNum));
        post.setViewNum(Long.parseLong(viewNum));
        post.setFileList(fileList);
        post.setPictureList(pictureList);
        post.setVideoList(videoList);

        if(!StrUtil.isBlank(reviewerId)){
            post.setReviewerId(Long.parseLong(reviewerId));
        }

        //创建post与label的关系（创建PostLabel对象）
        getPostLabel(array, postLabelList, id);

        return post;
    }

    /**
     * 本方法用于在爬取一个post对象的时候, 来创建post对象与label的关系，实现按标签归档
     * @param array
     * @return
     */
    private void getPostLabel(JSONArray array, List<PostLabel> postLabelList, Long postId) {

        for (Object o : array) {
            String labelName = o.toString();
            List<Long> labelIds = getLabelIdByLabelName(labelName);

            for (Long labelId : labelIds) {
                PostLabel postLabel = new PostLabel();
                postLabel.setPostId(postId);

                //根据labelName获取到labelId
                postLabel.setLabelId(labelId);
                postLabel.setCreateTime(new Date());
                postLabel.setUpdateTime(new Date());

                //添加到暂存列表中，等待批量插入
                postLabelList.add(postLabel);
            }
        }
    }

    /**
     * 根据label的name获取到label的id
     * @param labelName 标签名称
     * @return 标签id
     */
    private List<Long> getLabelIdByLabelName(String labelName){

        //从redis中获取
        String map = stringRedisTemplate.opsForValue().get(RedisKeyConstant.LABEL_MAP_KEY);
        //redis中没有，则去重建一下
        if(StrUtil.isBlank(map)){
            reBuildRedisLabelNameToIdMap();
        }
        //为labelNameToIdMap赋值
        getLabelNameToIdMap(map);

        return labelNameToIdMap.get(labelName);
    }


    /**
     * 重建一下redis中的标签的数据
     */
    private void reBuildRedisLabelNameToIdMap() {

        String labelVosJSONStr = stringRedisTemplate.opsForValue().get(RedisKeyConstant.LABEL_DATA_KEY);
        if(StrUtil.isBlank(labelVosJSONStr)){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "redis中的标签数据为空");
        }
        Map<String, List<Long>> labelNameToIdMap = labelService.getLabelNameToIdMap(labelVosJSONStr);

        String labelNameToMapMapJSONStr = JSONUtil.toJsonStr(labelNameToIdMap);
        stringRedisTemplate.opsForValue().set(RedisKeyConstant.LABEL_MAP_KEY, labelNameToMapMapJSONStr);

    }

    /**
     * 该方法用于初始化根label的name与id映射
     */
    public void getLabelNameToIdMap(String map){
        if(labelNameToIdMap.size() == 0){
            synchronized (this.labelNameToIdMap){
                if(labelNameToIdMap.size() == 0){
                    doGetLabelNameToIdMap(map);
                }
            }

        }
    }

    /**
     * 单个线程得到初始化label的name与id映射的锁，开始执行初始化
     */
    public void doGetLabelNameToIdMap(String map){
        //反序列化
        Map<String, List<Long>> labelNameToIdMap = JSONUtil.toBean(map, new TypeReference<Map<String, List<Long>>>() {
        }, false);
        this.labelNameToIdMap = labelNameToIdMap;
    }


    /**
     * 讲JSONArray里面的数据转成list
     * @param object
     * @return
     */
    public List<String> JSONArrayToList(Object object){

        if(ObjectUtil.isEmpty(object) || JSONUtil.isNull(object)){
            return null;
        }

        JSONArray jsonArray = (JSONArray) object;
        List<String> targetList = new ArrayList<>();
        for (Object item : jsonArray) {
            targetList.add(item.toString());
        }

        return targetList;
    }

    @Test
    public void testPost(){
        postLabelService.getPostData();
    }

    @Test
    public void testGetPostByLabelId(){
        postLabelService.getPostVOByLabelId(1, 5, 125l);
    }

}
