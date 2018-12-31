package com.peony.engine.framework.tool.util;

import com.peony.engine.framework.data.entity.account.LoginInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.UnpooledDirectByteBuf;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
//import org.apache.commons.pool2.impl.GenericObjectPool;
//
//import com.ai.toptea.common.log.TopteaLogger;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

public class SerializeUtil {
    private static final Logger logger = LoggerFactory.getLogger(SerializeUtil.class);

    public static final int BUFFER_SIZE = 2048;
    public static final int MAX_BUFFER_SIZE = 10485760;

    private  static ObjectPool<Kryo> kryo_pool;


    public static void main(String[] args) throws Exception{
//        Kryo kryo = kryo_pool.borrowObject();
//        kryo.
        long t1 = System.currentTimeMillis();

        for(int i =0;i<200000;i++){
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            TestData testData = new TestData();
            try {
                // FIXME  wjm 不安全,容易错!!!!
                try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
                    out.writeObject(testData);
                    out.flush();
                }
                byte[] aaa = bos.toByteArray();
//                System.out.println(bos.toByteArray().length);
            } finally {
                if (bos != null) {
                    bos.close();
                }
            }
        }

        long t2 = System.currentTimeMillis();

        TestData testData1 = new TestData();
        byte[] aaa1 = serialize(testData1);
        System.out.println(aaa1.length);

        for(int i =0;i<200000;i++){
            TestData testData = new TestData();
            byte[] aaa = serialize(testData);

        }
        long t3 = System.currentTimeMillis();
        System.out.println((t2-t1)+"\t\t"+(t3-t2));

    }

    static class TestData implements Serializable{
        int aaa = 10;
        int bbb = 300000;
        String ccc = "ggg";
        InnerData innerData = new InnerData();

    }
    static class InnerData implements Serializable{
        int aaa = 10;
        int ccc = 3033;
        String ttt = "dddd";
    }
    static{
        GenericObjectPoolConfig config = new GenericObjectPoolConfig<Kryo>();
        config.setMaxTotal(200);
        kryo_pool = new GenericObjectPool<Kryo>(new KryoPool(),config);
//        System.out.println("这个啥时执行");
    }
    static class KryoPool extends BasePooledObjectFactory{
        AtomicInteger i=new AtomicInteger();
        @Override
        public Object create() throws Exception {
            return new Kryo();
        }

        @Override
        public PooledObject wrap(Object obj) {
            return new DefaultPooledObject<Kryo>((Kryo)obj);
        }
    }

    public static byte[] serialize(Object t) throws Exception{
        Kryo kryo = null;
        Output output = null;
        try{
            kryo = kryo_pool.borrowObject();
            output = new Output(BUFFER_SIZE, MAX_BUFFER_SIZE);
//            kryo.writeClassAndObject(output, t);
            kryo.writeObject(output, t);
            return output.toBytes();

        }catch(Exception e){
            logger.error("异常", e);
            throw e;
        }finally{
            if(output != null){
                try {
                    output.close();
                    output = null;
                } catch (Exception e) {
                    logger.error("异常", e);
                }
            }
            if(kryo != null){
                try {
                    kryo_pool.returnObject(kryo);
                } catch (Exception e) {
                    logger.error("异常", e);
                }
            }
        }
    }

    public static Object deserialize(byte[] bytes,Class cls) throws Exception{
        Kryo kryo = null;
        Input input = null;
        try{
            kryo = kryo_pool.borrowObject();
            input = new Input(bytes);
//            Object t = kryo.readClassAndObject(input);
            Object t = kryo.readObject(input,cls);
            return t;
        }catch(Exception e){
            logger.error("异常", e);
            throw e;
        }finally{
            if(input != null){
                try {
                    input.close();
                    input = null;
                } catch (Exception e) {
                    logger.error("异常", e);
                }
            }
            if(kryo != null){
                try {
                    kryo_pool.returnObject(kryo);
                } catch (Exception e) {
                    logger.error("异常", e);
                }
            }
        }
    }
}