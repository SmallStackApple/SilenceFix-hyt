package net.netease.packet.impl;

import com.google.gson.JsonObject;
import lombok.Getter;
import net.minecraft.network.PacketBuffer;
import net.netease.GsonUtil;
import net.netease.packet.GermPacket;

/**
 * @author ByteBreaker
 * create 29/12/2023
 */

@Getter
public class Packet2141 implements GermPacket {
    private String data;

    public Packet2141() {
    }

    @Override
    public void process() {
        JsonObject object = GsonUtil.fromJson(data);
        if (object.get("hudMsgType").getAsString().equals("CENTER_UP_SCROLL")) {
            int priority = object.get("priority").getAsInt();
            String contents = object.get("contents").getAsString();
        }
    }

    @Override
    public void readPacketData(PacketBuffer packetBuffer) {
        data = packetBuffer.readStringFromBuffer(Short.MAX_VALUE);
    }

    @Override
    public int getPacketId() {
        return 2141;
    }
}
