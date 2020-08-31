package com.peony.demo.config.core.field;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Created by jiangmin.wu on 2018/3/7.
 */
public class Test {

    public static void main(String[] args) throws IOException {


        System.err.println(AbstractFieldType.class.isInterface());


        JSONObject verifys = JSON.parseObject(FileUtils.readFileToString(new File("/Users/wjm/sourceTree/newserver/CoreServer/resource/verify.json"), Charset.forName("utf-8")));


        System.err.println(verifys.toJSONString());

    }

}
