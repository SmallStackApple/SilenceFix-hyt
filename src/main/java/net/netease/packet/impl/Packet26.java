package net.netease.packet.impl;

import lombok.AllArgsConstructor;
import net.minecraft.network.PacketBuffer;
import net.netease.packet.GermPacket;

/**
 * @author ByteBreaker
 * create 29/12/2023
 */
@AllArgsConstructor
public class Packet26 implements GermPacket {
    private String string;
    private String json;

    public Packet26() {
    }

    @Override
    public void writePacketData(PacketBuffer packetBuffer) {
        packetBuffer.writeString(string);
        packetBuffer.writeString(json);
    }

    @Override
    public int getPacketId() {
        return 26;
    }
}
