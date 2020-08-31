package com.peony.demo.config.core;

/**
 * Created by gaoyun on 2017/9/7.
 *
 * 用于处理配置中有区间概念,并且区间有对应值的元素。比如1-100;10.代表[1,100]区间中的key,对应的value是10
 */
public class RangeValueEntry<T> {
    //
    private RangeEntry rangeEntry;
    //
    private T value;

    public RangeValueEntry(int min, int max, T value) {
        this.rangeEntry = new RangeEntry(min, max);
        this.value = value;
    }

    public int getMin() {
        return rangeEntry.getMin();
    }

    public int getMax() {
        return rangeEntry.getMax();
    }

    public T getValue() {
        return value;
    }

    public boolean isInRange(int key) {
        return rangeEntry.isInRange(key);
    }
}
