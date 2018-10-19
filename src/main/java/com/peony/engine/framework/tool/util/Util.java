package com.peony.engine.framework.tool.util;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.model.CountryResponse;
import com.maxmind.geoip2.record.Country;
import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.server.Server;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.websocket.RemoteEndpoint;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Administrator on 2015/11/16.
 */
public final class Util {
    private static final Logger log = LoggerFactory.getLogger(Util.class);

    private static FileInputStream database = null;
    private static DatabaseReader reader = null;

    static {
        try {
//            database = new FileInputStream("config/GeoIP2-Country.mmdb");
            database = new FileInputStream(ClassUtil.getClassLoader().getResource("GeoIP2-Country.mmdb").getPath());
            reader = new DatabaseReader.Builder(database).build();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (database != null) {
                try {
                    database.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 获取http访问的ip
    public static String getIp(HttpServletRequest request) {
        // We look if the request is forwarded
        // If it is not call the older function.
        String ip = request.getHeader("X-Pounded-For");
        if (ip != null) {
            return ip;
        }
        ip = request.getHeader("x-forwarded-for");

        if (ip == null) {
            return request.getRemoteAddr();
        } else {
            // Process the IP to keep the last IP (real ip of the computer on
            // the net)
            StringTokenizer tokenizer = new StringTokenizer(ip, ",");

            // Ignore all tokens, except the last one
            for (int i = 0; i < tokenizer.countTokens() - 1; i++) {
                tokenizer.nextElement();
            }
            ip = tokenizer.nextToken().trim();
            if (ip.equals("")) {
                ip = null;
            }
        }
        // If the ip is still null, we insert 0.0.0.0 to avoid null values
        if (ip == null) {
            ip = "0.0.0.0";
        }

        return ip;
    }
    // 获取websocket的ip
    //the socket object is hidden in WsSession, so you can use reflection to got the ip address.
    // the execution time of this method is about 1ms. this solution is not prefect but useful.
    public static String getIp(javax.websocket.Session session){
        RemoteEndpoint.Async async = session.getAsyncRemote();
        InetSocketAddress addr = (InetSocketAddress) getFieldInstance(async,
                "base#sos#socketWrapper#socket#sc#remoteAddress");
        if(addr == null){
            return "127.0.0.1";
        }
        return addr.getAddress().getHostAddress();
    }
    private static Object getFieldInstance(Object obj, String fieldPath) {
        String fields[] = fieldPath.split("#");
        for(String field : fields) {
            obj = getField(obj, obj.getClass(), field);
            if(obj == null) {
                return null;
            }
        }
        return obj;
    }

    private static Object getField(Object obj, Class<?> clazz, String fieldName) {
        for(;clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                Field field;
                field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(obj);
            } catch (Exception e) {
            }
        }
        return null;
    }
    public static boolean isLocalHost(String host){
        if(host.equals("localhost") || host.equals("127.0.0.1")){
            return true;
        }
        String localHost = getHostAddress();
        if(host.equals(localHost)){
            return true;
        }
        return false;
    }
    private static String hostAddress = null;
    public static String getHostAddress(){
        if(hostAddress == null){
            try {
                hostAddress = getIpAdd();//InetAddress.getLocalHost().getHostAddress();// 这种方式在linux中会有问题
                log.info("self ip = "+ hostAddress);
            }catch (SocketException|UnknownHostException e){
                throw new MMException(e);
            }
        }
        return hostAddress;
    }


    /**
     * 根据网卡获得IP地址
     * @return
     * @throws SocketException
     * @throws UnknownHostException
     */
    public static  String getIpAdd() throws SocketException, UnknownHostException{
        String ip="";
        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
            NetworkInterface intf = en.nextElement();
            String name = intf.getName();
            if (!name.contains("docker") && !name.contains("lo")) {
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    //获得IP
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        String ipaddress = inetAddress.getHostAddress().toString();
                        if (!ipaddress.contains("::") && !ipaddress.contains("0:0:") && !ipaddress.contains("fe80")) {

                            if(!"127.0.0.1".equals(ip)){
                                ip = ipaddress;
                            }
                        }
                    }
                }
            }
        }
        if(ip.equals("")){
            throw new MMException("get ip fail");
        }
        return ip;
    }




    public static String getLocalNetEventAdd(){
        return getHostAddress()+""+ Server.getEngineConfigure().getNetEventEntrance();
    }
    public static boolean isIP(String addr)
    {
        if(addr.length() < 7 || addr.length() > 15 || "".equals(addr))
        {
            return false;
        }
        /**
         * 判断IP格式和范围
         */
        String rexp = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";

        Pattern pat = Pattern.compile(rexp);

        Matcher mat = pat.matcher(addr);

        boolean ipAddress = mat.find();

        return ipAddress;
    }


    public static <K> List<K> split2List(String content,Class<K> cls){
        return split2List(content,cls,";");
    }
    public static <K> List<K> split2List(String content,Class<K> cls,String sp){
        List<K> ret = new LinkedList<>();
        if (StringUtils.isBlank(content)) {
            return ret;
        }
        String[] entryArray = StringUtils.splitByWholeSeparator(content, sp);
        for (String entry : entryArray) {
            ret.add(convert(cls, entry));
        }
        return ret;
    }

    public static <K> List<List<K>> split2NestingList(String content, Class<K> cls){
        List<List<K>> ret = new ArrayList<>();
        if (StringUtils.isBlank(content)) {
            return ret;
        }
        String[] entryArray = StringUtils.splitByWholeSeparator(content, "|");
        if (entryArray != null && entryArray.length != 0) {
            for (String entry : entryArray) {
                String[] keyValueArray = StringUtils.splitByWholeSeparator(entry, ";");
                ArrayList<K> list = new ArrayList<>();
                for(String val: keyValueArray) {
                    list.add(convert(cls, val));
                }
                ret.add(list);
            }
        }
        return ret;
    }

    public static <K,V> Map<K,V> split2Map(String content,Class<K> kCls,Class<V> vCls){
        Map<K, V> ret = new HashMap<>();
        if (StringUtils.isBlank(content)) {
            return ret;
        }
        String[] entryArray = StringUtils.splitByWholeSeparator(content, "|");
        if (entryArray != null && entryArray.length != 0) {
            for (String entry : entryArray) {
                String[] keyValueArray = StringUtils.splitByWholeSeparator(entry, ";");
                if (keyValueArray.length == 2) {
                    ret.put(convert(kCls, keyValueArray[0]), convert(vCls, keyValueArray[1]));
                }
            }
        }
        return ret;
    }

    public static <K,V> String map2String(Map<K,V> map){
        if(map == null){
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String seperator = "";
        for(Map.Entry entry : map.entrySet()){
            sb.append(seperator).append(entry.getKey().toString()).append(";").append(entry.getValue().toString());
            seperator = "|";
        }
        return sb.toString();
    }

    public static <K> String list2String(List<K> list) {
        if (list == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder("");
        String seperator = "";
        for (K entry : list) {
            sb.append(seperator).append(entry.toString());
            seperator = ";";
        }
        return sb.toString();
    }

    public static <T> T convert(Class<T> clazz, String content) {
        if (clazz.isAssignableFrom(Integer.class)) {
            return clazz.cast(Integer.parseInt(content));
        } else if (clazz.isAssignableFrom(Long.class)) {
            return clazz.cast(Long.parseLong(content));
        } else if (clazz.isAssignableFrom(Short.class)) {
            return clazz.cast(Short.parseShort(content));
        } else if (clazz.isAssignableFrom(Byte.class)) {
            return clazz.cast(Byte.parseByte(content));
        } else if (clazz.isAssignableFrom(Boolean.class)) {
            return clazz.cast(Boolean.parseBoolean(content));
        } else if (clazz.isAssignableFrom(Double.class)) {
            return clazz.cast(Double.parseDouble(content));
        } else if (clazz.isAssignableFrom(Float.class)) {
            return clazz.cast(Float.parseFloat(content));
        } else if (clazz.isAssignableFrom(String.class)) {
            return clazz.cast(content);
        } else {
            throw new RuntimeException("不支持的类型");
        }
    }

    public static Class getUnboxingClass(Class clazz){
        if (clazz.isAssignableFrom(Integer.class)) {
            return int.class;
        } else if (clazz.isAssignableFrom(Long.class)) {
            return long.class;
        } else if (clazz.isAssignableFrom(Short.class)) {
            return short.class;
        } else if (clazz.isAssignableFrom(Byte.class)) {
            return byte.class;
        } else if (clazz.isAssignableFrom(Boolean.class)) {
            return boolean.class;
        } else if (clazz.isAssignableFrom(Double.class)) {
            return double.class;
        } else if (clazz.isAssignableFrom(Float.class)) {
            return float.class;
        } else if (clazz.isAssignableFrom(String.class)) {
            return String.class;
        } else {
            return clazz;
        }
    }

    public static String getCountryCode(String ip) {
        String countryCode = null;
        if (reader != null) {
            try {
                InetAddress ipAddress = InetAddress.getByName(ip);
                CountryResponse response = reader.country(ipAddress);

                Country country = response.getCountry();
                countryCode = country.getIsoCode();
            } catch (Exception e) {
//                e.printStackTrace();
                if(e instanceof AddressNotFoundException){
                    // The address 10.1.38.89 is not in the database
                    log.warn(e.getMessage());
                }else{
                    e.printStackTrace();
                }
            }
        } else {
            log.info("GeoIP DatabaseReader is null");
        }
        return countryCode;
    }

    /** 获取服务器的utc时间的long值  单位ms**/
    public static long getSystemUtcTime(){
        return Calendar.getInstance().getTimeInMillis();
    }

    public static long getBeginTimeToday(){
        return getBeginTimeOfDay(new Date());
    }
    public static long getBeginTimeOfDay(Date date){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static String camelName(String name) {
        StringBuilder result = new StringBuilder();
        name = name.replaceAll("-", "_");

        // 快速检查
        if (name == null || name.isEmpty()) {
            // 没必要转换
            return "";
        } else if (!name.contains("_")) {
            // 不含下划线，仅将首字母小写
            return name.substring(0, 1).toUpperCase() + name.substring(1);
        }

        // 用下划线将原始字符串分割
        String camels[] = name.split("_");
        for (String camel : camels) {
            // 跳过原始字符串中开头、结尾的下换线或双重下划线
            if (camel.isEmpty()) {
                continue;
            }
            // 其他的驼峰片段，首字母大写
            result.append(camel.substring(0, 1).toUpperCase());
            result.append(camel.substring(1).toLowerCase());
        }
        return result.toString();
    }

    public static int randomInt(int bound){
        return RandomNumberGeneratorHolder.randomNumberGenerator.nextInt(bound);
    }
    public static int randomInt(int min,int max){
        return RandomNumberGeneratorHolder.randomNumberGenerator.nextInt(max-min+1)+min;
    }

    public static float randomFloat(float min,float max){
        int minInt = (int)(min*100);
        int maxInt = (int)(max*100);
        return (RandomNumberGeneratorHolder.randomNumberGenerator.nextInt(maxInt-minInt+1)+minInt)/100f;
    }

    public static int randomInt(String minMax){
        String[] data = minMax.split(";");
        return randomInt(Integer.parseInt(data[0]),Integer.parseInt(data[1]));
    }

    public static float randomFloat(String minMax){
        String[] data = minMax.split(";");
        return randomFloat(Float.parseFloat(data[0]),Float.parseFloat(data[1]));
    }

    private static final class RandomNumberGeneratorHolder {
        static final Random randomNumberGenerator = new Random();
    }

    public static String formatMsg(String msg,Object... args){
        return MessageFormatter.arrayFormat(msg,args);
    }
}
