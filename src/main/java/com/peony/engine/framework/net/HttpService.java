package com.peony.engine.framework.net;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.SuspendableRunnable;
import com.alibaba.fastjson.JSONObject;
import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.control.gm.Gm;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Author:   sunrui
 * Date:     2018/4/25 17:27
 * Description:
 */
@Service(init = "init", destroy = "destory", initPriority = 1)
public class HttpService {
    private static Logger logger = LoggerFactory.getLogger(HttpService.class);

    private CloseableHttpClient httpclient;

    public void init(){
        httpclient = HttpClients.createDefault();
    }

    public static interface HttpHandler{
        public void handle(String ret);
    }
    class HttpHandlerSuspendableRunnable implements SuspendableRunnable{
        final int method; // 0get,1post
        final String url;
        final HttpHandler httpHandler;
        final String body;
        final ContentType contentType;

        public HttpHandlerSuspendableRunnable(String url,String body, ContentType contentType,HttpHandler httpHandler){
            this(1,url,body,contentType,httpHandler);
        }
        public HttpHandlerSuspendableRunnable(String url,HttpHandler httpHandler){
            this(0,url,null,null,httpHandler);
        }
        public HttpHandlerSuspendableRunnable(int method,String url,String body, ContentType contentType,HttpHandler httpHandler){
            this.method = method;
            this.url = url;
            this.httpHandler = httpHandler;
            this.body = body;
            this.contentType = contentType;
        }

        @Override
        public void run() throws SuspendExecution, InterruptedException {
            if(method==0){
                String ret = doGet(url);
                httpHandler.handle(ret);
            }else if(method == 1){
                String ret = doPost(url,body,contentType);
                httpHandler.handle(ret);
            }
        }
    }



    public void doGetAsync(String url,HttpHandler httpHandler){
        new Fiber<String>(new HttpHandlerSuspendableRunnable(url,httpHandler)).start();
    }

    public void doPostAsync(String url,JSONObject param,HttpHandler httpHandler){
        doPostAsync(url,param.toJSONString(),ContentType.APPLICATION_JSON,httpHandler);
    }

    public void doPostAsync(String url,String body, ContentType contentType,HttpHandler httpHandler){
        new Fiber<String>(new HttpHandlerSuspendableRunnable(url,body,contentType,httpHandler)).start();
    }

    /**
     * get访问
     * @param url 地址
     * @return 访问返回信息
     */
    @Suspendable
    public String doGet(String url){
        HttpGet request = new HttpGet(url);
        String str = "";
        try {
            HttpResponse response = httpclient.execute(request);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instreams = entity.getContent();
                str = convertStreamToString(instreams);
                logger.info("get http response:{}", str);
            }
        }catch(Exception e){
            logger.error("request exception:", e);
        }finally {
            request.abort();
        }
        return str;
    }

    /**
     * post访问
     * @param url 地址
     * @param json 访问数据，json格式
     * @return 访问返回信息
     */
    public String doPost(String url, JSONObject json){
        return doPost(url,json.toString(),ContentType.APPLICATION_JSON);
    }

    /**
     * post访问
     * @param url 地址
     * @param body 访问数据
     * @param contentType 访问的格式
     * @return 访问返回值
     */
    @Suspendable
    public String doPost(String url, final String body, ContentType contentType){
        HttpPost post = new HttpPost(url);
        String str = "";
        try {

            StringEntity strEn = new StringEntity(body, contentType);
            post.setEntity(strEn);

            HttpResponse response = httpclient.execute(post);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                InputStream instreams = entity.getContent();
                str = convertStreamToString(instreams);
                logger.info("get http post response:{}", str);
            }
        }catch (Exception e){
            logger.error("post exception:", e);
        }finally {
            post.abort();
        }
        return str;
    }


    private String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            logger.error("parse stream exception:", e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public void destroy(){
        try{
            httpclient.close();
            logger.info("close http service success!");
        }catch(Exception e){
            logger.error("close http client exception:", e);
        }
    }
}
