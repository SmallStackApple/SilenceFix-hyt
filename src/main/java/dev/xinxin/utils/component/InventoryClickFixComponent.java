package dev.xinxin.utils.component;

import com.xinxin.client.viaversion.vialoadingbase.ViaLoadingBase;
import dev.xinxin.SilenceFix;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventPacketSend;
import dev.xinxin.utils.client.PacketUtil;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0EPacketClickWindow;

public class InventoryClickFixComponent {
    @EventTarget
    public void onPacketSend(EventPacketSend event) {
        if (ViaLoadingBase.getInstance().getTargetVersion().getVersion() > 47 && (event.getPacket() instanceof C0EPacketClickWindow || event.getPacket() instanceof C0BPacketEntityAction || event.getPacket() instanceof C08PacketPlayerBlockPlacement) && (SilenceFix.mc.currentScreen instanceof GuiChest || SilenceFix.mc.currentScreen instanceof GuiInventory)) {
            PacketUtil.sendPacketC0F();
        }
    }
}

