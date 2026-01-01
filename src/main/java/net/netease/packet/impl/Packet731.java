package net.netease.packet.impl;

import net.netease.packet.GermPacket;

/**
 * @author ByteBreaker
 * create 28/12/2023
 */
public class Packet731 implements GermPacket {
    public static volatile boolean flag;

    @Override
    public void process() {
        flag = true;
    }

    @Override
    public int getPacketId() {
        return 731;
    }
}
