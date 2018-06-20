package com.peony.engine.framework.tool.idenum;

import com.peony.engine.framework.security.exception.MMException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jiangmin.wu
 */
public class EnumUtils {

    /**
     * 枚举类对应的map的缓存
     */
    private static Map<Class<?>, Map> enumCacheMap = new ConcurrentHashMap<>();

    /**
     * <p>Gets the {@code Map} of enums by name.</p>
     * <p>
     * <p>This method is useful when you need a map of enums by name.</p>
     *
     * @param <E>       the type of the enumeration
     * @param enumClass the class of the idenum to query, not null
     * @return the modifiable map of idenum names to enums, never null
     */
    public static <E extends Enum<E> & IDEnum> Map<Integer, E> getEnumMap(final Class<E> enumClass) {
        Map resultMap = enumCacheMap.computeIfAbsent(enumClass, (tmpKey) -> {
            final Map<Integer, E> map = new LinkedHashMap<>();
            for (final E e : enumClass.getEnumConstants()) {
                if (map.containsKey(e.getId())) {
                    throw new MMException("enum have repeat id enumClass=" + enumClass.getName());
                }
                map.put(e.getId(), e);
            }
            return map;
        });
        return resultMap;
    }


    /**
     * <p>Gets the {@code List} of enums.</p>
     * <p>
     * <p>This method is useful when you need a list of enums rather than an array.</p>
     *
     * @param <E>       the type of the enumeration
     * @param enumClass the class of the idenum to query, not null
     * @return the modifiable list of enums, never null
     */
    public static <E extends Enum<E> & IDEnum> List<E> getEnumList(final Class<E> enumClass) {
        return new ArrayList<>(Arrays.asList(enumClass.getEnumConstants()));
    }

    /**
     * <p>Checks if the specified name is a valid idenum for the class.</p>
     * <p>
     * <p>This method differs from {@link Enum#valueOf} in that checks if the name is
     * a valid idenum without needing to catch the exception.</p>
     *
     * @param <E>       the type of the enumeration
     * @param enumClass the class of the idenum to query, not null
     * @param enumValue the idenum name, null returns false
     * @return true if the idenum name is valid, otherwise false
     */
    public static <E extends Enum<E> & IDEnum> boolean isValidEnum(final Class<E> enumClass, final int enumValue) {
        return getEnumMap(enumClass).containsKey(enumValue);
    }

    /**
     * <p>Gets the idenum for the class, returning {@code null} if not found.</p>
     * <p>
     * <p>This method differs from {@link Enum#valueOf} in that it does not throw an exception
     * for an invalid idenum name.</p>
     *
     * @param <E>       the type of the enumeration
     * @param enumClass the class of the idenum to query, not null
     * @param enumValue the idenum value, null returns null
     * @return the idenum, null if not found
     */
    public static <E extends Enum<E> & IDEnum> E getEnum(final Class<E> enumClass, final int enumValue) {
        return getEnumMap(enumClass).get(enumValue);
    }
}
