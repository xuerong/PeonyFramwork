package test.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Created by a on 2016/8/24.
 * 测试一下invoke效率
 */
public class TestInvoke {
    private static final Logger log = LoggerFactory.getLogger(TestInvoke.class);
    public static void main(String[] args) throws Throwable{
        Method[] methods = Aaa.class.getMethods();
        Method testMethod = null;
        for(Method method : methods){
            if(method.getName().equals("get1")){
                testMethod = method;

                break;
            }
        }
        // ceshi
        Aaa aaa = new Aaa();
        int count = 10000000;
        long t1 = System.currentTimeMillis();
        for(int i=0;i<count;i++){
            aaa.get1();
        }
        long t2 = System.currentTimeMillis();
        for(int i=0;i<count;i++){
            testMethod.invoke(aaa);
        }

        long t3 = System.currentTimeMillis();
        log.info(aaa.get1()+","+(t2-t1)+","+(t3-t2));
    }
}
class Aaa{
    int count = 0;
    public String get1(){
        return count+++"";
    }
    public String get2(){
        return "get2";
    }
}
