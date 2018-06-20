package test.netTest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by a on 2016/11/14.
 */
public class otherTest {
    private static final Logger log = LoggerFactory.getLogger(otherTest.class);
    static int a;
    public static void main(String[] args){
//        Integer a = 1;
//        Integer b = 2;
//        Integer c = 3;
//        Integer d = 3;
//        Integer e = 321;
//        Integer f = 321;
//
//        Long g = 3l;


        final CountDownLatch latch = new CountDownLatch(10);
        for (int i=0;i<10;i++) {

            new Thread(){
                public void run(){
                    try{
                        Thread.sleep((int)(Math.random()*1000));
                        latch.countDown();
                    }catch (Throwable e){
                        e.printStackTrace();
                    }
                }
            }.start();
        }
        for(int i=0;i<3;i++) {
            final int index = i;
            new Thread(){
                public void run(){
                    try{
                        log.info("en"+index);
                        latch.await();
                        log.info("de"+index);
                    }catch (Throwable e){
                        e.printStackTrace();
                    }
                }
            }.start();
        }
        try{
            log.info("en"+11);
            latch.await();
            log.info("de"+11);
        }catch (Throwable e){
            e.printStackTrace();
        }


    }

//    public static String myMethod(List<String> first){
//
//        return "";
//    }
    public static int myMethod(List<Integer> first){
        List<Integer> a= Arrays.asList(1,2,3,4);
        return 0;
    }

    public void test(){
        final Lock lock = new ReentrantLock();
        final Object locker = new Object();
        Thread t1=new Thread(){
            @Override
            public void run(){
                try {
                    synchronized (locker){
                        locker.wait();
                        log.info("do it");
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        Thread t2=new Thread(){
            @Override
            public void run(){
                try {
                    Thread.sleep(100);
                    synchronized (locker){
                        locker.notify();
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        };
        t1.start();
        t2.start();
    }
}
