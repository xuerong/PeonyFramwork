package com.peony.engine.framework.data.persistence.orm;

import com.peony.engine.framework.data.persistence.dao.ColumnDesc;
import com.peony.engine.framework.data.persistence.dao.DatabaseHelper;
import com.peony.engine.framework.data.persistence.orm.annotation.Column;
import com.peony.engine.framework.data.persistence.orm.annotation.DBEntity;
import com.peony.engine.framework.data.persistence.orm.annotation.StringTypeCollation;
import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.tool.helper.ClassHelper;
import com.peony.engine.framework.tool.helper.ConfigHelper;
import com.peony.engine.framework.tool.util.ObjectUtil;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 初始化 DBEntity 结构
 *
 * @author huangyong
 * @since 1.0
 */
public class EntityHelper {
    private static final Logger log = LoggerFactory.getLogger(EntityHelper.class);
    /**
     * 实体类 => 表名
     */
    private static final Map<Class<?>, TableInfo> entityClassTableNameMap = new HashMap<Class<?>, TableInfo>();

    /**
     * 实体类 => (字段名 => 列名)
     * TODO 这里面只需要存储entity中对应表中有的字段
     */
    private static final Map<Class<?>, Map<String, String>> entityClassFieldMapMap = new HashMap<Class<?>, Map<String, String>>();

    /**
     * DBEntity类与所有的get方法
     */
    private static final Map<Class<?>,Map<String,Method>> getMethodMap = new HashMap<>();

    /**
     * DBEntity类与所有的主键的get方法
     * fieldName-method
     */
    private static final Map<Class<?>,Map<String,Method>> getPkMethodMap = new HashMap<>();

    public static Map<String,Method> getGetMethodMap(Class<?> entityClass){
        return getMethodMap.get(entityClass);
    }
    public static Map<String,Method> getPkGetMethodMap(Class<?> entityClass){
        return getPkMethodMap.get(entityClass);
    }



    static {
        try {
            // 获取并遍历所有实体类
            // TODO 校验数据库中对应的表的存在和对应的字段,只需要数据库中存在的列即可
            List<Class<?>> entityClassList = ClassHelper.getClassListByAnnotation(DBEntity.class);
            for (Class<?> entityClass : entityClassList) {
//                initEntityNameMap(entityClass);
//                initEntityFieldMapMap(entityClass);
//                initEntityGetMethods(entityClass);
                initEntity(entityClass);
            }
        }catch (Throwable e){
            e.printStackTrace();
        }
    }

    /**
     * 构建key组成的sql语句中的condition
     * @param object
     * @return
     */
    public static ConditionItem parsePkCondition(Object object){
        Map<String,Method> map = getPkGetMethodMap(object.getClass());
        if(map == null || map.size() == 0){
            throw new MMException("getPkGetMethodMap is null , class = "+object.getClass());
        }

        Object[] params = new Object[map.size()];
        StringBuilder sb = new StringBuilder();
        int i=0;
        try {
            for(Map.Entry<String,Method> entry : map.entrySet()){
                sb.append(entry.getKey()+"=? and ");
                params[i++] = entry.getValue().invoke(object);
            }
        }catch (IllegalAccessException |InvocationTargetException e){
            throw new MMException(e);
        }
        String condition = "";
        if(sb.length()>0){
            condition = sb.substring(0,sb.length()-4);
        }
        ConditionItem result = new ConditionItem();
        result.setParams(params);
        result.setCondition(condition);
        return result;
    }

    public static class ConditionItem{
        private String condition;
        private Object[] params;

        public String getCondition() {
            return condition;
        }

        public void setCondition(String condition) {
            this.condition = condition;
        }

        public Object[] getParams() {
            return params;
        }

        public void setParams(Object[] params) {
            this.params = params;
        }
    }



    private static void initEntity(Class<?> entityClass){
        // ----------------initEntityNameMap
        DBEntity dbEntity=entityClass.getAnnotation(DBEntity.class);
        String tableName=dbEntity.tableName();

        Map<String,ColumnDesc> columnDescMap;
        if(dbEntity.tableNum()>1){
            columnDescMap = DataSet.getTableDescForMap(tableName+"_0");
            //  都检查一遍？不用！
        }else{
            columnDescMap = DataSet.getTableDescForMap(tableName);
        }
        // 查看table是否存在，并获取列名
        columnDescMap = checkAndModifyTable(columnDescMap,entityClass,dbEntity);
//        if(columnDescList == null){
//            log.warn("table is not exist ,tableName = "+tableName+",DBEntity = "+entityClass.getName()+"tableNum="+dbEntity.tableNum());
//            return;
//        }

        Set<String> columnNameSet = new HashSet<>(columnDescMap.size());
        for(ColumnDesc columnDesc : columnDescMap.values()){
//            columnDesc.getType()
            columnNameSet.add(columnDesc.getField());
        }
        if(dbEntity.tableNum() > 1 && dbEntity.pks().length<1){
            throw new MMException("Separate table must has pk ,and first pk must be pk Basis");
        }
        TableInfo tableInfo = new TableInfo(tableName,dbEntity.tableNum()<=1?1:dbEntity.tableNum(),dbEntity.pks().length>0?dbEntity.pks()[0]:null);
        entityClassTableNameMap.put(entityClass, tableInfo);
        // ---------------initEntityFieldMapMap
        // 获取并遍历该实体类中所有的字段（不包括父类中的方法）
        Field[] fields = entityClass.getDeclaredFields();
//        Field[] fields = entityClass.getFields();
        if (ArrayUtils.isEmpty(fields)) {
            log.warn("fields is null,entityClass = "+entityClass.getName());
            return;
        }
        // 创建一个 fieldMap（用于存放列名与字段名的映射关系）
        Map<String, String> fieldMap = new HashMap<String, String>();
        for (Field field : fields) {
            String fieldName = field.getName();
            String columnName;
            // 判断该字段上是否存在 Column 注解
            if (field.isAnnotationPresent(Column.class)) {
                // 若已存在，则使用该注解中定义的列名
                columnName = field.getAnnotation(Column.class).name();
                if(StringUtils.isEmpty(columnName)){
                    columnName = fieldName;
                }
            } else {
                // 若不存在，则直接用列名
                columnName = fieldName;
            }
            if(columnNameSet.contains(columnName)) {
                fieldMap.put(fieldName, columnName);
            }
        }
        entityClassFieldMapMap.put(entityClass, fieldMap);
        // ------------------initEntityGetMethods
        Set<String> set = fieldMap.keySet();
        Map<String,Method> getMethodMap = new HashMap<>(set.size());
        Map<String,Method> getPkMethodMap = new LinkedHashMap<>(set.size()); // 需要维持顺序，第一个是用于分表的主键
//        DBEntity dbEntity = entityClass.getAnnotation(DBEntity.class);
        String[] pks = dbEntity.pks();
        if(pks == null || pks.length==0){
            pks = set.toArray(new String[set.size()]);
            log.warn("DBEntity has no pk Annotation : use all field as pks");
        }
//        List<String> pkList = Arrays.asList(pks);
        for (String fieldName:set) {
            String first = fieldName.substring(0, 1);
            StringBuilder sb = new StringBuilder("get");
            sb.append(first.toUpperCase());
            sb.append(fieldName.substring(1));
            String methodName = sb.toString();
            Method method = null;
            try{
                method = entityClass.getMethod(methodName);
            }catch (NoSuchMethodException e){
                method = null;
            }
            if (method != null) {
                getMethodMap.put(fieldName,method);
//                if(pkList.contains(fieldName)){
//                    getPkMethodMap.put(fieldName,method);
//                }
            } else {
                log.error("DBEntity get method not found: class="
                        + entityClass + ",methodName="
                        + methodName);
            }
        }
        for(String pk : pks){
            getPkMethodMap.put(pk,getMethodMap.get(pk));
        }
        EntityHelper.getMethodMap.put(entityClass,getMethodMap);
        EntityHelper.getPkMethodMap.put(entityClass,getPkMethodMap);
    }


    // 检查并创建表、修改列、添加列、删除列
    private static Map<String,ColumnDesc> checkAndModifyTable(Map<String,ColumnDesc> columnDescList,Class<?> entityClass,DBEntity dbEntity){
        /**
         * #没有则创建
         create_if_not_exist = true
         #缺少字段则添加
         add_if_absent = true
         #类型不同则修改：包括编码格式
         modify_if_type_different = true
         #多出字段则删除：不建议
         delete_if_more = false
         */

        if(columnDescList == null){
            log.warn("\ntable {} is not exist",dbEntity.tableName());
        }

        boolean createIfNotExist = ConfigHelper.getBoolean("create_if_not_exist");
        boolean addIfAbsent = ConfigHelper.getBoolean("add_if_absent");
        boolean modifyIfTypeDifferent = ConfigHelper.getBoolean("modify_if_type_different");
        boolean deleteIfMore = ConfigHelper.getBoolean("delete_if_more");

        boolean create = columnDescList==null && createIfNotExist;

        Field[] fields = entityClass.getDeclaredFields();
        StringBuilder createSb = null;
        if(create){
            createSb = new StringBuilder();
        }
        HashSet<String> columnNameSet = new HashSet<>();
        boolean first = true;
        for (Field field : fields) {
            boolean isStatic = Modifier.isStatic(field.getModifiers());
            if(isStatic){
                continue;
            }
            String fieldName = field.getName();

            String columnName;
            StringTypeCollation collation = null;
            // 判断该字段上是否存在 Column 注解
            if (field.isAnnotationPresent(Column.class)) {
                // 若已存在，则使用该注解中定义的列名
                Column column = field.getAnnotation(Column.class);
                columnName = column.name();
                collation = column.stringColumnType();
                if(StringUtils.isEmpty(columnName)){
                    columnName = fieldName;
                }
            } else {
                // 若不存在，则直接用列名
                columnName = fieldName;
                if(field.getType() == String.class){
                    collation = StringTypeCollation.Varchar128;
                }
            }
            columnNameSet.add(columnName);
            if(create){
                if(!first){
                    createSb.append(",\n");
                }
                createSb.append(createSqlDes(field.getType(),columnName,collation));
            }else{
                // 添加
                if(addIfAbsent && !columnDescList.containsKey(columnName)){
                    // 创建
                    if(dbEntity.tableNum() > 1){
                        for(int i=0;i<dbEntity.tableNum();i++){
                            String tableName = dbEntity.tableName()+"_"+i;
                            String sql = "alter table `" + tableName + "` add column " + createSqlDes(field.getType(), columnName, collation);
                            DatabaseHelper.updateForCloseConn(sql);
                            log.warn("{} is not exist in table {},add it {}",columnName, tableName,sql);
                        }
                    }else {
                        String sql = "alter table `" + dbEntity.tableName() + "` add column " + createSqlDes(field.getType(), columnName, collation);
                        DatabaseHelper.updateForCloseConn(sql);
                        log.warn("{} is not exist in table {},add it {}",columnName, dbEntity.tableName(),sql);
                    }
                }
                // 修改
                if(modifyIfTypeDifferent && columnDescList.containsKey(columnName)){
                    // alter table student modify column sname varchar(20);
                    ColumnDesc columnDesc = columnDescList.get(columnName);
                    boolean changeColumn = false;
                    // 类型不同
                    if(!columnDesc.getType().equals(createSqlType(field.getType(),collation))){
                        changeColumn = true;
                    }
                    // 编码不同
                    if(collation != null && !columnDesc.getCollation().equals(collation.getCollation())){
                        changeColumn = true;
                    }
                    if(changeColumn){
                        if(dbEntity.tableNum() > 1){
                            for(int i=0;i<dbEntity.tableNum();i++){
                                String tableName = dbEntity.tableName()+"_"+i;
                                String sql = "alter table `" + tableName + "` modify column " + createSqlDes(field.getType(), columnName, collation);
                                DatabaseHelper.updateForCloseConn(sql);
                                log.warn("{} is not modified in table {},modify it {}",columnName, tableName,sql);
                            }
                        }else {
                            String sql = "alter table `" + dbEntity.tableName() + "` modify column " + createSqlDes(field.getType(), columnName, collation);
                            DatabaseHelper.updateForCloseConn(sql);
                            log.warn("{} is not modified in table {},modify it {}",columnName, dbEntity.tableName(),sql);
                        }
                    }
                }
            }
            if(first){
                first = false;
            }
        }

        if(create){
            StringBuilder nameBefore = new StringBuilder("create table `");
            StringBuilder nameAfter = new StringBuilder();
            nameAfter.append("`(\n");
            nameAfter.append(createSb);
            if(dbEntity.pks().length>0){
                first = true;
                nameAfter.append(",\n").append("PRIMARY KEY (");
                for(String pk : dbEntity.pks()){
                    // PRIMARY KEY (`shareId`,`fff`);
                    if(first){
                        first = false;
                    }else{
                        nameAfter.append(",");
                    }
                    nameAfter.append("`").append(pk).append("`");
                }
                nameAfter.append(")");
            }
            nameAfter.append("\n) ENGINE=InnoDB DEFAULT CHARSET=utf8;");

            if(dbEntity.tableNum() > 1){
                for(int i=0;i<dbEntity.tableNum();i++){
                    String tableName = dbEntity.tableName()+"_"+i;
                    StringBuilder sb = new StringBuilder(nameBefore).append(tableName).append(nameAfter);
                    log.info("\n"+sb+"\n");
                    DatabaseHelper.updateForCloseConn(sb.toString());
                    columnDescList = DataSet.getTableDescForMap(tableName);
                    if(columnDescList != null) {
                        log.info("create table {} success", tableName);
                    }
                }
            }else{
                StringBuilder sb = new StringBuilder(nameBefore).append(dbEntity.tableName()).append(nameAfter);
                log.info("\n"+sb+"\n");
                DatabaseHelper.updateForCloseConn(sb.toString());
                columnDescList = DataSet.getTableDescForMap(dbEntity.tableName());
                if(columnDescList != null) {
                    log.info("create table {} success", dbEntity.tableName());
                }
            }
        }

        if(deleteIfMore){
            for(Map.Entry<String,ColumnDesc> entry : columnDescList.entrySet()){
                if(!columnNameSet.contains(entry.getKey())){
                    //alter table 表名 drop column 列名;
                    if(dbEntity.tableNum() > 1){
                        for(int i=0;i<dbEntity.tableNum();i++){
                            String tableName = dbEntity.tableName()+"_"+i;
                            String sql = "alter table `" + tableName + "` drop column " + entry.getKey();
                            DatabaseHelper.updateForCloseConn(sql);
                            log.warn("{} do not need in table {},drop it {}",entry.getKey(), tableName,sql);
                        }
                    }else {
                        String sql = "alter table `" + dbEntity.tableName() + "` drop column " + entry.getKey();
                        DatabaseHelper.updateForCloseConn(sql);
                        log.warn("{} do not need in table {},drop it {}",entry.getKey(), dbEntity.tableName(),sql);
                    }
                }
            }
        }

        return columnDescList;
    }

    private static String createSqlType(Class clazz,StringTypeCollation typeCollation){
        if (clazz.isAssignableFrom(int.class) || clazz.isAssignableFrom(Integer.class)) {
            return "int(11)";
        } else if (clazz.isAssignableFrom(long.class)||clazz.isAssignableFrom(Long.class)) {
            return "bigint(20)";
        } else if (clazz.isAssignableFrom(short.class) || clazz.isAssignableFrom(Short.class)) {
            return "smallint(8)";
        } else if (clazz.isAssignableFrom(byte.class)||clazz.isAssignableFrom(Byte.class)) {
            return "tinyint(4)";
        } else if (clazz.isAssignableFrom(boolean.class)||clazz.isAssignableFrom(Boolean.class)) {
            return "tinyint(4)";
        } else if (clazz.isAssignableFrom(double.class)||clazz.isAssignableFrom(Double.class)) {
            return  "double(20)";
        } else if (clazz.isAssignableFrom(float.class)||clazz.isAssignableFrom(Float.class)) {
            return "float(11)";
        } else if (clazz.isAssignableFrom(String.class)) {
            return typeCollation.getTypeDes();
        } else if (clazz.isAssignableFrom(Timestamp.class)) {
            return "timestamp";
        } else {
            if(clazz.isArray()){
                clazz = clazz.getComponentType();
                while (clazz.isArray()){
                    clazz = clazz.getComponentType();
                }
                if(clazz.isPrimitive() || clazz.isAssignableFrom(Serializable.class)){
                    return  "blob";
                }
            }else if(clazz.isAssignableFrom(Serializable.class)){
                return  "blob";
            }

            throw new RuntimeException("不支持的类型");
        }
    }

    private static String createSqlDes(Class clazz,String columnName,StringTypeCollation typeCollation){
        if (clazz.isAssignableFrom(int.class) || clazz.isAssignableFrom(Integer.class)) {
            return new StringBuilder("`").append(columnName).append("` int(11) default 0").toString();
        } else if (clazz.isAssignableFrom(long.class)||clazz.isAssignableFrom(Long.class)) {
            return new StringBuilder("`").append(columnName).append("` bigint(20) default 0").toString();
        } else if (clazz.isAssignableFrom(short.class) || clazz.isAssignableFrom(Short.class)) {
            return new StringBuilder("`").append(columnName).append("` smallint(8) default 0").toString();
        } else if (clazz.isAssignableFrom(byte.class)||clazz.isAssignableFrom(Byte.class)) {
            return new StringBuilder("`").append(columnName).append("` tinyint(4) default 0").toString();
        } else if (clazz.isAssignableFrom(boolean.class)||clazz.isAssignableFrom(Boolean.class)) {
            return  new StringBuilder("`").append(columnName).append("` tinyint(4) default 0").toString();
        } else if (clazz.isAssignableFrom(double.class)||clazz.isAssignableFrom(Double.class)) {
            return  new StringBuilder("`").append(columnName).append("` double(20) default 0").toString();
        } else if (clazz.isAssignableFrom(float.class)||clazz.isAssignableFrom(Float.class)) {
            return  new StringBuilder("`").append(columnName).append("` float(11) default 0").toString();
        } else if (clazz.isAssignableFrom(String.class)) {
            return  new StringBuilder("`").append(columnName).append("` ").append(typeCollation.getDes()).
                    append(typeCollation.getTypeDes().equals("text")?"":" default ''").toString();
        } else if (clazz.isAssignableFrom(Timestamp.class)) {
            return  new StringBuilder("`").append(columnName).append("` timestamp NULL default NULL").toString();
        } else {
            if(clazz.isArray()){
                clazz = clazz.getComponentType();
                while (clazz.isArray()){
                    clazz = clazz.getComponentType();
                }
                if(clazz.isPrimitive() || clazz.isAssignableFrom(Serializable.class)){
                    return  new StringBuilder("`").append(columnName).append("` blob").toString();
                }
            }else if(clazz.isAssignableFrom(Serializable.class)){
                return  new StringBuilder("`").append(columnName).append("` blob").toString();
            }

            throw new RuntimeException("不支持的类型");
        }
    }


    public static Map<String, Object> getFieldMap(Object obj) {
        Map<String, Object> fieldMap = new LinkedHashMap<String, Object>();
        Class cls = obj.getClass();
        Set<String> fieldNameSet = entityClassFieldMapMap.get(cls).keySet(); // 这些是需要保存到数据库中的列
        Field[] fields = obj.getClass().getDeclaredFields();
        for (Field field : fields) {
            String fieldName = field.getName();
            if(!fieldNameSet.contains(fieldName)){
                continue;
            }
            Object fieldValue = ObjectUtil.getFieldValue(obj, fieldName);
            fieldMap.put(fieldName, fieldValue);
        }
        return fieldMap;
    }
    /**
     * 将驼峰风格替换为下划线风格
     */
    public static String camelhumpToUnderline(String str) {
        Matcher matcher = Pattern.compile("[A-Z]").matcher(str);
        StringBuilder builder = new StringBuilder(str);
        for (int i = 0; matcher.find(); i++) {
            builder.replace(matcher.start() + i, matcher.end() + i, "_" + matcher.group().toLowerCase());
        }
        if (builder.charAt(0) == '_') {
            builder.deleteCharAt(0);
        }
        return builder.toString();
    }

    public static String getTableName(Class<?> entityClass,int id/*用于分表的id*/) {
        TableInfo tableInfo = entityClassTableNameMap.get(entityClass);
        if(tableInfo == null){
            log.error("table is null,entityClass = {}",entityClass);
            return null;
        }
        if(tableInfo.getTableNum() == 1){
            return tableInfo.getTableName();
        }
        return tableInfo.getTableName()+"_"+Math.abs(id)%tableInfo.getTableNum();
    }

    public static Map<String, String> getFieldMap(Class<?> entityClass) {
        return entityClassFieldMapMap.get(entityClass);
    }

    public static Map<String, String> getColumnMap(Class<?> entityClass) {
        return invert(getFieldMap(entityClass));
    }

    public static String getColumnName(Class<?> entityClass, String fieldName) {
        String columnName = getFieldMap(entityClass).get(fieldName);
        return StringUtils.isNotEmpty(columnName) ? columnName : fieldName;
    }
    /**
     * 转置 Map
     */
    public static <K, V> Map<V, K> invert(Map<K, V> source) {
        Map<V, K> target = null;
        if (MapUtils.isNotEmpty(source)) {
            target = new LinkedHashMap<V, K>(source.size());
            for (Map.Entry<K, V> entry : source.entrySet()) {
                target.put(entry.getValue(), entry.getKey());
            }
        }
        return target;
    }

    public static TableInfo getTableInfo(Class<?> cls){
        return entityClassTableNameMap.get(cls);
    }

    public static class TableInfo{
        private String tableName;
        private int tableNum;
        private String tablePk; // 分表用的主键，一般是玩家id

        public TableInfo(String tableName,int tableNum,String tablePk){
            this.tableName = tableName;
            this.tableNum = tableNum;
            this.tablePk = tablePk;
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public int getTableNum() {
            return tableNum;
        }

        public void setTableNum(int tableNum) {
            this.tableNum = tableNum;
        }

        public String getTablePk() {
            return tablePk;
        }

        public void setTablePk(String tablePk) {
            this.tablePk = tablePk;
        }

        public String toString(){
            return "tableName:" + tableName + ", tableNum:" + tableNum + ", key:" + tablePk;
        }

    }
}