package com.yupi.springbootinit.constant;

/**
 * @author: tangchongjie
 * @creattime: 2023--05--07 16:47
 * @description 对编程导航爬取数据是使用的一些常量
 */
public interface CrawlPostConstant {
    /**
     * 爬取数据时用的userAgent
     */
    final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Safari/537.36";

    /**
     * 请求时使用的referer
     */
    String REFERER = "https://www.code-nav.cn/";

    /**
     * 用于获取编程导航所有帖子的url
     */
    String URL = "https://www.code-nav.cn/api/post/list/page/vo";

    /**
     * 爬取高匿ip做动态代理的地方（块代理官网）
     */
    String KUAIDAILI_BASE_URL = "https://www.kuaidaili.com/free/inha/%s";


    /**
     * 用于验证爬取到ip是否可用的地方
     */
    String VALID_URL = "http://httpbin.org/ip";

    /**
     * 基础的请求参数
     */
    static final String BASE_REQUEST_PARAM = "{\"current\":%s,\"reviewStatus\":1,\"sortField\":\"createTime\",\"sortOrder\":\"descend\"}";

    /**
     * 响应的json中状态码的json字段名
     */
    static String CODE = "code";

    /**
     * 向编程导航请求数据后得成功状态码
     */
     static Integer SUCCESS_CODE = 0;
}
