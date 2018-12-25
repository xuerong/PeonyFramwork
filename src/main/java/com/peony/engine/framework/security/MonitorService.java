package com.peony.engine.framework.security;

import com.peony.engine.framework.control.ServiceHelper;
import com.peony.engine.framework.control.annotation.EventListener;
import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.control.annotation.Updatable;
import com.peony.engine.framework.control.event.EventData;
import com.peony.engine.framework.control.event.EventService;
import com.peony.engine.framework.data.DataService;
import com.peony.engine.framework.data.cache.CacheService;
import com.peony.engine.framework.data.entity.account.Account;
import com.peony.engine.framework.data.entity.account.LogoutEventData;
import com.peony.engine.framework.data.entity.session.Session;
import com.peony.engine.framework.data.tx.LockerService;
import com.peony.engine.framework.server.SysConstantDefine;
import com.peony.engine.framework.tool.helper.BeanHelper;
import com.peony.engine.framework.tool.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Administrator on 2015/12/30.
 * TODO 这个要好好的实现一下
 * 系统检测服务，用于对系统的所有服务运行状况进行检测
 *
 * 这里要提供如下服务：
 * 1 启动时，判断哪些启动时必须满足的条件，即启动完成的判断，如NetEventService端口尚未启动
 * 2 当某些极端事件发生，如mainServer失联，则停止服务器的正常运行状态
 * 3 关闭服务器的时候，判断所有正常关闭的条件
 * 4 平时监控并提示服务器的状态，如负载，瓶颈，网络状态等
 */
@Service(init = "init",initPriority = 1)
public class MonitorService {
    private static final Logger log = LoggerFactory.getLogger(MonitorService.class);

    private Map<String,String> conditions = new HashMap<>();
    // 在线人数
    private ConcurrentHashMap<String,String> onlineUser = new ConcurrentHashMap<>();
    //
    Map<Method,Object> monitorMethod = new HashMap<>();

    //监控的数据，其它服务可以将要监控的数据放到这里面，统一打印
    private Map<MonitorNumType,AtomicLong> monitorNumData = new HashMap<>();

    private volatile  boolean running ;

    private EventService eventService;
    private DataService dataService;
    private CacheService cacheService;
    private LockerService lockerService;

    public void init(){
        Map<Class<?>,List<Method>> monitorClassMap= ServiceHelper.getMonitorClassMap();
        for(Map.Entry<Class<?>,List<Method>> entry : monitorClassMap.entrySet()){
            Object service= BeanHelper.getServiceBean(entry.getKey());
            List<Method> methodList=entry.getValue();
            for(Method method : methodList){
                method.setAccessible(true);//TODO 取消 Java 语言访问检查   看看其他地方能否用到
                monitorMethod.put(method,service);
            }
        }

        for(MonitorNumType monitorNumType : MonitorNumType.values()){
            monitorNumData.put(monitorNumType,new AtomicLong(0));
        }
    }

    @EventListener(event = SysConstantDefine.Event_AccountLoginAsync)
    public void login(EventData data){
        Session session = (Session) ((List)data.getData()).get(0);
        if(session.getUid() != null) {
            onlineUser.putIfAbsent(session.getUid(),session.getUid());
        }else{
            log.error("event Event_AccountLoginAsync,session has no accountId");
        }

    }
    @EventListener(event = SysConstantDefine.Event_AccountLogoutAsync)
    public void logout(EventData data){
        LogoutEventData logoutEventData = (LogoutEventData)data.getData();
        if(logoutEventData.getSession().getUid() != null) {
            onlineUser.remove(logoutEventData.getSession().getUid());
        }
    }


    @Updatable(isAsynchronous = true,cycle = 6000)
    public void monitorUpdate(int interval){
//        log.info("monitorUpdate:"+interval);
        String state = eventService.getMonitorData();
        if(!state.equals("ok")){
            log.error(state);
        }else{
//            log.info("server is ok!");
        }
        if(conditions.size()>0){
            for(Map.Entry<String,String> entry : conditions.entrySet()){
                log.warn(entry.getKey()+":"+entry.getValue());
            }
        }
    }
    // 5分钟输出一下
    @Updatable(isAsynchronous = true,cycle = 300000)
    public void monitorLog(int interval){
        // 缓存数据
        setMonitorNum(MonitorNumType.CacheNum,cacheService.size());
        setMonitorNum(MonitorNumType.CacheEvictedNum,cacheService.evictNum());
        setMonitorNum(MonitorNumType.OnlineUserNum,onlineUser.size());
        setMonitorNum(MonitorNumType.LockerNum,lockerService.getLockingNum());

        String date = DateUtils.formatNow("yyyy-MM-dd HH:mm:ss");
        // 打印监控数据
        StringBuilder sb = new StringBuilder("\n\n-----------------------------------server monitor begin,"+date+"-----------------------------------").append("\n");

        for(Map.Entry<Method,Object> entry : monitorMethod.entrySet()){
            try{
                Monitor monitor = entry.getKey().getAnnotation(Monitor.class);
                String result =  entry.getKey().invoke(entry.getValue(),null).toString();
                sb.append("\n---------------------------").append(monitor.name()).append(":\n").append(result);
            }catch (Throwable e){
                log.error("monitor server error!",e);
            }
        }

        sb.append("\n---------------------------参数们：\n");
        for(MonitorNumType monitorNumType : MonitorNumType.values()){
            sb.append(date).append("   ").append(monitorNumType.getKey()).append(":").append(monitorNumData.get(monitorNumType)).append("\n");
        }
        sb.append("\n---------------------------参数们：end\n");

        sb.append("\n\n").append("-----------------------------------server monitor end----------------------------------\n\n");
        // 显示
        log.info(sb.toString());
        //
        log.info("server is ok!");
        // 下下策，每隔
    }

    public void decrMonitorNum(MonitorNumType key,int num){
        AtomicLong atomicLong = monitorNumData.get(key);
        atomicLong.getAndAdd(-num);
    }

    public void addMonitorNum(MonitorNumType key,int num){
        AtomicLong atomicLong = monitorNumData.get(key);
        atomicLong.getAndAdd(num);
    }
    public void setMonitorNum(MonitorNumType key,int num){
        AtomicLong atomicLong = monitorNumData.get(key);
        atomicLong.set(num);
    }


    // 定时访问一下数据库
    @Updatable(isAsynchronous = true,cycle = 300000)
    public void monitorDataBase(int interval){
        dataService.selectListBySql(Account.class,"select * from account limit 1");
    }

    public synchronized void addStartCondition(String key,String describe){
        conditions.put(key,describe);
    }
    public synchronized void removeStartCondition(String key){
        conditions.remove(key);
        notify();
    }

    /**
     * 等待直到服务器启动完成
     */
    public synchronized void startWait(){
        while (conditions.size() > 0){
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        running = true;
        //
//        System.err.println("服务器启动完成");
    }



    public void stopWait(){

    }
}
