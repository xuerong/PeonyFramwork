package com.peony.engine.framework.tool.idenum;

/**
 * 所有的枚举都有一个id的概念，
 * 以这个id作为唯一标示某个枚举，
 * 最好不要用 ordinal() 作为标示
 *
 * @author jiangmin.wu
 */
public interface IDEnum {
    int getId();
}
