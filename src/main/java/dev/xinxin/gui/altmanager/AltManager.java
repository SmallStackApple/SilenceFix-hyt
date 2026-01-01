package dev.xinxin.gui.altmanager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.xinxin.api.netease.NeteaseAccount;

import java.util.ArrayList;

public final class AltManager {
    public static AltManager Instance;
    private final ArrayList<NeteaseAccount> altList = new ArrayList();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public AltManager() {
        Instance = this;
    }

    public void addAlt(NeteaseAccount alt) {
        this.altList.add(alt);
    }

    public ArrayList<NeteaseAccount> getAltList() {
        return this.altList;
    }
}