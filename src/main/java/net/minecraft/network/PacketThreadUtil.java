package net.minecraft.network;

import dev.xinxin.event.EventManager;
import dev.xinxin.event.world.EventPacketReceive;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.src.Config;
import net.minecraft.util.IThreadListener;

public class PacketThreadUtil {
    public static int lastDimensionId = Integer.MIN_VALUE;

    public static <T extends INetHandler> boolean checkThreadAndEnqueue(final Packet<T> p_180031_0_, final T p_180031_1_, IThreadListener p_180031_2_) throws ThreadQuickExitException {
        if (!p_180031_2_.isCallingFromMinecraftThread())
        {
            p_180031_2_.addScheduledTask(new Runnable()
            {
                public void run()
                {
                    PacketThreadUtil.clientPreProcessPacket(p_180031_0_);
                    p_180031_0_.processPacket(p_180031_1_);
                }
            });
            throw ThreadQuickExitException.INSTANCE;
        }
        else
        {
            EventPacketReceive packetReceive = new EventPacketReceive(p_180031_0_);
            if(Minecraft.getMinecraft().thePlayer != null) EventManager.call(packetReceive);
            if (packetReceive.isCancelled()) {
                return true;
            }


            clientPreProcessPacket(p_180031_0_);
        }
        return false;
    }

    protected static void clientPreProcessPacket(Packet p_clientPreProcessPacket_0_) {
        if (p_clientPreProcessPacket_0_ instanceof S08PacketPlayerPosLook) {
            Config.getRenderGlobal().onPlayerPositionSet();
        }
        if (p_clientPreProcessPacket_0_ instanceof S07PacketRespawn) {
            S07PacketRespawn s07packetrespawn = (S07PacketRespawn)p_clientPreProcessPacket_0_;
            lastDimensionId = s07packetrespawn.getDimensionID();
        } else if (p_clientPreProcessPacket_0_ instanceof S01PacketJoinGame) {
            S01PacketJoinGame s01packetjoingame = (S01PacketJoinGame)p_clientPreProcessPacket_0_;
            lastDimensionId = s01packetjoingame.getDimension();
        } else {
            lastDimensionId = Integer.MIN_VALUE;
        }
    }
}

