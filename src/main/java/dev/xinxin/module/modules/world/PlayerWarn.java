package dev.xinxin.module.modules.world;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventPacketSend;
import dev.xinxin.event.world.EventTick;
import dev.xinxin.event.world.EventWorldLoad;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.modules.misc.Teams;
import dev.xinxin.utils.HYTUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S02PacketChat;

import java.util.ArrayList;
import java.util.List;


public class PlayerWarn extends Module {
    public static List<Entity> flaggedEntity = new ArrayList<>();
    public static int banned = 0;

    public PlayerWarn() {
        super("PlayerTracker",  Category.Misc,"检测封号");
    }

    @EventTarget
    public void onWorld(EventWorldLoad e) {
        flaggedEntity.clear();
    }

    @EventTarget
    public void onPacket(EventPacketSend e) {
        if (e.getPacket() instanceof S02PacketChat packetChat) {
            String text = packetChat.chatComponent.getUnformattedText();
            if (text.contains("何树友") && text.contains("在本局游戏中行为异常")) {
                banned++;
            }
        }
    }

    @EventTarget
    public void onTick(EventTick e) {
        if (mc.theWorld != null && !mc.theWorld.loadedEntityList.isEmpty()) {
            if (!HYTUtils.isInLobby()) {
                if (mc.thePlayer.ticksExisted % 6 == 0) {
                    for (Entity ent : mc.theWorld.loadedEntityList) {
                        if (ent instanceof EntityPlayer) {
                            EntityPlayer player = (EntityPlayer)ent;
                            if (ent != mc.thePlayer) {
                                if (HYTUtils.isStrength(player) > 0 && !flaggedEntity.contains(player) && !Teams.isSameTeam(player)) {
                                    flaggedEntity.add(player);
                                }

                                if (HYTUtils.isRegen(player) > 0 && !flaggedEntity.contains(player) && !Teams.isSameTeam(player)) {
                                    flaggedEntity.add(player);
                                }

                                if (HYTUtils.isHoldingGodAxe(player) && !flaggedEntity.contains(player) && !Teams.isSameTeam(player)) {
                                    flaggedEntity.add(player);
                                }

                                if (HYTUtils.isKBBall(player.getHeldItem()) && !flaggedEntity.contains(player) && !Teams.isSameTeam(player)) {
                                    flaggedEntity.add(player);
                                }

                                if (HYTUtils.hasEatenGoldenApple(player) > 0 && !flaggedEntity.contains(player) && !Teams.isSameTeam(player)) {
                                    flaggedEntity.add(player);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
