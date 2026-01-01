package dev.xinxin.api.netease.receive;

import com.google.gson.annotations.SerializedName;

public class CreateCharacterResponse extends MessageResponse {
    @SerializedName("details")
    public String details;
    @SerializedName("entity")
    public GameCharactersResponse.Character entity;
}
