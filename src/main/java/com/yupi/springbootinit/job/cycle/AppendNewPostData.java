package com.yupi.springbootinit.job.cycle;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.constant.CrawlPostConstant;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.model.entity.Post;
import com.yupi.springbootinit.model.entity.PostLabel;
import com.yupi.springbootinit.service.PostLabelService;
import com.yupi.springbootinit.service.PostService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: tangchongjie
 * @creattime: 2023--05--07 16:06
 * @description 增量爬取编程导航中的文章数据,每分钟执行一次任务,从数据库中获取到最新的post的postId,
 *              与此次http请求得到的数据的postId进行比较,如果大于游标则表示是新的post（此处的postId是
 *              编程导航文章的主键，是无序但总体递增的，所以可以进行比较）
 */
//@Component
@Slf4j
public class AppendNewPostData {

    @Resource
    private PostService postService;

    @Resource
    private PostLabelService postLabelService;

    /**
     * 上一次跟新爬取的文章数据的postId,这个值不是本数据库中的主键，用于判断此次爬取是否有新的post需要爬取
     */
    public Long postCursor;

    /**
     * 暂存post数据的list
     */
    private ArrayList<Post> postList = new ArrayList<Post>();

    /**
     * 暂存ostLabel数据的list
     */
    private ArrayList<PostLabel> postLabelList = new ArrayList<>();

    /**
     * 使用动态代理重试获取数据的次数
     */
    public static final int RETRY_TIMES = 10;

    /**
     * 动态代理的ip池
     */
    public ArrayList<String> ipPool = new ArrayList<>();

    /**
     * 动态代理的ip对应的port池
     */
    public ArrayList<Integer> portPool = new ArrayList<>();


    /**
     * 每分钟执行一下
     */
    @Scheduled(fixedRate = 60 * 1000)
    public void AppendNewPost(){

        //更新游标
        updatePostCus();
        log.info("latest postId：{}", postCursor);

        //初始化动态代理池
        try {
            if(!ObjectUtil.isAllEmpty(ipPool, portPool) &&
                    ipPool.size() == portPool.size()){
                initProxyIPPool();
            }
        } catch (IOException e) {
            log.info("init ipProxyPool fail ~~");
        }

        //获取请求参数
        String format = String.format(CrawlPostConstant.BASE_REQUEST_PARAM, 1);

        //发出请求，等的JSON数据的map
        Map map = null;
        if(!ObjectUtil.isAllEmpty(ipPool, portPool) &&
                ipPool.size() == portPool.size()){
            //当代理池初始化成功后进行带有动态代理的请求
            map = getJSONDataMapWithProxyIP(format);
        }else {
            //当代理池初始化失败后，使用不带动态代理的请求
            map = postLabelService.getJSONDataMap(format);
        }

        //解析到post数据
        JSONObject data = (JSONObject) map.get("data");
        JSONArray records = (JSONArray) data.get("records");

        records.forEach(item -> {
            JSONObject jsonObject = (JSONObject) item;
            //先获取到postId
            String id = jsonObject.getStr("id");
            long postId = Long.parseLong(id);

            //当前请求道德的postId比游标大，需要入库
            if(postId > postCursor){
                //获取到post和postLabel
                Post post = postLabelService.getPost(jsonObject, postLabelList);
                //存入list
                postList.add(post);
            }
        });


        //入库和清空暂存list
        if(postList.size() > 0){
            log.info("append post data start -->");
            postService.saveBatch(postList);
            postLabelService.saveBatch(postLabelList);
            postList.clear();
            postLabelList.clear();
            log.info("append post data end -->");
        }

    }

    /**
     * 获取到动态代理ip成功后，使用动态代理ip去请求数据, 使用待用动态代理ip的请求去获取数据时
     *      若获取失败，会进行重试（重新从池中随机获取一个ip请求数据），重试 RETRY_TIMES 次
     *      若 RETRY_TIMES 次还失败则使用不带动态代理的请求去获取数据
     * @param format 请求参数
     * @return 响应数据
     */
    private Map getJSONDataMapWithProxyIP(String format) {

        for(int i = 0; i < RETRY_TIMES; i++){

            //从代理ip池中随机获取一个ip
            int random = (int) (Math.random() * (ipPool.size() - (ipPool.size() - 1) + 0));
            String result = null;

            try {
                //向编程导航发送请求，获取文章数据
                result = HttpRequest.post(CrawlPostConstant.URL)
                        .body(format)//表单内容
                        .header(Header.USER_AGENT, CrawlPostConstant.USER_AGENT)
                        .header(Header.REFERER, CrawlPostConstant.REFERER)
                        .setHttpProxy(ipPool.get(random), portPool.get(random)) //设置动态代理ip
                        .timeout(20)//超时，毫秒
                        .execute().body();
            }catch (Exception e){
                //出现异常，重试
                continue;
            }

            //转成map
            Map map = JSONUtil.toBean(result, Map.class);

            //获取成功, 则返回
            if(!ObjectUtil.isEmpty(map.get(CrawlPostConstant.CODE)) &&
                    CrawlPostConstant.SUCCESS_CODE.equals(map.get(CrawlPostConstant.CODE))){
                return map;
            }

        }

        //RETRY_TIMES后使用不带代理的请求
        return postLabelService.getJSONDataMap(format);
    }

    /**
     * 获取用于爬取数据的ip代理池,使用代理ip去请求数据
     */
    private void initProxyIPPool() throws IOException {

        log.info("init proxyIpPool start ===");
        //解析快代理的网页, 获取到一些高匿ip与其端口
        for(int i = 1; i <= 5; i++){

            //拼接请求url
            String url = String.format(CrawlPostConstant.KUAIDAILI_BASE_URL, i);

            //使用jsoup发出请求，从而开始解析网页数据
            Document doc = Jsoup.connect(url).get();
            String tagSyntax="td";
            Elements elements = doc.select(tagSyntax);
            for (Element element : elements)
            {
                //匹配ip地址
                Pattern ipPattern = Pattern.compile("^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])"
                        +"(\\.(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)){3}$");
                Matcher ipMatcher = ipPattern.matcher(element.text());
                if(ipMatcher.find()){
                    //加入ip池
                    ipPool.add(ipMatcher.group());
                }

                //匹配端口
                Pattern portPattern = Pattern.compile("^([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$");
                Matcher portMatcher = portPattern.matcher(element.text());
                if(portMatcher.find()){
                    //加入port池
                    portPool.add(Integer.parseInt(portMatcher.group()));
                }
            }
        }

        //验证这些ip可用,将不可用的ip去除,让他们取请求同一个地方：
        for (int i = 0; i < ipPool.size(); i ++) {
            String body = null;
            try {
                body = HttpRequest.get(CrawlPostConstant.VALID_URL)
                        .header(Header.USER_AGENT, CrawlPostConstant.USER_AGENT)
                        .header(Header.REFERER, CrawlPostConstant.REFERER)
                        .setHttpProxy(ipPool.get(i), portPool.get(i))
                        .timeout(2)//超时，毫秒
                        .execute().body();
            }catch (Exception e){
                log.info(ipPool.get(i) + " can't use");
                ipPool.remove(i);
                portPool.remove(i);
            }
        }

        log.info("init proxyIpPool success ===");

    }

    /**
     * 用于更新游标的方法
     */
    private void updatePostCus() {
        Long latestPostId = postService.getLatestPostId();
        if(ObjectUtil.isEmpty(latestPostId)){
            log.info("get latest postId fail");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "无法获取最新的postId");
        }
        //赋值
        postCursor = latestPostId;
    }

}
