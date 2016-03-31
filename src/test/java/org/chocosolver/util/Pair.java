package org.chocosolver.util;

/**
 * @author Моклев Вячеслав
 */
public class Pair<T, V> {
    private T a;
    private V b;

    public Pair(T a, V b) {
        this.a = a;
        this.b = b;
    }

    public T getA() {
        return a;
    }

    public V getB() {
        return b;
    }
}
