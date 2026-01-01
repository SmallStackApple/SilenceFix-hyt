package dev.xinxin.config;

import com.google.gson.JsonObject;

public abstract class Config {
    private final String name;

    public Config(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public abstract JsonObject saveConfig();

    public abstract void loadConfig(JsonObject var1);
}

