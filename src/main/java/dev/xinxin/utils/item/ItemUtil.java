package dev.xinxin.utils.item;

import net.minecraft.block.*;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerBrewingStand;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.ContainerFurnace;
import net.minecraft.item.*;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;

import java.util.*;

/**
 * @author Auth
 * @since 09/07/2022
 */
public class ItemUtil {

    private static final List<Item> WHITELISTED_ITEMS = Arrays.asList(Items.fishing_rod, Items.water_bucket, Items.bucket, Items.arrow, Items.bow, Items.snowball, Items.egg, Items.ender_pearl);

    private static final HashMap<Integer, Integer> GOOD_POTIONS = new HashMap<Integer, Integer>() {{
        put(6, 1); // Instant Health
        put(10, 2); // Regeneration
        put(11, 3); // Resistance
        put(21, 4); // Health Boost
        put(22, 5); // Absorption
        put(23, 6); // Saturation
        put(5, 7); // Strength
        put(1, 8); // Speed
        put(12, 9); // Fire Resistance
        put(14, 10); // Invisibility
        put(3, 11); // Haste
        put(13, 12); // Water Breathing
    }};


    public static boolean useful(final ItemStack stack) {
        if (stack == null) return false;
        final Item item = stack.getItem();

        if (item instanceof ItemPotion) {
            final ItemPotion potion = (ItemPotion) item;
            final List<PotionEffect> potionEffects = potion.getEffects(stack);
            if (potionEffects == null || potionEffects.isEmpty()) return true;
            return goodPotion(potionEffects.get(0).getPotionID());
        }

        if (item instanceof ItemBlock) {
            final Block block = ((ItemBlock) item).getBlock();
            if (block instanceof BlockGlass || block instanceof BlockStainedGlass || (block.isFullBlock() && !(block instanceof BlockTNT || block instanceof BlockSlime || block instanceof BlockFalling))) {
                return true;
            }
        }

        return item instanceof ItemSword ||
                item instanceof ItemTool ||
                item instanceof ItemArmor ||
                item instanceof ItemFood ||
                WHITELISTED_ITEMS.contains(item);
    }

    public static boolean goodPotion(final int id) {
        return GOOD_POTIONS.containsKey(id);
    }

    public ItemStack getCustomSkull(final String name, final String url) {
        final String gameProfileData = String.format("{\"textures\":{\"SKIN\":{\"url\":\"%s\"}}}", url);
        final String base64Encoded = Base64.getEncoder().encodeToString(gameProfileData.getBytes());
        return getItemStack(String.format("skull 1 3 {SkullOwner:{Id:\"%s\",Name:\"%s\",Properties:{textures:[{Value:\"%s\"}]}}}", UUID.randomUUID(), name, base64Encoded));
    }

    public static float bow(final ItemStack stack) {
        if (stack == null) return 0;
        final float power = EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, stack) * 0.2f;
        final float punch = EnchantmentHelper.getEnchantmentLevel(Enchantment.punch.effectId, stack) * 0.3f;
        final float flame = EnchantmentHelper.getEnchantmentLevel(Enchantment.flame.effectId, stack) * 0.3f;
        final float infinity = EnchantmentHelper.getEnchantmentLevel(Enchantment.infinity.effectId, stack) * 1f;
        return power + punch + flame + infinity;
    }

    public static float damage(final ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemSword)) return 0;
        final ItemSword sword = (ItemSword) stack.getItem();
        final int level = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack);
        return (float) (sword.getDamageVsEntity() + level * 1.25);
    }

    public static float mineSpeed(final ItemStack stack) {
        if (stack == null) return 0;
        final Item item = stack.getItem();
        int sharpness = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack);
        int level = EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, stack);

        switch (level) {
            case 1:
                level = 30;
                break;

            case 2:
                level = 69;
                break;

            case 3:
                level = 120;
                break;

            case 4:
                level = 186;
                break;

            case 5:
                level = 271;
                break;

            default:
                level = 0;
                break;
        }

        if (item instanceof ItemPickaxe) {
            final ItemPickaxe pickaxe = (ItemPickaxe) item;
            return pickaxe.getToolMaterial().getEfficiencyOnProperMaterial() + level + sharpness;
        } else if (item instanceof ItemSpade) {
            final ItemSpade shovel = (ItemSpade) item;
            return shovel.getToolMaterial().getEfficiencyOnProperMaterial() + level;
        } else if (item instanceof ItemAxe) {
            final ItemAxe axe = (ItemAxe) item;
            return axe.getToolMaterial().getEfficiencyOnProperMaterial() + level;
        }

        return 0;
    }

    public boolean isHoldingEnchantedGoldenApple() {
        ItemStack holdingItem = Minecraft.getMinecraft().thePlayer.getHeldItem();
        if (holdingItem == null) return false;
        return holdingItem.getItem() == Items.golden_apple && holdingItem.hasEffect();
    }

    public static double armorReduction(final ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemArmor)) return 0;
        final ItemArmor armor = (ItemArmor) stack.getItem();
        return armor.damageReduceAmount + EnchantmentHelper.getEnchantmentModifierDamage(new ItemStack[]{stack}, DamageSource.generic) * 0.1;
    }

    public ItemStack getItemStack(int slot) {
        return Minecraft.getMinecraft().thePlayer.inventory.getStackInSlot(slot);
    }

    public ItemStack getItemStack(Container container, int slot) {
        if (slot >= 99) {
            return getItemStack(slot - 100);
        } else {
            if (container instanceof ContainerChest)
                return ((ContainerChest) container).getLowerChestInventory().getStackInSlot(slot);
            else if (container instanceof ContainerFurnace)
                return ((ContainerFurnace) container).tileFurnace.getStackInSlot(slot);
            else if (container instanceof ContainerBrewingStand)
                return ((ContainerBrewingStand) container).tileBrewingStand.getStackInSlot(slot);
            else
                return null;
        }
    }

    public ItemStack getItemStack(String command) {
        try {
            command = command.replace('&', '\u00a7');
            final String[] args;
            int i = 1;
            int j = 0;
            args = command.split(" ");
            final ResourceLocation resourcelocation = new ResourceLocation(args[0]);
            final Item item = Item.itemRegistry.getObject(resourcelocation);

            if (args.length >= 2 && args[1].matches("\\d+")) {
                i = Integer.parseInt(args[1]);
            }

            if (args.length >= 3 && args[2].matches("\\d+")) {
                j = Integer.parseInt(args[2]);
            }

            final ItemStack itemstack = new ItemStack(item, i, j);
            if (args.length >= 4) {
                final StringBuilder NBT = new StringBuilder();

                int nbtCount = 3;
                while (nbtCount < args.length) {
                    NBT.append(" ").append(args[nbtCount]);
                    nbtCount++;
                }

                itemstack.setTagCompound(JsonToNBT.getTagFromJson(NBT.toString()));
            }
            return itemstack;
        } catch (final Exception ex) {
            ex.printStackTrace();
            return new ItemStack(Blocks.barrier);
        }
    }

    public String getCustomSkullNBT(final String name, final String url) {
        final String gameProfileData = String.format("{\"textures\":{\"SKIN\":{\"url\":\"%s\"}}}", url);
        final String base64Encoded = Base64.getEncoder().encodeToString(gameProfileData.getBytes());
        return String.format("SkullOwner:{Id:\"%s\",Name:\"%s\",Properties:{textures:[{Value:\"%s\"}]}}", UUID.randomUUID(), name, base64Encoded);
    }
}
