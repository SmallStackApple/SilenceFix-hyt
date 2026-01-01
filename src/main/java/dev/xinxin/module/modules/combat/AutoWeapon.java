package dev.xinxin.module.modules.combat;

import cn.dev.annotations.JNICInclude;
import dev.xinxin.SilenceFix;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.attack.EventAttack;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.modules.misc.AutoGapple;
import dev.xinxin.module.modules.movement.NoLiquid;
import dev.xinxin.module.values.ModeValue;
import dev.xinxin.module.values.NumberValue;
import dev.xinxin.utils.client.HelperUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.EnumFacing;

import static net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel;

@JNICInclude
public class AutoWeapon extends Module {

    private final ModeValue<Mode> modeValue = new ModeValue<>("Mode", Mode.values(), Mode.Normal);
    private final NumberValue health = new NumberValue("Health", 5.0, 0.1, 20.0, 0.1);
    private final ModeValue<SendMode> sendModeValue = new ModeValue<>("SendMode", SendMode.values(), SendMode.Old);
    boolean hasSwitched = false;

    public AutoWeapon() {
        super("AutoWeapon", Category.Combat, "自动武器","Automatically switch to the best weapon.","使用最佳武器");
    }

    public int slot;

    enum Mode {
        Normal,
        Silence
    }

    enum SendMode {
        Old,
        New,
        None
    }

    @EventTarget
    public void onAttack(EventAttack event) {
        if (!shouldWork() || mc.thePlayer == null || mc.theWorld == null || event == null) {
            return;
        }
        if (getModule(AutoGapple.class).state) return;

        switch (modeValue.getValue()) {
            case Normal:
                handleNormalMode(event);
                break;
            case Silence:
                handleSharpBugMode(event);
                break;
        }
    }

    private void handleNormalMode(EventAttack event) {
        if (event.isPre()) {
            int bestSlot = getBestWeaponSlot();

            Module autoSnowBall = SilenceFix.instance.moduleManager.getModule(AutoProjectile.class);
            Module noLiquid = SilenceFix.instance.moduleManager.getModule(NoLiquid.class);

            if (bestSlot >= 0 && bestSlot != mc.thePlayer.inventory.currentItem) {
                if (autoSnowBall != null && autoSnowBall.getState()) {
                    autoSnowBall.setState(false);
                    HelperUtil.sendMessage("§f准备破甲，禁止开启");
                }

                if (noLiquid != null && noLiquid.getState()) {
                    noLiquid.setState(false);
                    HelperUtil.sendMessage("§f准备破甲，禁止开启");
                }

                ItemStack newWeapon = mc.thePlayer.inventory.getStackInSlot(bestSlot);

                if (!hasSwitched) {
                    if (newWeapon.getItem() == Items.golden_sword) {
                        mc.thePlayer.addChatMessage(new ChatComponentText(
                                EnumChatFormatting.YELLOW + "何树友低伤害破甲 请控制好血量"));
                    } else if (newWeapon.getItem() instanceof ItemAxe) {
                        mc.thePlayer.addChatMessage(new ChatComponentText(
                                EnumChatFormatting.RED + "何树友高伤害破甲 请控制好血量"));
                    } else {
                        mc.thePlayer.addChatMessage(new ChatComponentText(
                                EnumChatFormatting.RED + "我是何树友 正在破甲中。"));
                    }
                    hasSwitched = true;
                }
                mc.thePlayer.inventory.currentItem = bestSlot;
                mc.playerController.updateController();
            } else {
                if (autoSnowBall != null && !autoSnowBall.getState()) {
                    autoSnowBall.setState(true);
                    HelperUtil.sendMessage("§f不破甲了，自动开启雪球");
                }
            }
        } else {
            hasSwitched = false;
        }
    }


    private void handleSharpBugMode(EventAttack event) {
        if (mc.thePlayer == null || mc.thePlayer.isDead ||
                mc.getNetHandler() == null || !mc.getNetHandler().getNetworkManager().isChannelOpen()) {
            hasSwitched = false;
            return;
        }

        if (mc.thePlayer.getHealth() > health.getValue() &&
                (mc.thePlayer.getHeldItem() != null &&
                        (mc.thePlayer.getHeldItem().getItem() instanceof ItemSword ||
                                mc.thePlayer.getHeldItem().getItem() instanceof ItemAxe))) {

            if (event.isPre()) {
                int goodWeaponSlot = getGoodWeapon();
                if (goodWeaponSlot >= 0) {
                    ItemStack newWeapon = mc.thePlayer.inventory.getStackInSlot(goodWeaponSlot);

                    if (newWeapon.getItem() instanceof ItemAxe) {
                        int sharpness = getEnchantmentLevel(Enchantment.sharpness.effectId, newWeapon);
                        if (sharpness < 10) {
                            return;
                        }
                    }

                    Module noLiquid = SilenceFix.instance.moduleManager.getModule(NoLiquid.class);
                    if (noLiquid != null && noLiquid.getState()) {
                        noLiquid.setState(false);
                    }

                    if (!hasSwitched) {
                        if (newWeapon.getItem() == Items.golden_sword) {
                            mc.thePlayer.addChatMessage(new ChatComponentText(
                                    EnumChatFormatting.YELLOW + "何树友低伤害破甲 血少直接开Gapple"));
                        } else if (newWeapon.getItem() instanceof ItemAxe) {
                            mc.thePlayer.addChatMessage(new ChatComponentText(
                                    EnumChatFormatting.RED + "何树友高伤害破甲 血少直接开Gapple"));
                        }
                        hasSwitched = true;
                    }

                    mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(goodWeaponSlot));
                    switch (sendModeValue.getValue()) {
                        case New:
                            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
                            buffer.writeString("bypass_hyt");
                            mc.thePlayer.sendQueue.addToSendQueue(new C17PacketCustomPayload("bypass_hyt", buffer));
                            break;
                        case Old:
                            mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(
                                    new BlockPos(-1, -1, -1), EnumFacing.DOWN.getIndex(),
                                    mc.thePlayer.getHeldItem(), 0.0f, 0.0f, 0.0f));
                            break;
                    }
                }
            } else {
                hasSwitched = false;
                mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
            }
        } else {
            int bestSlot = getBestWeaponSlot();
            if (bestSlot >= 0 && bestSlot != mc.thePlayer.inventory.currentItem) {
                ItemStack newWeapon = mc.thePlayer.inventory.getStackInSlot(bestSlot);

                if (!hasSwitched) {
                    if (newWeapon.getItem() == Items.golden_sword) {
                        mc.thePlayer.addChatMessage(new ChatComponentText(
                                EnumChatFormatting.YELLOW + "何树友低伤害破甲 请控制好血量"));
                    } else if (newWeapon.getItem() instanceof ItemAxe) {
                        int sharpness = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, newWeapon);
                        if (sharpness <= 10) {
                            mc.thePlayer.addChatMessage(new ChatComponentText(
                                    EnumChatFormatting.RED + "何树友的秒人斧头生效了！！！"));
                        }
                    }

                    hasSwitched = true;
                }
                mc.thePlayer.inventory.currentItem = bestSlot;
                mc.playerController.updateController();
            }
        }
    }



    public boolean shouldWork() {
        return !getModule(AutoGapple.class).state;
    }

    private int getBestWeaponSlot() {
        if (mc.thePlayer == null || mc.thePlayer.isDead || mc.theWorld == null) {
            return -1;
        }

        ItemStack perfectAxe = null;
        ItemStack strongAxe = null;
        int perfectAxeSlot = -1;
        int strongAxeSlot = -1;
        int bestSwordSlot = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack == null) continue;

            if (stack.getItem() instanceof ItemAxe) {
                int sharpness = getEnchantmentLevel(Enchantment.sharpness.effectId, stack);
                if (sharpness == 10) {
                    perfectAxe = stack;
                    perfectAxeSlot = i;
                } else if (sharpness > 10) {
                    strongAxe = stack;
                    strongAxeSlot = i;
                }
            } else if (stack.getItem() instanceof ItemSword && isBestSword(stack)) {
                bestSwordSlot = i;
            }
        }

        if (perfectAxe != null) {
            return perfectAxeSlot;
        }

        if (mc.thePlayer.getHealth() <= health.getValue() && strongAxe != null) {
            return strongAxeSlot;
        }

        return bestSwordSlot;
    }

    private boolean isBestSword(ItemStack stack) {
        final float damage = getDamage(stack);
        for (int i = 0; i < mc.thePlayer.inventory.getSizeInventory(); i++) {
            ItemStack is = mc.thePlayer.inventory.getStackInSlot(i);
            if (is != null && getDamage(is) > damage && is.getItem() instanceof ItemSword) {
                return false;
            }
        }
        return true;
    }

    private int getGoodWeapon() {
        if (mc.thePlayer == null || mc.thePlayer.inventory == null) {
            return -1;
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack == null || !(stack.getItem() instanceof ItemSword || stack.getItem() instanceof ItemAxe) ||
                    i == mc.thePlayer.inventory.currentItem) {
                continue;
            }

            ItemStack currentStack = mc.thePlayer.inventory.getStackInSlot(mc.thePlayer.inventory.currentItem);
            int currentSharpness = currentStack != null ? getEnchantmentLevel(Enchantment.sharpness.effectId, currentStack) : 0;
            int stackSharpness = getEnchantmentLevel(Enchantment.sharpness.effectId, stack);

            if (stack.getItem() instanceof ItemAxe) {
                if (stackSharpness >= 10 && stackSharpness > currentSharpness) {
                    return i;
                }
            }

            if (stack.getItem() instanceof ItemSword) {
                if (stackSharpness > currentSharpness) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static float getDamage(ItemStack stack) {
        if (stack == null) return 0;

        float damage = 0;
        final Item item = stack.getItem();

        if (item instanceof ItemAxe) {
            damage += ((ItemAxe) item).getToolMaterial().getDamageVsEntity();
        } else if (item instanceof ItemSword) {
            damage += ((ItemSword) item).getDamageVsEntity();
        }

        damage += getEnchantmentLevel(Enchantment.sharpness.effectId, stack) * 1.25F +
                getEnchantmentLevel(Enchantment.fireAspect.effectId, stack) * 0.01F;
        return damage;
    }

    private int getPlayerSize() {
        return mc.getNetHandler() != null ? mc.getNetHandler().getPlayerInfoMap().size() - 1 : 0;
    }
}