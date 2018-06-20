package com.peony.engine.framework.server;

import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.control.netEvent.remote.RemoteCallService;
import com.peony.engine.framework.data.DataService;
import com.peony.engine.framework.data.tx.LockerService;
import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.tool.helper.BeanHelper;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by a on 2016/9/14.
 * id服务，为类的对象生成唯一
 * TODO id服务的持久化
 * id服务提供两种就可:int和long
 * 
 * ,runOnEveryServer = false
 */
@Service(init = "init", destroy = "destroy",runOnEveryServer = false)
public class IdService {
    private static final Logger log = LoggerFactory.getLogger(IdService.class);

    private RemoteCallService remoteCallService;
    private DataService dataService;
    private LockerService lockerService;

//    private ConcurrentHashMap<Class,IdSegment> intIdSegmentMap;
    private ConcurrentHashMap<Class,IdSegmentLong> longIdSegmentMap;

    public void init() {
        remoteCallService = BeanHelper.getServiceBean(RemoteCallService.class);
//        intIdSegmentMap = new ConcurrentHashMap<>();
        longIdSegmentMap = new ConcurrentHashMap<>();
        //TODO 从数据库中载入当前各个id状态
        List<IdGenerator> idGenerators = dataService.selectList(IdGenerator.class,null);
        if(idGenerators != null){
            for(IdGenerator idGenerator : idGenerators){
                try {
                    Class cls = Class.forName(idGenerator.getClassName());
                    IdSegmentLong idSegment = new IdSegmentLong(cls,idGenerator.getId());
                    longIdSegmentMap.put(cls, idSegment);
                }catch (Throwable e){
                    e.printStackTrace();
                }
            }
        }
    }

    public long acquireLong(Class<?> cls){
        if(lockerService.lockKeys(cls.getName())) {
            try {
                IdSegmentLong idSegmentLong = longIdSegmentMap.get(cls);
                if (idSegmentLong == null) {
                    idSegmentLong = new IdSegmentLong(cls);
                    IdSegmentLong old = longIdSegmentMap.putIfAbsent(cls, idSegmentLong);
                    if (old != null) {
                        idSegmentLong = old;
                    } else {
                        IdGenerator idGenerator = new IdGenerator();
                        idGenerator.setClassName(cls.getName());
                        idGenerator.setId(idSegmentLong.getIdMark().get());
                        dataService.insert(idGenerator);
                    }
                }
                long result = idSegmentLong.acquire();
                IdGenerator idGenerator = new IdGenerator();
                idGenerator.setClassName(cls.getName());
                idGenerator.setId(idSegmentLong.getIdMark().get());
                dataService.update(idGenerator);
                return result;
            }catch (Throwable e){
                throw new MMException("acquireLong error",e);
            }finally {
                lockerService.unlockKeys(cls.getName());
            }
        }else{
            throw new MMException("acquireLong error,lock fail");
        }
    }
    public int acquireInt(Class<?> cls){
//        IdSegment IdSegment = intIdSegmentMap.get(cls);
//        if(IdSegment == null){
//            IdSegment = new IdSegment(cls);
//            intIdSegmentMap.putIfAbsent(cls, IdSegment);
//            IdSegment = intIdSegmentMap.get(cls);
//        }
//        return IdSegment.acquire();
        return (int)acquireLong(cls);
    }

    public void releaseInt(Class<?> cls, int id){
//        IdSegment IdSegment = intIdSegmentMap.get(cls);
//        if(IdSegment == null){
//            throw new MMException("IdSegment is not exist,cls = "+cls.getName());
//        }
//        IdSegment.release(id);
    }

    public void destroy(){
        for(IdSegmentLong idSegmentLong : longIdSegmentMap.values()){
            IdGenerator idGenerator = new IdGenerator();
            idGenerator.setClassName(idSegmentLong.getCls().getName());
            idGenerator.setId(idSegmentLong.getIdMark().get());
            dataService.update(idGenerator);
        }
    }

    class IdSegmentLong{
        private Class cls;
        private AtomicLong idMark;
        public IdSegmentLong(Class cls){
            this.cls = cls;
            this.idMark = new AtomicLong(100000);
        }
        public IdSegmentLong(Class cls,long id){
            this.cls = cls;
            this.idMark = new AtomicLong(id);
        }
        public long acquire(){
            long id = idMark.getAndIncrement();
            return id;
        }

        public Class getCls() {
            return cls;
        }

        public void setCls(Class cls) {
            this.cls = cls;
        }

        public AtomicLong getIdMark() {
            return idMark;
        }

        public void setIdMark(AtomicLong idMark) {
            this.idMark = idMark;
        }
    }

    class IdSegment{
        private Class cls;
        private Set<Integer> usingIds;
        private Queue<Integer> canUseIds;
        private AtomicInteger idMark;

        public IdSegment(Class cls){
            this.cls = cls;
            this.usingIds = new ConcurrentHashSet<>();
            this.canUseIds = new ConcurrentLinkedDeque<>();
            this.idMark = new AtomicInteger();
        }

        public int acquire(){
            Integer id = canUseIds.poll();
            if(id == null){
                id = idMark.getAndIncrement();
                usingIds.add(id);
            }
            return id;
        }

        public void release(Integer id){
            usingIds.remove(id);
            canUseIds.offer(id);
        }


        public Class getCls() {
            return cls;
        }

        public void setCls(Class cls) {
            this.cls = cls;
        }

        public Set<Integer> getUsingIds() {
            return usingIds;
        }

        public Queue<Integer> getCanUseIds() {
            return canUseIds;
        }

        public Number getIdMark() {
            return idMark;
        }
    }
}
