package net.netease.packet.impl;

import net.minecraft.network.PacketBuffer;
import net.netease.PacketProcessor;
import net.netease.gui.GermGameGui;
import net.netease.packet.GermPacket;

import static dev.xinxin.utils.misc.MinecraftInstance.mc;


/**
 * @author ByteBreaker
 * create 28/12/2023
 */
public class Packet76 implements GermPacket {
    private String name;

    @Override
    public void process() {
        PacketProcessor.INSTANCE.sendPacket(new Packet04("germ_gui_loading"));
        PacketProcessor.INSTANCE.sendPacket(new Packet04(name));
        if (name.equals("mainmenu")) {
            GermGameGui.INSTANCE.setGuiName(name);
            mc.addScheduledTask(() -> {
                mc.displayGuiScreen(GermGameGui.INSTANCE);
            });
        }
    }

    @Override
    public void readPacketData(PacketBuffer packetBuffer) {
        name = packetBuffer.readStringFromBuffer(Short.MAX_VALUE);
    }

    @Override
    public int getPacketId() {
        return 76;
    }
}
