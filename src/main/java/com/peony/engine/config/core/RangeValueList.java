package com.peony.engine.config.core;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Created by gaoyun on 2017/9/7.
 *
 * 每个元素都是带有区间概念的list
 */
public class RangeValueList<T> {
    private List<RangeValueEntry<T>> list;

    public RangeValueList(@Nonnull List<RangeValueEntry<T>> list) {
        this.list = list;
    }

    /**
     * 取key所在区间的rangeEntry
     * @param key
     * @return
     */
    public RangeValueEntry<T> getRangeEntry(int key) {
        for (RangeValueEntry entry : list) {
            if (entry.isInRange(key)) {
                return entry;
            }
        }
        return null;
    }

    public T getValue(int key) {
        RangeValueEntry<T> rangeValueEntry = getRangeEntry(key);
        if (rangeValueEntry != null) {
            return rangeValueEntry.getValue();
        }
        return null;
    }

    public int getMaxOfRange() {
        int maxValue = 0;
        for (RangeValueEntry entry : list) {
            maxValue = Math.max(maxValue, entry.getMax());
        }
        return maxValue;
    }

    public RangeValueEntry<T> getRangeEntryByIndex(int index) {
        return list.get(index);
    }

    public int getListSize() {
        return list.size();
    }
}
