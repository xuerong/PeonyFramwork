package com.peony.core.data.cache;

import com.peony.core.data.persistence.orm.EntityHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by a on 2016/8/12.
 *
 * 对需要缓存的数据进行一下封装，然后缓存CacheEntity
 * 这里面可以记录一些特殊的点
 */
public class CacheEntity implements Serializable{
    private static final Logger logger = LoggerFactory.getLogger(CacheEntity.class);

    private static Map<Class<?>,Field[]> clsFieldMap = new ConcurrentHashMap<>();

    private Object entity;
    private CacheEntityState state;
    private long casUnique;

    public CacheEntity(){
        this(null);
    }
    public CacheEntity(Object entity){
        this.entity = entity;
        this.state = CacheEntityState.Normal;
    }

    public Object getEntity() {
        return entity;
    }

    public void setEntity(Object entity) {
        this.entity = entity;
    }

    public CacheEntityState getState() {
        return state;
    }

    public void setState(CacheEntityState state) {
        this.state = state;
    }

    public long getCasUnique() {
        return casUnique;
    }

    public void setCasUnique(long casUnique) {
        this.casUnique = casUnique;
    }

    @Override
    public CacheEntity clone(){
//        long t1 = System.nanoTime();
//        long t2 = System.nanoTime();
//        long t3 = System.nanoTime();
        CacheEntity cacheEntity = new CacheEntity();
        cacheEntity.setState(state);
        cacheEntity.setCasUnique(casUnique);
        if(entity == null){
            cacheEntity.setEntity(null);
        }else if(entity instanceof LinkedHashSet){
            LinkedHashSet list = (LinkedHashSet)entity;
            LinkedHashSet target = new LinkedHashSet();
            for(Object object : list){
                target.add(object);
            }
            cacheEntity.setEntity(target);
        }else {

            // TODO 这个地方有两种优化的方向：完全可行的是，不用反射创建，而是用生成代码创建，另一种是缓存，对象池

//            Class<?> cls = entity.getClass();
//            Object target = ObjectUtil.newInstance(cls);
//            Field[] fields = clsFieldMap.get(cls);
//            if(fields == null){
//                Field[] fs = cls.getDeclaredFields();
//                List<Field> fieldsList = new ArrayList<>();
//                for(Field field : fs){
//                    if (!Modifier.isStatic(field.getModifiers())) {
//                        fieldsList.add(field);
//                    }
//                }
//                fields =new Field[fieldsList.size()];
//                int i=0;
//                for(Field field:fieldsList){
//                    field.setAccessible(true);
//                    fields[i++] = field;
//                }
//                clsFieldMap.putIfAbsent(cls,fields);
//            }
//
//            try {
//                for (Field field : fields) {
//                    //
//                    field.set(target, field.get(entity));
//                }
//            } catch (Exception e) {
//                logger.error("复制成员变量出错！", e);
//                throw new RuntimeException(e);
//            }

//            t2 = System.nanoTime();
//            ObjectUtil.copyFields(entity,target);
//            t3 = System.nanoTime();
            cacheEntity.setEntity(EntityHelper.copyEntity(entity));
        }
        /**
         * __________--------------------40347,346590
         __________--------------------11837,183629
         __________--------------------15419,54732

         __________--------------------15127,19561
         __________--------------------15705,12089
         __________--------------------11657,9042
         __________--------------------132,100
         __________--------------------9809,10629
         __________--------------------10215,9962
         __________--------------------17780,35366
         __________--------------------101,80
         __________--------------------11578,12832
         __________--------------------110,82
         */
//        log.info("__________--------------------"+(t2-t1)+","+(t3-t2)+"");


        return cacheEntity;
    }

    //
    public static enum CacheEntityState{
        Normal,
        Delete, // 这个说明该数据已经被删除
        HasNot//说明数据库中也没有，这样就不要穿透到数据库判断一个没有的数据，可以考虑用Delete？
    }
}
