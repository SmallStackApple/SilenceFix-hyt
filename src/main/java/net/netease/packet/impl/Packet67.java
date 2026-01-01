package net.netease.packet.impl;

import net.minecraft.network.PacketBuffer;
import net.netease.packet.GermPacket;

/**
 * @author ByteBreaker
 * create 29/12/2023
 */
public class Packet67 implements GermPacket {
    private String message;
    private String message2;

    @Override
    public void process() {

    }

    @Override
    public void readPacketData(PacketBuffer packetBuffer) {
        message = packetBuffer.readStringFromBuffer(Short.MAX_VALUE);
        message2 = packetBuffer.readStringFromBuffer(Short.MAX_VALUE);
    }

    @Override
    public int getPacketId() {
        return 67;
    }
}
