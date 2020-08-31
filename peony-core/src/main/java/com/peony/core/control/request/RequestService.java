package com.peony.core.control.request;

import com.peony.core.control.ServiceHelper;
import com.peony.core.control.annotation.Request;
import com.peony.core.control.annotation.Service;
import com.peony.core.control.gm.Gm;
import com.peony.core.data.entity.session.Session;
import com.peony.core.security.Monitor;
import com.peony.core.security.MonitorNumType;
import com.peony.core.security.MonitorService;
import com.peony.common.exception.MMException;
import com.peony.core.control.BeanHelper;
import com.peony.common.tool.helper.ClassHelper;
import com.peony.common.tool.utils.DateUtils;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntObjectProcedure;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Administrator on 2015/11/17.
 */
@Service(init = "init")
public class RequestService {
    private static final Logger log = LoggerFactory.getLogger(RequestService.class);

    private MonitorService monitorService;

    private Set<Integer> donNotPrintOpcode = new HashSet<Integer>(){
        {
//            add(MiGongOpcode.CSCommon)
        }
    };

    private Map<Integer,String> opcodeNames = new HashMap<>();
    private Map<Integer,String> exceptionNames = new HashMap<>();

    private Map<Integer,RequestHandler> handlerMap=new HashMap<Integer,RequestHandler>();
    private volatile Map<Integer,ConcurrentLinkedDeque<Integer>> timeMap=new HashMap<>();
    private AtomicInteger requestNum = new AtomicInteger(0);// 当访问数量大于一定值时，才重置用时统计数据

    private int resetCount = 0;

    public void init(){
        TIntObjectHashMap<Class<?>> requestHandlerClassMap = ServiceHelper.getRequestHandlerMap();
        requestHandlerClassMap.forEachEntry(new TIntObjectProcedure<Class<?>>(){
            @Override
            public boolean execute(int i, Class<?> aClass) {
                handlerMap.put(i, (RequestHandler)BeanHelper.getServiceBean(aClass));
                timeMap.put(i,new ConcurrentLinkedDeque<>());
                return true;
            }
        });
        // 命令
        List<Class<?>> cmdClasses = ClassHelper.getClassListEndWith("com.peony.demo.myFruit.cmd","Cmd");
        for (Class<?> cls:cmdClasses) {

            Field[] fields = cls.getFields();
            for(Field field : fields){
                try {
                    opcodeNames.put(field.getInt(null),field.getName());
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public String getOpName(int id){
        return opcodeNames.get(id);
    }
    public String getExceptionName(int id){
        return exceptionNames.get(id);
    }

    /**
     * 接受网络传过来的客户端数据，转发给对应的处理器处理，并返回处理结果
     * @param opcode 命令号
     * @param clientData 客户端数据
     * @param session 会话
     * @param <T> 返回值，可以是任意类型
     * @return
     * @throws Exception
     */
    public <T> T handleRequest(int opcode, Object clientData, Session session) throws Exception{
//        log.info("request:"+opcode+","+clientData);
        RequestHandler handler = handlerMap.get(opcode);
        if(handler == null){
            throw new MMException("can't find handler of "+opcode);
        }
        //
        monitorService.addMonitorNum(MonitorNumType.RequestNum,1);
        // 如果属于加锁失败（事务中）导致的，在这里重新执行，这里只是确保用户访问的事务能够被重新执行
        T ret;
        int count = 0;
        long t1 = System.currentTimeMillis();
        while (true) {
            try {
                ret = handler.handle(opcode, clientData, session);

            } catch (MMException e) {
                if (e.getExceptionType() == MMException.ExceptionType.TxCommitFail) {
                    if(count++<2) {
                        log.warn("----------TxCommitFail ----json---"+opcode);
                        continue;
                    }else {
                        log.error("tx commit fail after 3 times");
                        throw e;
                    }
                }else{
                    throw e;
                }
            }finally {
                long t2 = System.currentTimeMillis();
                ConcurrentLinkedDeque<Integer> timeList = timeMap.get(opcode);
                timeList.add((int)(t2-t1));
                requestNum.getAndIncrement();
            }
            break;
        }
//        log.info("response:"+opcode+","+ret);
        return ret;
    }


    @Monitor(name = "客户端访问平均消耗时间")
    public String monitorRequest(){
        // 分析数据
        String date = DateUtils.formatNow("yyyy-MM-dd HH:mm:ss");
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<Integer,ConcurrentLinkedDeque<Integer>> entry: timeMap.entrySet()){
            int size = entry.getValue().size();
            if(size > 0) {
                int all = 0;
                int min=entry.getValue().peek();
                int max = min;
                Map<Integer,Integer> useTime = new TreeMap<>();
                for (Integer value : entry.getValue()) {
                    all += value;
                    if(value >max){
                        max = value;
                    }else if(value < min){
                        min = value;
                    }
                    Integer num = useTime.get(value);
                    if(num==null){
                        num=0;
                    }
                    num++;
                    int de = 1;
                    while (value/de>10) {
                        de*=10;
                    }
                    useTime.put(value/de*de, num);
                }

                int average = all / size;

                sb.append(date).append("   ").append(opcodeNames.get(entry.getKey())).append("【数量:").append(size).append(",平均用时:").
                        append(average).append(",最大用时：").append(max).append(",最小用时:").append(min).append("】;");
                for (Map.Entry<Integer,Integer> useTimeEntry : useTime.entrySet()){
                    int value = useTimeEntry.getKey();
                    int de = 1;
                    while (value/de>=10) {
                        de*=10;
                    }
                    sb.append("[").append(value).append("-").append(value+de).append(":").append(useTimeEntry.getValue()).append("]");
                }
                sb.append("\n");
            }
        }
        sb.append(date).append("   ").append("requestNum:"+requestNum.get()).append("\n");
        // 清0
        if(requestNum.get() > Integer.MAX_VALUE-1000000) {
            requestNum.set(0);
        }
        // 每2000000重置一次
        if(requestNum.get() > 2000000*(resetCount+1)){
            Map<Integer, ConcurrentLinkedDeque<Integer>> newTimeMap = new HashMap<>();
            TIntObjectHashMap<Class<?>> requestHandlerClassMap = ServiceHelper.getRequestHandlerMap();
            requestHandlerClassMap.forEachEntry(new TIntObjectProcedure<Class<?>>() {
                @Override
                public boolean execute(int i, Class<?> aClass) {
                    newTimeMap.put(i, new ConcurrentLinkedDeque<Integer>());
                    return true;
                }
            });
            this.timeMap = newTimeMap;
            resetCount++;
        }

        return sb.toString();
    }

    @Gm(id="requestservice test")
    public void test()throws Exception{
        JSONObject req = new JSONObject();
        req.put("aaa","bbb");
        handleRequest(6666,req,null);

    }

    @Request(opcode = 6666)
    public JSONObject testsss(JSONObject req,Session session){
        System.out.println("aaa:"+req.get("aaa"));
        return new JSONObject();
    }

    public Map<Integer, String> getOpcodeNames() {
        return opcodeNames;
    }
}
