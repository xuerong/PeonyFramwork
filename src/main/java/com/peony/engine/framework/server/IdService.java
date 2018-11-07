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
import java.util.Random;
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
@Service(init = "init",initPriority = 3,destroy = "destroy",runOnEveryServer = false)
public class IdService {
    private static final Logger log = LoggerFactory.getLogger(IdService.class);

    private DataService dataService;
    private ConcurrentHashMap<Class,IdSegmentLong> longIdSegmentMap;
    private static int pow = 13; // long.max=9223372036854775807,前6位用于serverId，后13位用于index
    private long preByServerId = 0; // 最大支持 long.max/(10^pow)

    private boolean use9hex = false;

    private Random random = new Random();


    public void init() {
//        intIdSegmentMap = new ConcurrentHashMap<>();
        longIdSegmentMap = new ConcurrentHashMap<>();
        // serverId转9进制
        if(use9hex){
            preByServerId = Long.parseLong(Integer.toString(Server.getServerId(),9))*(long)Math.pow(10,pow);
        }else{
            //
            preByServerId = Server.getServerId()*(long)Math.pow(10,pow);
        }

        //TODO 从数据库中载入当前各个id状态
        List<IdGenerator> idGenerators = dataService.selectList(IdGenerator.class,null);
        if(idGenerators != null){
            for(IdGenerator idGenerator : idGenerators){
                try {
                    Class cls = Class.forName(idGenerator.getClassName());
                    IdSegmentLong idSegment = new IdSegmentLong(cls,idGenerator.getId());
                    longIdSegmentMap.put(cls, idSegment);
                }catch (Throwable e){
                    log.error("",e);
                }
            }
        }

    }

    public long getPreByServerId() {
        return preByServerId;
    }

    /**
     * 根据id获取对应的服id，
     * @param id
     * @return
     */
    public int getServerIdById(long id){
        int ret = (int)(id/(long)Math.pow(10,pow));
        if(use9hex){
            ret = Integer.parseInt(String.valueOf(ret),9);
        }
        return ret;
    }

    /**
     * 获取一个ID
     * @param cls 该ID所属类型
     * @return id
     */
    public long acquireLong(Class<?> cls){
        try {
            IdSegmentLong idSegmentLong = getIdSegmentLong(cls);
            long result = idSegmentLong.acquire();
            IdGenerator idGenerator = new IdGenerator();
            idGenerator.setClassName(cls.getName());
            idGenerator.setId(idSegmentLong.getIdMark().get());
            dataService.update(idGenerator);
            return result+preByServerId;
        }catch (Throwable e){
            throw new MMException("acquireLong error",e);
        }
    }

    public long getCurrentValue(Class<?> cls){
        try {
            IdSegmentLong idSegmentLong = getIdSegmentLong(cls);
            return idSegmentLong.get();
        }catch (Throwable e){
            throw new MMException("acquireLong error",e);
        }
    }

    public long randomHaveValue(Class<?> cls){
        long max = getCurrentValue(cls);
        long min = preByServerId+0;
        return (long)(Math.random()*(max-min))+min;
    }

    private IdSegmentLong getIdSegmentLong(Class<?> cls){
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
        return idSegmentLong;
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

        public long get(){
            return idMark.get();
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
}
