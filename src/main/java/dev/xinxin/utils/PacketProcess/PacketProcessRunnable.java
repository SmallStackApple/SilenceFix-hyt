package dev.xinxin.utils.PacketProcess;

import net.minecraft.network.Packet;

public abstract class PacketProcessRunnable implements Runnable {
    public final Packet<?> packetToProcess;
    public PacketProcessRunnable(Packet<?> packetToProcess) {
        this.packetToProcess = packetToProcess;
    }
}
