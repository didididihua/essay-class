package com.yupi.springbootinit.postclass;

import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.yupi.springbootinit.constant.CrawlPostConstant;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: tangchongjie
 * @creattime: 2023--05--07 21:40
 * @description
 */
@SpringBootTest
public class IpPoolTest {

    public static final String KUAIDAILI_BASE_URL = "https://www.kuaidaili.com/free/inha/%s";

    public static final String VALID_URL = "http://httpbin.org/ip";

    public ArrayList<String> ipPool = new ArrayList<>();

    public ArrayList<Integer> port = new ArrayList<>();

    @Test
    public void testGetIP() throws IOException {

        //获取到一些高匿ip
        for(int i = 1; i <= 5; i++){

            String url = String.format(KUAIDAILI_BASE_URL, 1);

            Document doc = Jsoup.connect(url).get();
            String tagSyntax="td";
            Elements elements = doc.select(tagSyntax);
            for (Element element : elements)
            {
                Pattern ipPattern = Pattern.compile("^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])"
                        +"(\\.(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)){3}$");
                Matcher ipMatcher = ipPattern.matcher(element.text());
                if(ipMatcher.find()){
                    ipPool.add(ipMatcher.group());
                }

            Pattern portPattern = Pattern.compile("^([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$");
            Matcher portMatcher = portPattern.matcher(element.text());
            if(portMatcher.find()){
                port.add(Integer.parseInt(portMatcher.group()));
                }
            }
        }

        //验证这些ip可用：
        for (int i = 0; i < ipPool.size(); i ++) {
            System.out.println(ipPool.get(i));

            String body = null;
            try {
                body = HttpRequest.get(VALID_URL)
                        .header(Header.USER_AGENT, CrawlPostConstant.USER_AGENT)
                        .header(Header.REFERER, CrawlPostConstant.REFERER)
                        .setHttpProxy(ipPool.get(i), port.get(i))
                        .timeout(2)//超时，毫秒
                        .execute().body();
            }catch (Exception e){
                System.out.println(ipPool.get(i) + "不可用");
                ipPool.remove(i);
                port.remove(i);
            }
        }

    }

}
