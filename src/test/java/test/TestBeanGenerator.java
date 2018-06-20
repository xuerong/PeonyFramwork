package test;

import test.proxyTest.MyProxy;
import net.sf.cglib.core.ClassGenerator;
import org.objectweb.asm.ClassVisitor;

/**
 * Created by Administrator on 2015/11/18.
 */
public class TestBeanGenerator {
    static {
//        BeanGenerator generator=new BeanGenerator();
//        generator.addProperty();
        Class<?> cls= MyProxy.class;
        ClassGenerator classGenerator=new ClassGenerator() {
            @Override
            public void generateClass(ClassVisitor classVisitor) throws Exception {

            }
        };

    }
}
