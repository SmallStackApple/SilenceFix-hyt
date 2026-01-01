package dev.yalan.live.silencefix;

import com.google.gson.annotations.SerializedName;

public class NeteaseApiResult {
    @SerializedName("Code")
    private final int code;

    @SerializedName("Message")
    private final String message;

    public NeteaseApiResult(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
