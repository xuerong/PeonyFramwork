package com.peony.demo;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Administrator on 2015/11/24.
 */
public class TestEhcache {
    private static final Logger log = LoggerFactory.getLogger(TestEhcache.class);
    public static void main(String[] args){
//        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
//                .withCache("preConfigured",
//                        CacheConfigurationBuilder.newCacheConfigurationBuilder().buildConfig(Long.class, String.class))
//                .build(true);
//
//        Cache<Long, String> preConfigured
//                = cacheManager.getCache("preConfigured", Long.class, String.class);
//
//        Cache<Long, TestData> myCache = cacheManager.createCache("myCache",
//                CacheConfigurationBuilder.newCacheConfigurationBuilder().buildConfig(Long.class, TestData.class));
//        Cache<Long, TestData2> myCache2 = cacheManager.createCache("myCache2",
//                CacheConfigurationBuilder.newCacheConfigurationBuilder().buildConfig(Long.class, TestData2.class));
//
//        TestData data=new TestData();
//        myCache.put(1L, data);
//        myCache.put(1L, data);
//        myCache.put(1L,new TestData());
//        myCache.put(1L, data);
//        myCache2.put(2l,data.testData2);
//        data.a=30;
//        data.testData2.a=50;
//        TestData value = myCache.get(1L);
//
//        log.info(value.toString());
//
//        cacheManager.close();
    }
    public static class TestData{
        public int a=10;
        public int b=20;
        TestData2 testData2=new TestData2();
        public String toString(){
            return "a:"+a+",b:"+b+",testData2:"+testData2;
        }
    }
    public static class TestData2{
        public int a=10;
        public int b=20;
        public String toString(){
            return "a:"+a+",b:"+b;
        }
    }
}
