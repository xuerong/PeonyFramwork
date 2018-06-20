package com.peony.engine.config.core;

/**
 * Created by jiangmin.wu on 2017/12/24.
 */
public class Tuple2<F, S> {
    private F first;
    private S second;

    public Tuple2(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public F getFirst() {
        return first;
    }

    public S getSecond() {
        return second;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Tuple{");
        sb.append("first=").append(first);
        sb.append(", second=").append(second);
        sb.append('}');
        return sb.toString();
    }
}
