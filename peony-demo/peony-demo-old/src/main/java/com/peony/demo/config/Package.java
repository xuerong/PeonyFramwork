package com.peony.demo.config;

/**
 * ConfigMgr运行机制:
 * <p>
 * 配置文件加载验证 ConfigService#load()
 * 自动加载 ConfigService#init()
 * 获取配置文件 ConfigService#getContainer(java.lang.Class)
 * <p>
 * <p>
 * 增加数据类型:com.peony.demo.config.core.field.impl 包下增加相应的 IFieldType 实现类
 * 增加验证类型:com.peony.demo.config.core.verify.impl 包下增加相应的 IVerify 实现类
 * <p>
 * Created by jiangmin.wu on 2018/3/8.
 */
public class Package {
}
