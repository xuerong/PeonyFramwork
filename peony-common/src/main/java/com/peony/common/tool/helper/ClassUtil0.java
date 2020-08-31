package com.peony.common.tool.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 用于获取指定包名下的所有类名.<br/>
 * 并可设置是否遍历该包名下的子包的类名.<br/>
 * 并可通过Annotation(内注)来过滤，避免一些内部类的干扰.<br/>
 *
 * @author jiangmin.wu
 */
public class ClassUtil0 {
    private static Logger logger = LoggerFactory.getLogger(ClassUtil0.class);
    private static ThreadLocal<ClassLoader> parentLoader = new ThreadLocal<>();

    public static final long MAX_SCAN_MILLIS = 100L;


    /**
     * 拿到class下面的所有实现接口
     *
     * @param tClass
     * @return
     */
    public static Set<Class<?>> getInterfacesByClass(Class<?> tClass) {
        Set<Class<?>> classSet = new HashSet<>();
        while (tClass != null) {
            for (Class<?> interfaceClass : tClass.getInterfaces()) {
                classSet.add(interfaceClass);
            }
            tClass = tClass.getSuperclass();
        }
        return classSet;
    }

    /**
     * 扫描指定包下的所有 class
     *
     * @param pkgName     包路径
     * @param isRecursive 是否递归扫描
     * @param superClass  超类或者Class注解
     * @param classLoader 类加载器
     * @return
     */
    public static List<Class<?>> getClassList(String pkgName, boolean isRecursive, Class<?> superClass, ClassLoader classLoader) {
        long start = System.currentTimeMillis();
        List<Class<?>> classList = new ArrayList<>();
        ClassLoader loader = classLoader == null ? Thread.currentThread().getContextClassLoader() : classLoader;
        try {
            if (classLoader != null) {
                parentLoader.set(classLoader);
            }
            // 按文件的形式去查找
            String strFile = pkgName.replaceAll("\\.", "/");
            Enumeration<URL> urls = loader.getResources(strFile);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                if (url != null) {
                    String protocol = url.getProtocol();
                    String pkgPath = url.getPath();
                    logger.debug("protocol:" + protocol + " path:" + pkgPath);
                    if ("file".equals(protocol)) {
                        // 本地自己可见的代码  
                        findClassName(classList, pkgName, pkgPath, isRecursive, superClass);
                    } else if ("jar".equals(protocol)) {
                        // 引用第三方jar的代码  
                        findClassName(classList, pkgName, url, isRecursive, superClass);
                    }
                }
            }
        } catch (IOException e) {
            logger.error(String.format("Scan Class error, pkgName %s, superClass %s, recursive %s", pkgName, superClass, isRecursive), e);
            throw new RuntimeException(e);
        } finally {
            parentLoader.set(null);
        }

        long useMillis = System.currentTimeMillis() - start;
        if (useMillis > MAX_SCAN_MILLIS) {
            logger.info("slow scan class use {} ms [{} {}]", useMillis, pkgName, superClass);
        }

        return classList;
    }

    static void findClassName(List<Class<?>> clazzList, String pkgName, String pkgPath, boolean isRecursive, Class<?> superClass) {
        if (clazzList == null) {
            return;
        }
        File[] files = filterClassFiles(pkgPath);// 过滤出.class文件及文件夹
        logger.debug("files:" + ((files == null) ? "null" : "length=" + files.length));
        if (files != null) {
            for (File f : files) {
                String fileName = f.getName();
                if (f.isFile()) {
                    // .class 文件的情况
                    String clazzName = getClassName(pkgName, fileName);
                    addClassName(clazzList, clazzName, superClass);
                } else {
                    // 文件夹的情况
                    if (isRecursive) {
                        // 需要继续查找该文件夹/包名下的类
                        String subPkgName = pkgName + "." + fileName;
                        String subPkgPath = pkgPath + "/" + fileName;
                        findClassName(clazzList, subPkgName, subPkgPath, true, superClass);
                    }
                }
            }
        }
    }

    /**
     * 第三方Jar类库的引用。<br/>
     *
     * @throws IOException
     */
    static void findClassName(List<Class<?>> clazzList, String pkgName, URL url, boolean isRecursive, Class<?> superClass) throws IOException {
        JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
        JarFile jarFile = jarURLConnection.getJarFile();
        logger.debug("jarFile:" + jarFile.getName());
        Enumeration<JarEntry> jarEntries = jarFile.entries();
        while (jarEntries.hasMoreElements()) {
            JarEntry jarEntry = jarEntries.nextElement();
            String jarEntryName = jarEntry.getName(); // 类似：sun/security/internal/interfaces/TlsMasterSecret.class  
            String clazzName = jarEntryName.replace("/", ".");
            int endIndex = clazzName.lastIndexOf(".");
            String prefix = null;
            if (endIndex > 0) {
                String prefix_name = clazzName.substring(0, endIndex);
                endIndex = prefix_name.lastIndexOf(".");
                if (endIndex > 0) {
                    prefix = prefix_name.substring(0, endIndex);
                }
            }
            if (prefix != null && jarEntryName.endsWith(".class")) {
                if (prefix.equals(pkgName)) {
                    logger.debug("jar entryName:" + jarEntryName);
                    addClassName(clazzList, clazzName, superClass);
                } else if (isRecursive && prefix.startsWith(pkgName)) {
                    // 遍历子包名：子类  
                    logger.debug("jar entryName:" + jarEntryName + " isRecursive:" + isRecursive);
                    addClassName(clazzList, clazzName, superClass);
                }
            }
        }
    }

    private static File[] filterClassFiles(String pkgPath) {
        if (pkgPath == null) {
            return null;
        }
        // 接收 .class 文件 或 类文件夹  
        return new File(pkgPath).listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return (file.isFile() && file.getName().endsWith(".class")) || file.isDirectory();
            }
        });
    }

    private static String getClassName(String pkgName, String fileName) {
        int endIndex = fileName.lastIndexOf(".");
        String clazz = null;
        if (endIndex >= 0) {
            clazz = fileName.substring(0, endIndex);
        }
        String clazzName = null;
        if (clazz != null) {
            clazzName = pkgName + "." + clazz;
        }
        return clazzName;
    }

    private static void addClassName(List<Class<?>> clazzList, String clazzName, Class<?> superClass) {
        if (clazzList != null && clazzName != null) {
            if (clazzName.endsWith(".class")) {
                clazzName = clazzName.substring(0, clazzName.length() - 6);
            }
            Class<?> clazz = null;
            try {
                ClassLoader clazzLoader;
                if (parentLoader.get() != null) {
                    clazzLoader = parentLoader.get();
                } else {
                    clazzLoader = Thread.currentThread().getContextClassLoader();
                }
                clazz = clazzLoader.loadClass(clazzName);
            } catch (ClassNotFoundException e) {
            }
            if (clazz != null) {
                if (superClass == null) {
                    clazzList.add(clazz);
                    logger.debug("add:" + clazz);
                } else if (Annotation.class.isAssignableFrom(superClass) && clazz.isAnnotationPresent((Class<Annotation>) superClass)) {
                    clazzList.add(clazz);
                    logger.debug("add superClass:" + clazz);
                } else if (superClass.isAssignableFrom(clazz) && clazz != superClass) {
                    clazzList.add(clazz);
                    logger.debug("add subclass:" + clazz);
                }
            }
        }
    }
} 