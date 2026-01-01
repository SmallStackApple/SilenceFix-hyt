package net.netease.packet.impl;

import net.netease.PacketProcessor;
import net.netease.packet.GermPacket;

/**
 * @author ByteBreaker
 * create 28/12/2023
 */
public class Packet72 implements GermPacket {
    public static String getRandomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; ++i) {
            int number = (int) (Math.random() * str.length());
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }

    @Override
    public void process() {
        if (Packet731.flag) {
            PacketProcessor.INSTANCE.sendPacket(new Packet16("3.4.2", getRandomString(20)));
            Packet731.flag = false;
        }
    }

    @Override
    public int getPacketId() {
        return 72;
    }
}
