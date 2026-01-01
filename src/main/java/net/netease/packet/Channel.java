package net.netease.packet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;

import static dev.xinxin.utils.misc.MinecraftInstance.mc;


/**
 * @author ByteBreaker
 * create 29/12/2023
 */


@AllArgsConstructor
@Getter
public class Channel {
    private final String name;

    public void sendToServer(String name, PacketBuffer buffer) {
        mc.thePlayer.sendQueue.addToSendQueue(new C17PacketCustomPayload(name, buffer));
    }
}
