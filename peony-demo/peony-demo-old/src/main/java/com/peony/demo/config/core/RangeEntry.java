package com.peony.demo.config.core;

/**
 * Created by gaoyun on 2017/11/18.
 *
 * 用于处理配置中有区间概念的元素。比如1-100.代表[1,100]区间中的key的集合
 */
public class RangeEntry {
    //
    private int min;
    //
    private int max;

    public RangeEntry(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public boolean isInRange(int key) {
        return key >= min && key <= max;
    }
}
