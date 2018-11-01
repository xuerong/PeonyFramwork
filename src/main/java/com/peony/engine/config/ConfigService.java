package com.peony.engine.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.peony.engine.config.core.*;
import com.peony.engine.config.core.field.ConfigFieldTypeFactory;
import com.peony.engine.config.core.field.IFieldType;
import com.peony.engine.config.core.verify.VerifyContext;
import com.peony.engine.config.core.watch.FileListener;
import com.peony.engine.config.core.watch.ResourceListener;
import com.google.api.client.util.Lists;
import com.google.common.base.Charsets;
import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.tool.helper.ClassUtil0;
import com.peony.engine.framework.tool.helper.ConfigHelper;
import com.peony.engine.framework.tool.util.Util;
import com.opencsv.CSVReader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * 基于csv格式的配置文件
 * =========================================
 * 潜规则:
 * 分隔符为 `
 * 第一列名称必须是 id 类型必须是 整数或字符串(int/string)
 * 分隔符优先级  |;,:#$
 * =========================================
 * <p>
 * 文件格式:
 * <p>
 * ID`名称`属性
 * id`namekey`equips
 * int`array<string>`map<int,int>
 * range:1-100|notnull`ref:build-name`range:1-5|ref:reward-id
 * cs`cs`s
 * 1`build1;build2`1;1001|2;1002|3;1003
 * 2`build3;build4`1;1001|2;1002
 * =========================================
 * <p>
 * 第一行    中文描述
 * 第二行    字段名
 * 第三行    数据类型
 * 第四行    验证信息
 * 第五行    前/后端读取标记(c 前端, s 后端, cs 前后端都读)
 * ... 数据行...
 * <p>
 * <p>
 * 支持的数据类型如下(详细见 FieldType 可自行扩展):
 * <p>
 * 简单的值类型:
 * bool string int long double
 * <p>
 * 集合类型:                            数据格式
 * array<int>                          1;2;3
 * array<string>                       a;b;c
 * map<int,int>                        a;1|b;2
 * map<int,string>                     a;1|b;2
 * map<string,string>                  a;1|b;2
 * map<string,int>                     a;1|b;2
 * map<string,array<string>>           a;1,2,3|b;2,3,4
 * <p>
 * tuple2<int,int>                     1;2
 * tuple2<int,string>                  1;a
 * tuple2<string,string>               a;b
 * tuple2<string,int>                  a;1
 * <p>
 * <p>
 * 格式验证:                                                                           例子
 * 引用验证                              {t:'ref',p:[fileName,fieldName]}      {t:'ref',p:['item','id']}           (当前列是 item.csv的 id)
 * 范围验证(数字)                         {t:'range',p:[min,max]}               {t:'range',p:[1000,2000]}           (当前列的范围 [1000-2000])
 * 唯一性验证:                            {t:'unique'}                          {t:'unique'}                        (当前列不允许重复)
 * 非空验证:                             {t:'notnull'}                          {t:'notnull'}                      (当前列必填)
 * 列默认值:                             {t:'def'}                              {t:'def', p:[0]}                   (当前列默认值为0)
 * <p>
 * 如果需要对一列进行多项验证时可以这样:       [{t:'notnull'},{t:'unique'},{t:'range',p:[1000,2000]}]
 * <p>
 * ##### 验证字段采用 json 格式
 * 一个验证项的结构如下:
 * {
 * t:(字符串) 验证类型    如: ref/range/unique/notnull
 * p:(数组)  参数        无参验证项可不写 p 属性.
 * }
 * <p>
 * 对于一个配置列的验证,可以指定一个或者多个验证(多个验证需用数组[]标记)
 * 例: 对一个单值得列同时使用了 notnull:非空; ref:引用; range:范围 三个验证.
 * [{t:'notnull'},{t:'ref',p:['tableName','columnName']},{t:'range',p:[100,2000]}]
 * 可以描述为: 该列必填,引用自tableName表的columnName列的值,数值范围在[100-2000]之间.
 * <p>
 * ###### 对于像 map / tuple2 等多值字段则需要指定所验证的特定字段
 * map类型的验证
 * {
 * k:[{t:'notnull'},{t:'ref',p:['tableName','columnName']},{t:'range',p:[100,2000]}],
 * v:[{t:'notnull'},{t:'ref',p:['tableName','columnName']},{t:'range',p:[100,2000]}]
 * }
 * <p>
 * tuple2类型的验证
 * {
 * f:[{t:'notnull'},{t:'ref',p:['tableName','columnName']},{t:'range',p:[100,2000]}],
 * s:[{t:'notnull'},{t:'ref',p:['tableName','columnName']},{t:'range',p:[100,2000]}]
 * }
 *
 * 另: 如果针对某字段的验证信息太多不太好编辑, 可以写到 verify.json
 *
 * Created by jiangmin.wu on 17/7/20.
 */
@Service(init = "init",initPriority = 1)
public class ConfigService {
    public static final char CSV_SEPARATOR = '`';
    private static Pattern FIELDNAME_PATTERN = Pattern.compile("[a-z-A-Z]{1}[a-z-A-Z_0-9]+");
    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);
    public static String CONFIG_PATH = "./config/csv";
    public static String CONFIG_PATH2 = ConfigService.class.getClassLoader().getResource("csv").getPath();
    private String implPack = ConfigHelper.getString("appPackage")+".config";
    private PropertyConfig property = new PropertyConfig();
    private AtomicInteger updates = new AtomicInteger(0);
    private ScheduledExecutorService updater;
    private Map<String, Class<? extends ConfigContainer>> containerClasses = new HashMap<>();
    private Map<Class<?>, ConfigContainer<?, IConfig<?>>> configContainers = new ConcurrentHashMap<>();

    private JSONObject configVerifyDef = new JSONObject();

    public void init() {
        try {
            doInit(ConfigHelper.getString("appPackage")+".config", CONFIG_PATH2);
        } catch (Exception e) {
            throw new MMException("ConfigService init error", e);
        }
    }

    public PropertyConfig getPropertyConfig() {
        return property;
    }

    private void initContainer() {
        int count = 0;
        List<Class<?>> classes = ClassUtil0.getClassList(implPack, true, Config.class, getClass().getClassLoader());
        for (Class<?> cls : classes) {
            Config config = cls.getAnnotation(Config.class);
            if (config != null) {
                count++;
                containerClasses.put(config.value(), (Class<? extends ConfigContainer>) cls);
            }
        }
        logger.info("-----init ConfigContainer: {}-----", count);
    }

    @SuppressWarnings("unchecked")
    public <T extends ConfigContainer> T getContainer(String name) {
        for (ConfigContainer<?, IConfig<?>> t : configContainers.values()) {
            if (t.getMetaData().getFileName().equals(name)) {
                return (T) t;
            }
        }
        logger.error("config [{}.csv] not found !", name);
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends ConfigContainer> T getContainer(Class<T> clazz) {
        return (T) configContainers.get(clazz);
    }

    @SuppressWarnings("unchecked")
    public <T extends ConfigContainer, I, C extends IConfig<I>> C getItem(Class<T> clazz, I id) {
        return (C) ((T)configContainers.get(clazz)).getConfig(id);
    }

    public <T extends ConfigContainer, I, C extends IConfig<I>> List<C> getList(Class<T> clazz) {
        return ((T)configContainers.get(clazz)).getAll();
    }

    public <T extends ConfigContainer, I, C extends IConfig<I>> Map<I, C> getConfigMap(Class<T> clazz) {
        return ((T) configContainers.get(clazz)).getMap();
    }

    public void doInit(String implPack, String resPath) throws Exception {
        this.implPack = implPack;
        CONFIG_PATH = resPath;

        initContainer();
        load();

        // 更新检查
        final File root = new File(CONFIG_PATH);
        ResourceListener.addListener(root.getPath(), new FileListener() {

            @Override
            public void onEvent(String rootPath, WatchEvent<Path> pathEvent) {
                String fileName = pathEvent.context().getFileName().toString();
                if (fileName.endsWith(".properties")) {
                    try {
                        // 实时更新
                        property.loadConfig(fileName);
                    } catch (Exception e) {
                        logger.error("load config error " + fileName, e);
                    }
                }
                if (fileName.endsWith(".csv")) {
                    File file = pathEvent.context().toFile();
                    if (!file.exists()) {
                        Iterator<File> it = FileUtils.iterateFiles(root, new String[]{"csv"}, true);
                        while (it.hasNext()) {
                            File tmp = it.next();
                            if (tmp.getName().equals(fileName)) {
                                file = tmp;
                                break;
                            }
                        }
                    }
                    // 修改更新文件个数, 由于涉及到引用验证, 所以使用定时扫描来统一更新所有csv配置文件
                    if (file.exists()) {
                        updates.incrementAndGet();
                    }
                }
            }

            @Override
            public Kind<?>[] events() {
                return new Kind<?>[]{StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.OVERFLOW};
            }
        });

        updater = new ScheduledThreadPoolExecutor(1, (task) -> new Thread(task, "Config Timer"));
        // 每30S检查更新
        updater.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                int count = updates.get();
                if (count > 0) {
                    try {
                        load();
                    } catch (Exception e) {
                        logger.error("load config error", e);
                    } finally {
                        // 如果结果变化了就不管了, 下次在继续更新
                        updates.compareAndSet(count, 0);
                    }
                }
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    public Object getFieldVerifyDef(String fileName, String fieldName) {
        if(!configVerifyDef.containsKey(fileName)) {
            return null;
        }
        JSONObject fileDef = configVerifyDef.getJSONObject(fileName);
        if(!fileDef.containsKey(fieldName)) {
            return null;
        }
        return fileDef.get(fieldName);
    }

    @SuppressWarnings("unchecked")
    private void load() throws Exception {
        long start = System.currentTimeMillis();

        File virifyJsonFile = new File(CONFIG_PATH+"/verify.json");
        if(virifyJsonFile.exists()) {
            configVerifyDef = JSON.parseObject(FileUtils.readFileToString(virifyJsonFile, Charset.forName("UTF-8")));
        }

        logger.info("load config start");
        Map<Class<?>, ConfigContainer<?, IConfig<?>>> containers = new ConcurrentHashMap<>();
        for (File f : new File(CONFIG_PATH).listFiles((dir, name) -> name.endsWith(".csv"))) {
            // 配置文件读取解析
            String fileName = f.getName().toLowerCase();
            fileName = fileName.substring(0, fileName.indexOf("."));

            if (!FIELDNAME_PATTERN.matcher(fileName).matches()) {
                throw new ConfigException("config file name is invalid " + fileName);
            }

            Class<?> containerClass = getContainerClass(fileName);
            if (containerClass == null) {
                logger.error("ConfigContainer not exists {}", f.getName());
                continue;
            }

            ConfigMetaData metaData = getConfigMetaData(f);
            List<IConfig<?>> dataList = parseConfigData(f);

            ConfigContainer<?, IConfig<?>> container = (ConfigContainer<?, IConfig<?>>) containerClass.newInstance();
            container.setMetaData(metaData);
            container.init(dataList);
            containers.put(containerClass, container);
            logger.info("load config succ " + fileName);
        }

        // 合法性检查
        VerifyContext context = new VerifyContext(containers);
        StringBuilder log = new StringBuilder();

        // 自动验证
        for (ConfigContainer<?, IConfig<?>> container : containers.values()) {
            String ret = container.verify(context, false);
            if (StringUtils.isNotBlank(ret)) {
                log.append(ret);
            }
        }

        // 手动验证
        for (ConfigContainer<?, IConfig<?>> container : containers.values()) {
            String ret = container.check(context, false);
            if (StringUtils.isNotBlank(ret)) {
                log.append(ret);
            }
        }

        String error = log.toString();
        if (StringUtils.isNotBlank(error)) {
            throw new ConfigException(error);
        }
        configContainers = containers;
        logger.info("load config finish " + (System.currentTimeMillis() - start));
    }

    private Class<? extends ConfigContainer> getContainerClass(String fileName) throws ClassNotFoundException {
        Class<? extends ConfigContainer> containerClass = null;

        containerClass = containerClasses.get(fileName);
        if (containerClass != null) {
            return containerClass;
        }

        String containerName = Util.camelName(fileName) + "Container";
        try {
            containerClass = (Class<? extends ConfigContainer>) Class.forName(ConfigHelper.getString("appPackage")+".config." + containerName);
            return containerClass;
        } catch (ClassNotFoundException e) {
            logger.error("ClassNotFoundException " + containerName);
            return null;
        }
    }

    private ConfigMetaData getConfigMetaData(File file) {
        CSVReader csvReader = null;
        ConfigMetaData metaData = new ConfigMetaData();
        try {
            String tableName = file.getName().toLowerCase();
            tableName = tableName.substring(0, tableName.indexOf("."));
            metaData.setFileName(tableName);

            csvReader = new CSVReader(new InputStreamReader(new FileInputStream(file), Charsets.UTF_8), CSV_SEPARATOR);
            List<String[]> content = csvReader.readAll();
            String[] descs = content.get(0); // desc
            String[] coltypes = content.get(1); // type
            String[] colnames = content.get(0); // name
            //String[] verifies = content.get(3); // verify
            //String[] flags = content.get(4); // flag

            Map<String, ConfigFieldMeta> fields = new LinkedHashMap<>();
            for (int k = 0; k < coltypes.length; k++) {
                coltypes[k] = coltypes[k].trim();
                if (coltypes[k].isEmpty()) {
                    continue;
                }

                if (k == 0) {
                    colnames[k] = colnames[k].toLowerCase();
                }

                ConfigFieldMeta field = new ConfigFieldMeta(this, tableName, coltypes[k], descs[k], colnames[k],
                        //verifies[k], flags[k]
                        "", "s"
                );
                fields.put(field.getName(), field);
            }
            metaData.setFields(fields);
        } catch (Exception e) {
            throw new ConfigException("init config meta error " + file.getName(), e);
        } finally {
            if (csvReader != null) {
                try {
                    csvReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return metaData;
    }

    @SuppressWarnings("unused")
    private List<IConfig<?>> parseConfigData(File file) {
        CSVReader csvReader = null;
        List<IConfig<?>> dataList = Lists.newArrayList();
        try {
            String tableName = file.getName().toLowerCase();
            tableName = tableName.substring(0, tableName.indexOf("."));

            Class<?> containerClass = getContainerClass(tableName);
            if (containerClass == null) {
                logger.error("ConfigContainer not exists {}", tableName);
                return null;
            }

            ParameterizedType parameterizedType = (ParameterizedType) containerClass.getGenericSuperclass();
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            Class<?> configClass = (Class<?>) actualTypeArguments[1];

            // -----------

            csvReader = new CSVReader(new InputStreamReader(new FileInputStream(file), Charsets.UTF_8), CSV_SEPARATOR);
            List<String[]> content = csvReader.readAll();
            String[] descs = content.get(0); // desc
            String[] coltypes = content.get(1); // type
            String[] colnames = content.get(0); // name
//            String[] verifies = content.get(3); // verify
//            String[] flags = content.get(4); // flag
            int firstDataLineIndex = 2;

            Map<String, Field> fieldCache = com.google.common.collect.Maps.newHashMap();
            for (int i = firstDataLineIndex; i < content.size(); i++) {

                IConfig<?> config = (IConfig<?>) configClass.newInstance();

                int index = 0;
                for (String col : content.get(i)) {
                    String name = colnames[index]/* .toLowerCase().trim() */;
                    String type = coltypes[index].toLowerCase().trim();
                    String flag = "s";// flags[index].toLowerCase();

                    index++;

                    if (!flag.equals("cs") && !flag.equals("s")) {
                        continue;
                    }

                    Object val = null;
                    IFieldType fieldType = ConfigFieldTypeFactory.getFieldType(type);
                    if (fieldType == null) {
                        throw new ConfigException(String.format("FieldType not found %s in %s", type, file.getName()));
                    }

                    if(StringUtils.isBlank(col)) {
                        continue;
                    }

                    try {
                        val = fieldType.parseValue(col);
                    } catch (Exception e) {
                        logger.error("Config parse error {} {} {} {}", tableName, name, type, col);
                        throw e;
                    }

                    Field field = fieldCache.get(name);
                    if (field == null) {
                        field = configClass.getDeclaredField(name);
                        field.setAccessible(true);
                        fieldCache.put(name, field);
                    }
                    field.set(config, val);
                }
                dataList.add(config);
            }

        } catch (ClassNotFoundException e) {
            logger.error("ClassNotFoundException " + file.getName());
        } catch (Exception e) {
            throw new ConfigException("parse config error :" + file.getName(), e);
        } finally {
            if (csvReader != null) {
                try {
                    csvReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return dataList;
    }
}
