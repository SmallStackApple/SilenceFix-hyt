package dev.yalan.live.silencefix;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class LiveClientSetting {
    @SerializedName("freeVersion")
    public String freeVersion;

    @SerializedName("paidVersion")
    public String paidVersion;

    @SerializedName("navenVersion")
    public String navenVersion;

    @SerializedName("allowFreeAttackFree")
    public boolean allowFreeAttackFree;

    @SerializedName("neteaseFree")
    public boolean neteaseFree;

    @SerializedName("neteaseMinimumLevel")
    public String neteaseMinimumLevel;

    @SerializedName("neteasePeriodDisabled")
    public boolean neteasePeriodDisabled;

    @SerializedName("neteaseStartHour")
    public int neteaseStartHour;

    @SerializedName("neteaseEndHour")
    public int neteaseEndHour;

    @SerializedName("clientWhitelist")
    public List<String> clientWhitelist;
}