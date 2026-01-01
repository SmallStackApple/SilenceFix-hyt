package dev.xinxin.utils.component;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventPacketReceive;
import dev.xinxin.event.world.EventPacketSend;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;

import static dev.xinxin.utils.misc.MinecraftInstance.mc;

public final class BadPacketsComponent {
    private static boolean slot;
    private static boolean attack;
    private static boolean swing;
    private static boolean block;
    private static boolean inventory;
    public int playerSlot = -1;
    public int serverSlot = -1;

    public static boolean bad() {
        return BadPacketsComponent.bad(true, true, true, true, true);
    }

    public static boolean bad(boolean slot, boolean attack, boolean swing, boolean block, boolean inventory) {
        return BadPacketsComponent.slot && slot || BadPacketsComponent.attack && attack || BadPacketsComponent.swing && swing || BadPacketsComponent.block && block || BadPacketsComponent.inventory && inventory;
    }


    @EventTarget
    public void onReceivePacket(EventPacketReceive e) {
        Packet<?> packet = e.getPacket();
        if (packet instanceof S09PacketHeldItemChange) {
            S09PacketHeldItemChange packet2 = (S09PacketHeldItemChange)packet;
            if (packet2.getHeldItemHotbarIndex() >= 0 && packet2.getHeldItemHotbarIndex() < InventoryPlayer.getHotbarSize()) {
                this.serverSlot = packet2.getHeldItemHotbarIndex();
            }
        } else if (e.getPacket() instanceof S0CPacketSpawnPlayer &&mc.thePlayer != null) {
            if (((S0CPacketSpawnPlayer)e.getPacket()).getEntityID() != mc.thePlayer.getEntityId()) {
                return;
            }
            this.playerSlot = -1;
        }
    }
    @EventTarget(value=4)
    public void onPacketSend(EventPacketSend event) {
        Packet packet = event.getPacket();
        if (packet instanceof C09PacketHeldItemChange) {
            slot = true;
        } else if (packet instanceof C0APacketAnimation) {
            swing = true;
        } else if (packet instanceof C02PacketUseEntity) {
            attack = true;
        } else if (packet instanceof C08PacketPlayerBlockPlacement || packet instanceof C07PacketPlayerDigging) {
            block = true;
        } else if (packet instanceof C0EPacketClickWindow || packet instanceof C16PacketClientStatus && ((C16PacketClientStatus)packet).getStatus() == C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT || packet instanceof C0DPacketCloseWindow) {
            inventory = true;
        } else if (packet instanceof C03PacketPlayer) {
            BadPacketsComponent.reset();
        }
    }

    public static void reset() {
        slot = false;
        swing = false;
        attack = false;
        block = false;
        inventory = false;
    }
}

