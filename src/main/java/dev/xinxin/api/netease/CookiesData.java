package dev.xinxin.api.netease;

import com.google.gson.annotations.SerializedName;
import dev.xinxin.api.netease.send.SAuth;
import dev.xinxin.api.utils.GsonIgnore;

import static dev.xinxin.api.SilenceAPi.GSON;

public class CookiesData {
    @SerializedName("sauth_json")
    public String sauth_json;

    @GsonIgnore
    public SAuth sauth;

    @GsonIgnore
    public final String cookies;

    public CookiesData(String cookies) {
        this.cookies = cookies;
        CookiesData temp = GSON.fromJson(cookies, CookiesData.class);
        this.sauth_json = temp.sauth_json;
        this.sauth = new SAuth(this.sauth_json);
    }

    @Override
    public String toString() {
        return cookies;
    }
}
