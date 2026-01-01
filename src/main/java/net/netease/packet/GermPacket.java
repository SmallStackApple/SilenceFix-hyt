package net.netease.packet;

import net.minecraft.network.PacketBuffer;

/**
 * @author ByteBreaker
 * create 28/12/2023
 */
public interface GermPacket {
    default void process() {}
    default void writePacketData(PacketBuffer packetBuffer) {}
    default void readPacketData(PacketBuffer packetBuffer) {}
    int getPacketId();
}
