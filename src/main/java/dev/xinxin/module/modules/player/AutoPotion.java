package dev.xinxin.module.modules.player;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventMotion;
import dev.xinxin.event.world.EventUpdate;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.modules.combat.KillAura;
import dev.xinxin.module.modules.misc.AutoGapple;
import dev.xinxin.module.modules.world.ChestAura;
import dev.xinxin.module.values.NumberValue;
import dev.xinxin.utils.RotationComponent;
import dev.xinxin.utils.TimerUtil;
import dev.xinxin.utils.client.PacketUtil;
import dev.xinxin.utils.player.RotationUtil;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.potion.PotionEffect;
import org.lwjgl.util.vector.Vector2f;

public class AutoPotion extends Module {
    public AutoPotion() {
        super("AutoPotion", Category.Player, "自动药水");
    }

    public final NumberValue delayValue = new NumberValue("Delay", 250, 0, 1000, 50);
    public final NumberValue healthValue = new NumberValue("Health", 1, 1, 20, 1);
    public final NumberValue selectValue = new NumberValue("Select Slot", 6, 0, 8, 1);
    public final NumberValue autoMoveValue = new NumberValue("Auto Move", 1, 0, 1, 1); // 0=关闭 1=开启

    TimerUtil delayTimer = new TimerUtil();
    int potionSlot, oldSlot;
    boolean should;

    public boolean isThrowing() {
        return this.should;
    }


    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (ChestStealer.working
                || ChestAura.confirmedOpenedContainers.contains(ChestStealer.currentContainerPos)
                || AutoGapple.eating
                || Blink.fakePlayer != null) {
            return;
        }



        if (should && autoMoveValue.getValue().intValue() == 1) {
            mc.thePlayer.movementInput.moveForward = 1.0f;
        }
        if (mc.thePlayer.isUsingItem() && mc.thePlayer.getHeldItem() != null &&
                mc.thePlayer.getHeldItem().getItem() instanceof ItemBow) {
            return;
        }
        if (!KillAura.targets.isEmpty()) return;
        if (mc.thePlayer.openContainer != mc.thePlayer.inventoryContainer) {
            return;
        }
        if (should || !delayTimer.hasTimeElapsed(delayValue.getValue().intValue())) return;
        int hotbarPotionSlot = findPotionInHotbar();
        if (hotbarPotionSlot != -1) {
            potionSlot = hotbarPotionSlot;
            oldSlot = mc.thePlayer.inventory.currentItem;
            prepareThrow();
            return;
        }
        if (autoMoveValue.getValue().intValue() == 1) {
            int inventoryPotionSlot = getPotInInventory();
            if (inventoryPotionSlot != -1) {
                if (movePotionToHotbar(inventoryPotionSlot, selectValue.getValue().intValue())) {
                    potionSlot = selectValue.getValue().intValue();
                    oldSlot = mc.thePlayer.inventory.currentItem;
                    prepareThrow();
                }
            }
        }
    }

    private void prepareThrow() {
        Vector2f rotation = RotationUtil.getPlayerRotation();
        rotation.y = 38f;
        RotationComponent.setRotation(rotation, 0, true, false);
        this.should = true;
    }

    @EventTarget
    public void onPost(EventMotion event) {
        if (event.isPost() && should) {
            try {
                PacketUtil.sendPacket(new C09PacketHeldItemChange(potionSlot));
                ItemStack stack = mc.thePlayer.inventory.mainInventory[potionSlot];
                PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(stack));
                PacketUtil.sendPacket(new C09PacketHeldItemChange(oldSlot));

                delayTimer.reset();
                this.oldSlot = this.potionSlot = -1;
                should = false;
            } catch (Exception e) {
                e.printStackTrace();
                should = false;
            }
        }
    }

    private int findPotionInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
            if (isGoodPotion(stack)) {
                return i;
            }
        }
        return -1;
    }

    private boolean movePotionToHotbar(int inventorySlot, int hotbarSlot) {
        try {
            int windowSlot = inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
            mc.playerController.windowClick(
                    mc.thePlayer.inventoryContainer.windowId,
                    windowSlot,
                    hotbarSlot,
                    2, // 交换操作
                    mc.thePlayer
            );
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isGoodPotion(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemPotion)) return false;

        ItemPotion potion = (ItemPotion) stack.getItem();
        for (PotionEffect effect : potion.getEffects(stack)) {
            String effectName = effect.getEffectName();
            if (isDesiredEffect(effectName) && !hasEffect(effectName)) {
                return true;
            }
        }
        return false;
    }


    private boolean isDesiredEffect(String effectName) {
        return "potion.moveSpeed".equals(effectName)
                || "potion.jump".equals(effectName)
                || ("potion.regeneration".equals(effectName) && healthValue.getValue().intValue() >= mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount())
                || "potion.nightVision".equals(effectName)
                || "potion.invisibility".equals(effectName)
                || "potion.resistance".equals(effectName)
                || "potion.fireResistance".equals(effectName)
                || ("potion.heal".equals(effectName) && healthValue.getValue().intValue() >= mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount());
    }

    int getPotInInventory() {
        for (int i = 9; i < mc.thePlayer.inventoryContainer.getInventory().size(); i++) {
            ItemStack itemStack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (itemStack != null && itemStack.getItem() instanceof ItemPotion itemPotion) {
                for (PotionEffect effect : itemPotion.getEffects(itemStack)) {
                    String effectName = effect.getEffectName();
                    if (isDesiredEffect(effectName) && !hasEffect(effectName)) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    private boolean hasEffect(String effectName) {
        for (PotionEffect active : mc.thePlayer.getActivePotionEffects()) {
            if (effectName.equals(active.getEffectName())) {
                return true;
            }
        }
        return false;
    }


}