package dev.xinxin.module.values;

import lombok.Getter;

public abstract class Value<V> {
    public V value;
    String name;
    @Getter
    public float height = 22f;
    protected final Dependency dependency;
    public float animation = 0f;

    public Value(String name, Dependency dependenc) {
        this.name = name;
        this.dependency = dependenc;
    }

    public Value(String name) {
        this.name = name;
        this.dependency = () -> Boolean.TRUE;
    }

    public String getName() {
        return this.name;
    }

    public V getValue() {
        return this.value;
    }

    public void setValue(V val) {
        this.value = val;
    }
    public abstract <T> T getConfigValue();

    public boolean isAvailable() {
        return this.dependency != null && this.dependency.check();
    }
    public boolean isHidden() {
        return !isAvailable();
    }
    @FunctionalInterface
    public static interface Dependency {
        public boolean check();
    }
}

