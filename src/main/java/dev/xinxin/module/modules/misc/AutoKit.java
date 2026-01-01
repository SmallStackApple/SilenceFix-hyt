package dev.xinxin.module.modules.misc;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventPacketReceive;
import dev.xinxin.event.world.EventWorldLoad;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.values.NumberValue;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.network.play.server.S30PacketWindowItems;

public class AutoKit extends Module {
    public AutoKit() {
        super("AutoKit", Category.Misc, "自动职业");
    }

    public final NumberValue select = new NumberValue("Select Slot", 6, 0, 7, 1);
    private SelectStatus status = SelectStatus.IDLE;

    @Override
    public void onEnable() {
        this.status = SelectStatus.IDLE;
    }

    @EventTarget
    public void onWorldLoad(EventWorldLoad event) {
        this.status = SelectStatus.IDLE;
    }

    @EventTarget
    public void onPacket(EventPacketReceive event) {
        if (mc == null || mc.thePlayer == null || mc.playerController == null) {
            return;
        }

        if (!SelectStatus.DONE.equals(this.status)) {
            Packet<?> packet = event.getPacket();
            if (packet instanceof S2FPacketSetSlot) {
                S2FPacketSetSlot setSlot = (S2FPacketSetSlot) packet;
                if (setSlot.func_149174_e() != null && setSlot.func_149174_e().getItem() != null) {
                    if (SelectStatus.IDLE.equals(this.status)) {
                        int slotId = setSlot.func_149173_d();
                        if (slotId >= 36 && slotId <= 44) {
                            ItemStack itemStack = setSlot.func_149174_e();
                            if (itemStack.getItem().equals(Items.ender_eye)) {
                                int slot = slotId - 36;
                                if (mc.thePlayer.inventory.currentItem != slot) {
                                    mc.thePlayer.inventory.currentItem = slot;
                                    mc.playerController.updateController();
                                }
                                if (mc.gameSettings != null) {
                                    mc.rightClickMouse();
                                }
                                this.status = SelectStatus.WAITING_OPEN;
                            }
                        }
                    }
                }
            }
            if (packet instanceof S30PacketWindowItems) {
                S30PacketWindowItems windowItems = (S30PacketWindowItems) packet;
                ItemStack[] itemStacks = windowItems.getItemStacks();

                if (itemStacks != null) {
                    if (SelectStatus.IDLE.equals(this.status) && windowItems.func_148911_c() == 0) {
                        for (int i = 36; i <= 44 && i < itemStacks.length; i++) {
                            ItemStack itemStack = itemStacks[i];
                            if (itemStack != null && itemStack.getItem() != null &&
                                    itemStack.getItem().equals(Items.ender_eye)) {
                                int slot = i - 36;
                                if (mc.thePlayer.inventory.currentItem != slot) {
                                    mc.thePlayer.inventory.currentItem = slot;
                                    mc.playerController.updateController();
                                }
                                if (mc.gameSettings != null) {
                                    mc.rightClickMouse();
                                }
                                this.status = SelectStatus.WAITING_OPEN;
                                break;
                            }
                        }
                    } else if (SelectStatus.WAITING_ITEMS.equals(status) && windowItems.func_148911_c() != 0) {
                        for (int i = 0; i < itemStacks.length; i++) {
                            ItemStack itemStack = itemStacks[i];
                            if (itemStack != null && i == select.getValue().intValue()) {
                                mc.playerController.windowClick(
                                        windowItems.func_148911_c(),
                                        i,
                                        0,
                                        0,
                                        mc.thePlayer
                                );
                                event.setCancelled();
                                break;
                            }
                        }
                    }
                }
            }
            if (packet instanceof S2DPacketOpenWindow) {
                S2DPacketOpenWindow openWindow = (S2DPacketOpenWindow) packet;
                if (openWindow.getWindowTitle() != null &&
                        openWindow.getWindowTitle().getFormattedText().equals("§5选择你的职业§r")) {
                    this.status = SelectStatus.WAITING_ITEMS;
                    event.setCancelled();
                }
            }

        }
    }

    enum SelectStatus {
        IDLE,
        WAITING_OPEN,
        WAITING_ITEMS,
        DONE
    }
}