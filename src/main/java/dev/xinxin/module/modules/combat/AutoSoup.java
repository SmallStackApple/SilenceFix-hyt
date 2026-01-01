package dev.xinxin.module.modules.combat;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventMotion;
import dev.xinxin.event.world.EventUpdate;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.values.BoolValue;
import dev.xinxin.module.values.NumberValue;
import dev.xinxin.utils.client.PacketUtil;
import dev.xinxin.utils.client.TimeUtil;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.*;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import org.apache.commons.lang3.RandomUtils;

public class AutoSoup extends Module {
    private final NumberValue health = new NumberValue("Health", 15.0, 0.0, 20.0, 1.0);
    private final NumberValue minDelay = new NumberValue("Min Delay", 300.0, 0.0, 1000.0, 1.0);
    private final NumberValue maxDelay = new NumberValue("Max Delay", 500.0, 0.0, 1000.0, 1.0);
    private final BoolValue dropBowl = new BoolValue("Drop Bowl", true);
    private final BoolValue Legit = new BoolValue("Legit", false);
    private final TimeUtil timer = new TimeUtil();
    private boolean switchBack;
    private long decidedTimer;
    private int soup = -1;
    private int prevslot = 0;
    private boolean movingSoup;

    public AutoSoup() {
        super("AutoSoup", Category.Combat, "自动蘑菇饱");
    }

    @Override
    public void onDisable() {
        this.switchBack = false;
        this.soup = -1;
        this.movingSoup = false;
    }

    @EventTarget
    public void onUpdate(EventUpdate eventUpdate) {
        if (!movingSoup && getEmptyHotbarSlot() != -1) {
            int soupInBackpack = findSoupInBackpack();
            if (soupInBackpack != -1) {
                movingSoup = true;
                moveSoupToHotbar(soupInBackpack);
            }
        }
    }

    @EventTarget
    public void onMotion(EventUpdate event) {
            if (this.switchBack) {
                handleSwitchBack();
                return;
            }
            if (!movingSoup && getEmptyHotbarSlot() != -1) {
                int soupInBackpack = findSoupInBackpack();
                if (soupInBackpack != -1) {
                    movingSoup = true;
                    moveSoupToHotbar(soupInBackpack);
                }
            }

            if (shouldDrinkSoup()) {
                handleSoupDrinking();
            }
    }


    private boolean shouldDrinkSoup() {
        return this.timer.hasPassed(this.decidedTimer) &&
                mc.thePlayer.ticksExisted > 10 &&
                mc.thePlayer.getHealth() < (float) this.health.getValue().intValue();
    }

    private void handleSwitchBack() {
        if (this.Legit.getValue()) {
            mc.playerController.updateController();
        } else {
            mc.thePlayer.inventory.currentItem = prevslot;
        }

        this.switchBack = false;
        this.movingSoup = false;
    }

    private void handleSoupDrinking() {
        this.soup = findSoupInHotbar();

        if (this.soup != -1) {
            drinkSoup();
            if (!movingSoup && getEmptyHotbarSlot() != -1) {
                int soupInBackpack = findSoupInBackpack();
                if (soupInBackpack != -1) {
                    movingSoup = true;
                    moveSoupToHotbar(soupInBackpack);
                }
            }
        } else {
            tryMoveSoupFromBackpack();
        }

        resetTimer();
    }


    private void drinkSoup() {
        if (this.Legit.getValue()) {
            mc.thePlayer.inventory.currentItem = this.soup;
            mc.gameSettings.keyBindUseItem.setPressed(true);
        } else {
            prevslot = mc.thePlayer.inventory.currentItem;
            mc.thePlayer.inventory.currentItem = this.soup;

            mc.playerController.sendUseItem(mc.thePlayer,mc.theWorld,mc.thePlayer.inventory.getCurrentItem());
            if (this.dropBowl.getValue()) {
                PacketUtil.send(new C07PacketPlayerDigging(
                        C07PacketPlayerDigging.Action.DROP_ITEM,
                        new BlockPos(-1,-1,-1),
                        EnumFacing.DOWN
                ));
            }
        }

        this.switchBack = true;
    }


    private void tryMoveSoupFromBackpack() {
        int soupInBackpack = findSoupInBackpack();
        if (soupInBackpack != -1 && getEmptyHotbarSlot() != -1) {
            movingSoup = true;
            moveSoupToHotbar(soupInBackpack);
        }
    }

    private void resetTimer() {
        int delayFirst = (int) Math.floor(Math.min(this.minDelay.getValue(), this.maxDelay.getValue()));
        int delaySecond = (int) Math.ceil(Math.max(this.minDelay.getValue(), this.maxDelay.getValue()));
        this.decidedTimer = RandomUtils.nextInt(delayFirst, delaySecond);
        this.timer.reset();
    }

    private int findSoupInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() == Items.mushroom_stew) {
                return i;
            }
        }
        return -1;
    }

    private int findSoupInBackpack() {
        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() == Items.mushroom_stew) {
                return i;
            }
        }
        return -1;
    }

    private int getEmptyHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.thePlayer.inventory.getStackInSlot(i) == null) {
                return i;
            }
        }
        return -1;
    }

    private void moveSoupToHotbar(int sourceSlot) {
        boolean needOpenInventory = !(mc.currentScreen instanceof GuiInventory);

        if (needOpenInventory) {
            mc.thePlayer.sendQueue.addToSendQueue(
                    new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
        }

        int emptySlot = getEmptyHotbarSlot();
        if (emptySlot != -1) {
            mc.playerController.windowClick(0, sourceSlot, 0, 1, mc.thePlayer);
            mc.playerController.windowClick(0, emptySlot + 36, 0, 1, mc.thePlayer);
        }

        if (needOpenInventory) {
            mc.thePlayer.sendQueue.addToSendQueue(new C0DPacketCloseWindow());
        }
    }
}