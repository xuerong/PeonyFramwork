package com.peony.engine.framework.net;

import com.alibaba.fastjson.JSONObject;
import com.peony.engine.framework.control.annotation.Service;
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

    /**
     * get访问
     * @param url 地址
     * @return 访问返回信息
     */
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
        HttpPost post = new HttpPost(url);
        String str = "";
        try {

            StringEntity strEn = new StringEntity(json.toString(), ContentType.APPLICATION_JSON);
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

    /**
     * post访问
     * @param url 地址
     * @param body 访问数据
     * @param contentType 访问的格式
     * @return 访问返回值
     */
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
