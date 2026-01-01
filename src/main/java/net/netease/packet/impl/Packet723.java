package net.netease.packet.impl;

import net.minecraft.network.PacketBuffer;
import net.netease.PacketProcessor;
import net.netease.packet.GermPacket;

/**
 * @author ByteBreaker
 * create 01/02/2024
 */

public class Packet723 implements GermPacket {
    private int key;

    @Override
    public void process() {
        PacketProcessor.INSTANCE.getOutstandingKeys().add(key);
    }

    @Override
    public void writePacketData(PacketBuffer packetBuffer) {
        packetBuffer.writeInt(key);
    }

    @Override
    public void readPacketData(PacketBuffer packetBuffer) {
        key = packetBuffer.readInt();
    }

    @Override
    public int getPacketId() {
        return 723;
    }
}
