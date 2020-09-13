package com.peony.core.configure;

import com.peony.common.exception.MMException;
import com.peony.common.tool.helper.ConfigHelper;
import com.peony.core.cluster.ServerInfo;
import com.peony.core.data.cache.CacheCenter;
import com.peony.core.data.persistence.dao.DataAccessor;
import com.peony.core.data.persistence.ds.DataSourceFactory;
import com.peony.core.server.ServerType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by Administrator on 2015/11/16.
 */
public final class EngineConfigure {

    private static final String FRAMEBEAN_DATASOURCEFACTORY_DEFAULT = "com.peony.core.data.persistence.ds.impl.HikariDataSourceFactory";
    private static final String FRAMEBEAN_DATAACCESSOR_DEFAULT = "com.peony.core.data.persistence.dao.impl.DefaultDataAccessor";

    private static final String FRAMEBEAN_CACHECENTER_DEFAULT = "com.peony.core.data.cache.LRUMapCacheCenter";


    private static final Logger log = LoggerFactory.getLogger(EngineConfigure.class);
    private Map<Class<?>,Class<?>> configureBeans=new HashMap<Class<?>,Class<?>>();
    private String defaultRequestController;
//    private int syncUpdateCycle = 1000;

    // 系统开启的网络入口
    private final Map<String, EntranceConfigure> entranceClassMap = new HashMap<>();
    public EntranceConfigure netEventEntrance;
    public EntranceConfigure requestEntrance;
    // session update:貌似不能定义系统参数

    //
    public EngineConfigure(){
        this(null);
    }
    public EngineConfigure(String serverTypeStr){
        if(serverTypeStr!=null){
            ServerType.setServerType(serverTypeStr);
        }else{
            ServerType.setServerType(serverTypeStr);
            // 从配置文件中取server类型，如果没有，就是默认类型nodeServer
        }
        // 初始化配置：从配置文件中读取
        configureBeans.put(DataSourceFactory.class,getBeanFromConfigure("frameBean.dataSourceFactory",FRAMEBEAN_DATASOURCEFACTORY_DEFAULT));
        configureBeans.put(DataAccessor.class,getBeanFromConfigure("frameBean.dataAccessor",FRAMEBEAN_DATAACCESSOR_DEFAULT));
        configureBeans.put(CacheCenter.class,getBeanFromConfigure("frameBean.cacheCenter",FRAMEBEAN_CACHECENTER_DEFAULT));

        defaultRequestController="DefaultRequestController";
        // 初始化入口
        initEntrance();
        // session.cycle
//        sessionUpdateCycle = Integer.parseInt(getString("session.cycle"));
        // syncUpdate.cycle
//        syncUpdateCycle = Integer.parseInt(getString("syncUpdate.cycle"));
    }

    public void changeEntrancePort(String portStr){
        if(portStr == null || portStr.length() == 0){
            return;
        }
        String[] pStrs  = portStr.split("\\|");
        for(String pStr : pStrs){
            String[] itemStrs = pStr.split(":");
            EntranceConfigure configure = entranceClassMap.get(itemStrs[0]);
            if(configure == null ){
//                throw new MMException("entrance "+itemStrs[0]+" is not exist");
                log.warn("entrance "+itemStrs[0]+" is not exist");
                continue;
            }
            configure.setPort(Integer.parseInt(itemStrs[1]));
        }
    }

    private void initEntrance(){
        // entrance
        Map<String, String> entranceMap = ConfigHelper.getMap("entrance");
        Set<String> nameSet = new HashSet<>();
        for(Map.Entry<String,String> entry : entranceMap.entrySet()){
            String name = entry.getKey().replace("entrance.","").replace(".port","").replace(".class","");
            if(nameSet.contains(name)){
                continue;
            }
            if(name.equals("mainClient") && !ConfigHelper.getBoolean("mainServer.use")){ // main server 是否使用
                continue;
            }
            nameSet.add(name);
            String portStr = ConfigHelper.getString("entrance."+name+".port");
            String clsStr = ConfigHelper.getString("entrance."+name+".class");

            int port = Integer.parseInt(portStr);
            Class cls;
            try {
                cls = Class.forName(clsStr);
            } catch (ClassNotFoundException e) {
                throw new MMException(e);
            }
            EntranceConfigure configure = new EntranceConfigure();
            configure.setName(name);
            configure.setPort(port);
            configure.setCls(cls);
            entranceClassMap.put(name, configure);

            if(name.equals("netEvent")){
                netEventEntrance = configure;
            }
            if(name.equals("request")){
                requestEntrance = configure;
            }
        }
//        if(netEventEntrance == null){
//            throw new MMException("configure has no netEventEntrance");
//        }
//        if(requestEntrance == null){
//            throw new MMException("configure has no requestEntrance");
//        }
//        if(roomEntrance == null){
//            throw new MMException("configure has no roomEntrance");
//        }
    }

    public EntranceConfigure getNetEventEntrance() {
        return netEventEntrance;
    }

    private Class<?> getBeanFromConfigure(String beanType,String defaultBeanType){
        String classPath= ConfigHelper.getString(beanType,defaultBeanType);
        if(StringUtils.isEmpty(classPath)){
            throw new RuntimeException("class bean set is Invalid ,value is "+classPath);
        }

        Class<?> result;
        try {
            result=Class.forName(classPath);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("class bean set is Invalid ,value is "+classPath);
        }
        return result;
    }

    public  Map<Class<?>,Class<?>> getConfigureBeans(){
        return configureBeans;
    }
    public String getDefaultRequestController(){
        return defaultRequestController;
    }

    public String getString(String key){
        return ConfigHelper.getString(key);
    }

    public String getString(String key, String defaultVal){
        String val = ConfigHelper.getString(key);
        if(StringUtils.isBlank(val)) {
            return defaultVal;
        }
        return val;
    }

    public Integer getInteger(String key) {
        String val = ConfigHelper.getString(key);
        if(StringUtils.isBlank(val)) {
            return null;
        }
        return Integer.parseInt(val);
    }

    public int getInteger(String key, int defVal) {
        String val = ConfigHelper.getString(key);
        if(StringUtils.isBlank(val)) {
            return defVal;
        }
        return Integer.parseInt(val);
    }


    public boolean getBoolean(String key, boolean def) {
        String val = ConfigHelper.getString(key);
        if(StringUtils.isBlank(val)) {
            return def;
        }
        return Boolean.parseBoolean(val);
    }

    public Map<String, EntranceConfigure> getEntranceClassMap() {
        return entranceClassMap;
    }

    public boolean isAsyncServer(){ // 是否是异步服务器
        return true;
    }
    public int getNetEventPort(){
        if(netEventEntrance == null){
            return -1;
        }
        return netEventEntrance.getPort();
    }
    public int getRequestPort(){
        return requestEntrance.getPort();
    }

    public ServerInfo getMainServerInfo(){
        Map<String, String> mainServerMap = ConfigHelper.getMap("mainServer");
        ServerInfo ret = new ServerInfo();
        ret.setId((Integer.parseInt(mainServerMap.get("mainServer.id"))));
        ret.setHost(mainServerMap.get("mainServer.host"));
        ret.setNetEventPort((Integer.parseInt(mainServerMap.get("mainServer.port"))));
        return ret;
    }

    public String[] getAllPackets(){
        List<String> stringList = new ArrayList<>();
        stringList.add("com.peony.common");
        stringList.add("com.peony.core");
        stringList.add("com.peony.entrance");
        stringList.add("com.peony.platform");
        String appPackage = getString("appPackage");
        if(StringUtils.isNotBlank(appPackage)){
            stringList.add(appPackage);
        }
        return stringList.toArray(new String[stringList.size()]);
    }

}
