package com.peony.common.tool;

/**
 * 用于获取名字末尾有摸个字符串的模板类
 *
 * @author huangyong
 * @since 2.3
 */
public abstract class EndWithClassTemplate extends ClassTemplate {

    protected final String endWithString;

    protected EndWithClassTemplate(String packageName, String endWithString) {
        super(packageName);
        this.endWithString = endWithString;
    }
}
