package com.peony.engine.config.core;

/**
 * Created by jiangmin.wu on 2017/12/24.
 */
public class Tuple3<F, S, T> {
    private F first;
    private S second;
    private T third;

    public Tuple3(F first, S second, T third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public F getFirst() {
        return first;
    }

    public S getSecond() {
        return second;
    }

    public T getThird() {
        return third;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Tuple3{");
        sb.append("first=").append(first);
        sb.append(", second=").append(second);
        sb.append(", third=").append(third);
        sb.append('}');
        return sb.toString();
    }
}
