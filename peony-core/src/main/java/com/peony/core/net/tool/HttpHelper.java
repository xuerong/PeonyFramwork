package com.peony.core.net.tool;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by a on 2016/9/13.
 */
public class HttpHelper {
    public static byte[] decode(HttpServletRequest request) throws IOException {
        String ignoredata = request.getHeader("IGNORE_DATA");// 当是空包的时候，如，logout,unity发不出来，故设此参数
        byte[] buffer = null;
        if(ignoredata==null || ignoredata.length()<=0){
            InputStream is = request.getInputStream();
            int bufSize = request.getContentLength();
            if(bufSize < 0){
                buffer = new byte[0];
//                log.error("request getContentLength={}",bufSize);
//                return null;
            }else{
                buffer = new byte[bufSize];
                int size = is.read(buffer);
                int readedSize = size;
                if (size != bufSize) {
                    while (size > -1) {
                        size = is.read(buffer, readedSize, bufSize - readedSize);
                        readedSize += size;
                    }
                }
            }
        }else {
            buffer = new byte[0];
        }
        return buffer;
    }
}
