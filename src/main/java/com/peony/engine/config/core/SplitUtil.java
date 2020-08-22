package com.peony.engine.config.core;

import java.util.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

/**
 * 默认严格按照下面分隔符优先级顺序进行字符串分割
 *  |;,:#$
 *
 * @author wjm
 */
public class SplitUtil {
    public static final String SEPARATOR_1 = ";";
    public static final String SEPARATOR_2 = "|";
    public static final String SEPARATOR_3 = ",";
    public static final String SEPARATOR_4 = ":";
    public static final String SEPARATOR_5 = "#";
    public static final String SEPARATOR_6 = "$";

    /**
     * 1|2|3 转成 Integer[]
     *
     * @param str
     * @return
     */
    public static Integer[] splitToInt(String str) {
        return splitToInt(str, SEPARATOR_1);
    }

    public static Integer[] splitToInt(String str, String spStr) {
        if (str == null || str.trim().length() == 0) {
            return new Integer[0];
        }

        try {
            String[] temps = StringUtils.splitByWholeSeparator(str, spStr);
            int len = temps.length;
            Integer[] results = new Integer[len];
            for (int i = 0; i < len; i++) {
                if (temps[i].trim().length() == 0) {
                    continue;
                }
                results[i] = Integer.parseInt(temps[i].trim());
            }
            return results;
        } catch (Exception e) {
            return new Integer[0];
        }
    }

    public static String concatToStr(int[] ints, String spStr) {
        if (ints == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ints.length; i++) {
            sb.append(ints[i]).append(spStr);
        }
        if (sb.length() == 0) {
            return "";
        }
        return sb.substring(0, sb.length() - 1).toString();
    }

    public static void main(String[] str) {
//		print(splitToInt("1"));
//		print(splitToInt("1"));
//		print(splitToInt("1,2,3"));
//		print(splitToInt("1,2,3,"));
//		print(concatToStr(new int[] { 1, 2, 3, 4 }));

        int i = convert(int.class, "123");
        System.err.println(i);
    }

    public static boolean isNumeric(String str) {
        return StringUtils.isNotBlank(str) && StringUtils.isNumeric(str);
    }

    public static int[][] parseIntArray(String src, String separator1, String separator2) {
        String[] strs = StringUtils.splitByWholeSeparator(src, separator1);
        int[][] buffInfos = new int[strs.length][2];
        for (int i = 0; i < strs.length; i++) {
            String[] regs = StringUtils.splitByWholeSeparator(strs[i], separator2);
            buffInfos[i][0] = Integer.parseInt(regs[0]);
            buffInfos[i][1] = Integer.parseInt(regs[1]);
        }
        return buffInfos;
    }

    /**
     * 查分成数组
     *
     * @param content
     * @param separator
     * @return
     */
    public static int[] convertStringToIntArray(String content, String separator) {
        if (StringUtils.isBlank(content)) {
            return new int[0];
        }
        String[] entryArray = StringUtils.splitByWholeSeparator(content, separator);
        int[] ret = new int[entryArray.length];
        int i = 0;
        for (String entry : entryArray) {
            ret[i++] = Integer.parseInt(entry);
        }
        return ret;
    }

    /**
     * 1;1|2;2 类型的串转成map
     *
     * @param
     * @return
     */
    public static <K, V> Map<K, V> convertContentToMap(String content, Class<K> kClass, Class<V> vClass) {
        return convertContentToMap(content, SEPARATOR_2, SEPARATOR_1, kClass, vClass);
    }

    public static <K, V> Map<K, V> convertContentToMap(String content, String separator1, String separator2, Class<K> kClass, Class<V> vClass) {
        Map<K, V> ret = new HashMap<>();
        if (StringUtils.isBlank(content)) {
            return ret;
        }
        String[] entryArray = StringUtils.splitByWholeSeparator(content, separator1);
        if (entryArray != null && entryArray.length != 0) {
            for (String entry : entryArray) {
                String[] keyValueArray = StringUtils.splitByWholeSeparator(entry, separator2);
                if (keyValueArray.length == 2) {
                    ret.put(convert(kClass, keyValueArray[0]), convert(vClass, keyValueArray[1]));
                }
            }
        }
        return ret;
    }


    /**
     * a;1,2,3|b;2,3,4  to  Map<K,List<V>>
     *
     * @param content
     * @param kClass
     * @param vClass
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V> Map<K, List<V>> convertContentToMapList(String content, Class<K> kClass, Class<V> vClass) {
        ImmutableMap<K, List<V>> ret = ImmutableMap.of();
        if (StringUtils.isBlank(content)) {
            return ret;
        }
        Map<K, List<V>> map = new HashMap<>();
        String[] entryArray = StringUtils.splitByWholeSeparator(content, SEPARATOR_2);
        if (entryArray != null && entryArray.length != 0) {
            for (String entry : entryArray) {
                String[] keyValueArray = StringUtils.splitByWholeSeparator(entry, SEPARATOR_1);
                if (keyValueArray.length == 2) {
                    map.put(convert(kClass, keyValueArray[0]), ImmutableList.copyOf(convertContentToList(keyValueArray[1], SEPARATOR_3, vClass)));
                }
            }
        }
        ret = ImmutableMap.copyOf(map);
        return ret;
    }

    public static <K, V> Map<K, V> convertContentToLinkedMap(String content, Class<K> kClass, Class<V> vClass) {
        return convertContentToLinkedMap(content, SEPARATOR_1, SEPARATOR_2, kClass, vClass);
    }

    public static <K, V> Map<K, V> convertContentToLinkedMap(String content, String separator1, String separator2, Class<K> kClass, Class<V> vClass) {
        Map<K, V> ret = new LinkedHashMap<>();
        if (StringUtils.isBlank(content)) {
            return ret;
        }
        String[] entryArray = StringUtils.splitByWholeSeparator(content, separator1);
        for (String entry : entryArray) {
            String[] keyValueArray = StringUtils.splitByWholeSeparator(entry, separator2);
            if (keyValueArray.length != 2) {
                throw new ConfigException("convertContentToMap err content=" + content);
            }
            ret.put(convert(kClass, keyValueArray[0]), convert(vClass, keyValueArray[1]));
        }
        return ret;
    }

    /**
     * 1|2|3 转成list
     *
     * @param content
     * @param kClass
     * @param <K>
     * @return
     */
    public static <K> List<K> convertContentToList(String content, Class<K> kClass) {
        return convertContentToList(content, SEPARATOR_1, kClass);
    }

    public static <K> List<K> convertContentToList(String content, String separator, Class<K> kClass) {
        List<K> ret = new ArrayList<>();
        if (StringUtils.isBlank(content)) {
            return ret;
        }
        String[] entryArray = StringUtils.splitByWholeSeparator(content, separator);
        for (String entry : entryArray) {
            ret.add(convert(kClass, entry));
        }
        return ret;
    }

    public static <K> Set<K> convertContentToSet(String content, Class<K> kClass) {
        return convertContentToSet(content, SEPARATOR_1, kClass);
    }

    public static <K> Set<K> convertContentToSet(String content, String separator, Class<K> kClass) {
        Set<K> set = new HashSet<K>();
        if (StringUtils.isBlank(content)) {
            return set;
        }
        String[] entryArray = StringUtils.splitByWholeSeparator(content, separator);
        for (String entry : entryArray) {
            set.add(convert(kClass, entry));
        }
        return set;
    }


    /**
     * 1|2 转成Tuple2
     *
     * @param content
     * @param firstClass
     * @param secondClass
     * @param <F>
     * @param <S>
     * @return
     */
    public static <F, S> Tuple2<F, S> convertContentToTuple2(String content, Class<F> firstClass, Class<S> secondClass) {
        return convertContentToTuple2(content, SEPARATOR_1, firstClass, secondClass);
    }

    /**
     * 1;2|2;3 转成 List<Tuple2>
     *
     * @param content
     * @param firstClass
     * @param secondClass
     * @param <F>
     * @param <S>
     * @return
     */
    public static <F, S> List<Tuple2<F, S>> convertContentToTuple2List(String content, Class<F> firstClass, Class<S> secondClass) {
        return convertContentToTuple2List(content, SEPARATOR_1, SEPARATOR_2, firstClass, secondClass);
    }

    public static <F, S> List<Tuple2<F, S>> convertContentToTuple2List(String content, String separator1, String separator2, Class<F> firstClass, Class<S> secondClass) {
        List<Tuple2<F, S>> ret = new ArrayList<>();
        if (StringUtils.isBlank(content)) {
            return ret;
        }
        String[] entryArray = StringUtils.splitByWholeSeparator(content, separator1);
        for (String entry : entryArray) {
            ret.add(convertContentToTuple2(entry, separator2, firstClass, secondClass));
        }
        return ret;
    }

    public static <F, S> Tuple2<F, S> convertContentToTuple2(String content, String separator, Class<F> firstClass, Class<S> secondClass) {
        if (StringUtils.isBlank(content)) {
            return null;
        }
        String[] entryArray = StringUtils.splitByWholeSeparator(content, separator);
        return new Tuple2(convert(firstClass, entryArray[0]), convert(secondClass, entryArray[1]));
    }

    /**
     * 1;1;2|2;2;10  转成3元组 List<Tuple3<V1,V2,V3>>
     *
     * @return
     */
    public static <V1, V2, V3> List<Tuple3<V1, V2, V3>> convertContentToTuple3List(String content, Class<V1> classV1, Class<V2> classV2, Class<V3> classV3) {
        return convertContentToTuple3List(content, SEPARATOR_2, SEPARATOR_1, classV1, classV2, classV3);
    }

    public static <V1, V2, V3> List<Tuple3<V1, V2, V3>> convertContentToTuple3List(String content, String separator1, String separator2, Class<V1> classV1, Class<V2> classV2, Class<V3> classV3) {
        List<Tuple3<V1, V2, V3>> ret = new LinkedList<>();
        if (StringUtils.isBlank(content)) {
            return ImmutableList.copyOf(ret);
        }

        String[] entryArray = StringUtils.splitByWholeSeparator(content, separator1);
        for (String entry : entryArray) {
            Tuple3 e = convertContentToTuple3(entry, separator2, classV1, classV2, classV3);
            ret.add(e);
        }

        return ImmutableList.copyOf(ret);
    }

    public static <V1, V2, V3> Tuple3 convertContentToTuple3(String entry, String separator, Class<V1> classV1, Class<V2> classV2, Class<V3> classV3) {
        String[] arr = StringUtils.splitByWholeSeparator(entry, separator);
        return new Tuple3(convert(classV1, arr[0]), convert(classV2, arr[1]), convert(classV3, arr[2]));
    }

    /**
     * 2,3;1|4,5;3 转 rangeList
     *
     * @return
     */
    public static <T> RangeValueList convertContentToRangeList(String content, Class<T> tClass) {
        return convertContentToRangeList(content, SEPARATOR_1, SEPARATOR_2, SEPARATOR_3, tClass);
    }


    public static <T> RangeValueList convertContentToRangeList(String content, String separator1, String separator2, String separator3, Class<T> tClass) {
        List<RangeValueEntry<T>> tempList = Lists.newArrayList();
        String[] rangeEntryStrArray = StringUtils.splitByWholeSeparator(content, separator1);
        for (String rangeEntryStr : rangeEntryStrArray) {
            String[] rangeEntryKeyValuleStrArray = StringUtils.splitByWholeSeparator(rangeEntryStr, separator2);
            String[] rangeEntryKeyStrArray = StringUtils.splitByWholeSeparator(rangeEntryKeyValuleStrArray[0], separator3);
            RangeValueEntry<T> rangeEntry = new RangeValueEntry<T>(Integer.parseInt(rangeEntryKeyStrArray[0]), Integer.parseInt(rangeEntryKeyStrArray[1]),
                    convert(tClass, rangeEntryKeyValuleStrArray[1]));
            tempList.add(rangeEntry);
        }
        return new RangeValueList(tempList);
    }

    public static <K> String convertCollectionToContent(Collection<K> collection) {
        return convertCollectionToContent(collection, SEPARATOR_1);
    }

    public static <K> String convertCollectionToContent(Collection<K> collection, String separator) {
        StringBuilder sb = new StringBuilder("");
        if (collection == null) {
            return sb.toString();
        }
        String spe = "";
        for (K entry : collection) {
            sb.append(spe).append(entry.toString());
            spe = separator;
        }
        return sb.toString();
    }

    /**
     * map转成1;1|2;2类型的串
     *
     * @param map
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V> String convertMapToContent(Map<K, V> map) {
        return convertMapToContent(map, SEPARATOR_1, SEPARATOR_2);
    }


    public static <K, V> String convertMapToContent(Map<K, V> map, String sep1, String sep2) {
        if (map == null) {
            return "";
        }
        String seperator = "";
        StringBuilder strBuilder = new StringBuilder();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            strBuilder.append(seperator).append(entry.getKey()).append(sep2).append(entry.getValue());
            seperator = sep1;
        }
        return strBuilder.toString();

    }

    /**
     * map<K,List<V>>  to  1;1,2,3,4|2;1,2,3
     *
     * @param map
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V> String convertMapListToContent(Map<K, List<V>> map) {
        return convertMapListToContent(map, SEPARATOR_1, SEPARATOR_2, SEPARATOR_3);
    }

    public static <K, V> String convertMapListToContent(Map<K, List<V>> map, String separator1, String separator2, String separator3) {
        StringBuilder sb = new StringBuilder();
        String seperator = "";
        for (Map.Entry<K, List<V>> entry : map.entrySet()) {
            sb.append(seperator).append(entry.getKey()).append(separator2).append(convertCollectionToContent(entry.getValue(), separator3));
            seperator = separator1;
        }
        return sb.toString();
    }


    public static <T> T convert(Class<T> clazz, String content) {
        if (clazz == Integer.class || clazz == int.class) {
            return (T) Integer.valueOf(content);
        } else if (clazz.isAssignableFrom(Long.class) || clazz == long.class) {
            return (T) Long.valueOf(content);
        } else if (clazz.isAssignableFrom(Short.class) || clazz == short.class) {
            return (T) Short.valueOf(content);
        } else if (clazz.isAssignableFrom(Byte.class) || clazz == byte.class) {
            return (T) Byte.valueOf(content);
        } else if (clazz.isAssignableFrom(Boolean.class) || clazz == boolean.class) {
            return (T) Boolean.valueOf(content);
        } else if (clazz.isAssignableFrom(Double.class) || clazz == double.class) {
            return (T) Double.valueOf(content);
        } else if (clazz.isAssignableFrom(Float.class) || clazz == float.class) {
            return (T) Float.valueOf(content);
        } else if (clazz.isAssignableFrom(String.class)) {
            return (T) content;
        } else {
            throw new RuntimeException("不支持的类型");
        }
    }
}
