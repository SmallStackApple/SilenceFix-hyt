package dev.xinxin.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

public class HYTUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final int GOD_AXE_MAX_DURABILITY = 2;
    private static final int GOD_AXE_ENCHANT_ID = 16; // 没有具体名称，假设是附魔ID
    private static final int GOD_AXE_MIN_LEVEL = 666;
    private static final int KB_BALL_ENCHANT_ID = 19;
    private static final int KB_BALL_MIN_LEVEL = 2;
    private static final int FIRE_ENCHANT_BALL_ENCHANT_ID = 20;
    private static final int FIRE_ENCHANT_BALL_MIN_LEVEL = 1;
    private static final int REGEN_POTION_AMPLIFIER = 4; // 金苹果的附魔等级
    private static final String[] LOBBY_KEYWORDS = {
            "\u95ee\u9898\u53cd\u9988", // 问题反馈
            "\u7ec3\u4e60\u573a",     // 练习场
            "\u5355\u4eba\u6a21\u5f0f" // 单人模式
    };

    public static boolean isInLobby() {
        if (mc.theWorld == null) {
            return false;
        }

        return java.util.Arrays.stream(LOBBY_KEYWORDS)
                .anyMatch(keyword -> mc.theWorld.playerEntities.stream()
                        .anyMatch(player -> player.getName().contains(keyword)));
    }

    public static boolean isGoodItem(final ItemStack stack) {
        if (stack == null) {
            return false;
        }

        Item item = stack.getItem();
        return item instanceof ItemEnderPearl ||
                item == Items.arrow ||
                item == Items.lava_bucket ||
                item == Items.water_bucket ||
                isGodAxe(stack) ||
                isKBBall(stack);
    }

    public static boolean isHoldingGodAxe(EntityPlayer player) {
        ItemStack holdingItem = player.getEquipmentInSlot(0);
        return isGodAxe(holdingItem);
    }

    public static boolean isGodAxe(ItemStack stack) {
        if (stack == null || stack.getItem() != Items.golden_axe) {
            return false;
        }

        int durability = stack.getMaxDamage() - stack.getItemDamage();
        if (durability > GOD_AXE_MAX_DURABILITY) {
            return false;
        }

        NBTTagList enchantmentTagList = stack.getEnchantmentTagList();
        if (enchantmentTagList == null) {
            return false;
        }

        return hasSpecificEnchantment(enchantmentTagList, GOD_AXE_ENCHANT_ID, GOD_AXE_MIN_LEVEL);
    }

    public static boolean isKBBall(ItemStack stack) {
        if (stack == null || stack.getItem() != Items.slime_ball) {
            return false;
        }

        NBTTagList enchantmentTagList = stack.getEnchantmentTagList();
        if (enchantmentTagList == null) {
            return false;
        }

        return hasSpecificEnchantment(enchantmentTagList, KB_BALL_ENCHANT_ID, KB_BALL_MIN_LEVEL);
    }

    public static boolean isFireEnchantBall(ItemStack stack) {
        if (stack == null || stack.getItem() != Items.magma_cream) {
            return false;
        }

        NBTTagList enchantmentTagList = stack.getEnchantmentTagList();
        if (enchantmentTagList == null) {
            return false;
        }

        return hasSpecificEnchantment(enchantmentTagList, FIRE_ENCHANT_BALL_ENCHANT_ID, FIRE_ENCHANT_BALL_MIN_LEVEL);
    }

    public static boolean isHoldingEnchantedGoldenApple(EntityPlayer player) {
        ItemStack holdingItem = player.getEquipmentInSlot(0);
        return holdingItem != null &&
                holdingItem.getItem() == Items.golden_apple &&
                holdingItem.hasEffect();
    }

    public static int hasEatenGoldenApple(EntityPlayer player) {
        PotionEffect regenPotion = player.getActivePotionEffect(Potion.regeneration);
        if (regenPotion == null || regenPotion.getAmplifier() < REGEN_POTION_AMPLIFIER) {
            return -1;
        }
        return regenPotion.getDuration();
    }

    public static int isRegen(EntityPlayer player) {
        PotionEffect regenPotion = player.getActivePotionEffect(Potion.regeneration);
        return regenPotion != null ? regenPotion.getDuration() : -1;
    }

    public static int isStrength(EntityPlayer player) {
        PotionEffect strengthPotion = player.getActivePotionEffect(Potion.damageBoost);
        return strengthPotion != null ? strengthPotion.getDuration() : -1;
    }
    private static boolean hasSpecificEnchantment(NBTTagList enchantmentTagList, int enchantId, int minLevel) {
        for (int i = 0; i < enchantmentTagList.tagCount(); i++) {
            NBTTagCompound nbt = enchantmentTagList.getCompoundTagAt(i);
            if (nbt.hasKey("id") && nbt.hasKey("lvl") &&
                    nbt.getInteger("id") == enchantId &&
                    nbt.getInteger("lvl") >= minLevel) {
                return true;
            }
        }
        return false;
    }
}