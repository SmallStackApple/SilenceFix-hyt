package dev.xinxin.module.modules.player;

import dev.xinxin.SilenceFix;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventMotion;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.ModuleManager;
import dev.xinxin.module.modules.misc.AutoGapple;
import dev.xinxin.module.modules.world.Scaffold;
import dev.xinxin.utils.InventoryUtil;
import dev.xinxin.utils.TimerUtils;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.inventory.GuiBrewingStand;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.client.gui.inventory.GuiFurnace;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.util.MovingObjectPosition;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import static dev.xinxin.utils.InventoryUtil.windowClick;

public class InvCleaner extends Module {
    private static final int SWORD_SLOT = 0;
    private static final int BLOCK_SLOT = 1;
    private static final int BOW_SLOT = 2;
    private static final int GAPPLE_SLOT = 3;
    private static final int AXE_SLOT = 4;
    private static final int PICK_SLOT = 5;
    private static final int GOLD_SWORD_SLOT = 6;
    private static final int PEARL_SLOT = 7;
    private static final int THROWABLE_SLOT = 8;
    private enum ItemProcessorType {
        SWORD, ARMOR, PICKAXE, AXE, GAPPLE, BOW, BLOCK, PEARL, THROWABLE
    }
    private interface ItemProcessor {
        boolean shouldProcess(ItemStack stack);
        void process(int slot, ItemStack stack);
    }

    public static final TimerUtils chestOpenTimer = new TimerUtils();
    private boolean wasOpeningChest = false;
    private boolean processing = false;
    private int gappleMessageCount = 0;
    private static final Map<ItemProcessorType, ItemProcessor> PROCESSORS = new HashMap<>();
    private static final Map<String, Integer> ARMOR_PRIORITY = new HashMap<>();

    static {
        ARMOR_PRIORITY.put("DIAMOND", 1000);
        ARMOR_PRIORITY.put("IRON", 30);
        ARMOR_PRIORITY.put("GOLD", 20);
        ARMOR_PRIORITY.put("CHAINMAIL", 15);
        ARMOR_PRIORITY.put("LEATHER", 5);
        PROCESSORS.put(ItemProcessorType.SWORD, new SwordProcessor());
        PROCESSORS.put(ItemProcessorType.ARMOR, new ArmorProcessor());
        PROCESSORS.put(ItemProcessorType.PICKAXE, new PickaxeProcessor());
        PROCESSORS.put(ItemProcessorType.AXE, new AxeProcessor());
        PROCESSORS.put(ItemProcessorType.GAPPLE, new GappleProcessor());
        PROCESSORS.put(ItemProcessorType.BOW, new BowProcessor());
        PROCESSORS.put(ItemProcessorType.BLOCK, new BlockProcessor());
        PROCESSORS.put(ItemProcessorType.PEARL, new PearlProcessor());
        PROCESSORS.put(ItemProcessorType.THROWABLE, new ThrowableProcessor());
    }

    public InvCleaner() {
        super("Manager", Category.Player, "整理背包");
    }

    private static boolean isSafeToOperate() {
        return mc != null && mc.thePlayer != null && mc.theWorld != null
                && mc.thePlayer.inventoryContainer != null;
    }

    private boolean shouldProcess() {
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (heldItem != null && heldItem.getItem() instanceof ItemAppleGold) {
            if (gappleMessageCount < 1) {
                mc.ingameGUI.displayTitle("§a手持金苹果无法整理背包", null, 10, 60, 10);
                gappleMessageCount++;
            }
            return false;
        } else {
            gappleMessageCount = 0;
        }
        if (!isSafeToOperate() || processing) return false;
        if (ChestStealer.isStealing
                || ChestStealer.stealingNow
                || System.currentTimeMillis() - ChestStealer.lastStealTime < 300
                || (!ChestStealer.stealCompleteTimer.finished(175) && !chestOpenTimer.hasTimeElapsed(500))) {
            return false;
        }



        if (mc.currentScreen instanceof GuiChest
                || mc.currentScreen instanceof GuiFurnace
                || mc.currentScreen instanceof GuiBrewingStand) {
            return false;
        }

        if (mc.thePlayer.inventory.getItemStack() != null) {
            windowClick(mc, -999, 0, InventoryUtil.ClickType.CLICK);
            return false;
        }
        boolean isOpeningChest = mc.thePlayer.openContainer != mc.thePlayer.inventoryContainer;
        if (wasOpeningChest && !isOpeningChest) {
            chestOpenTimer.reset();
        }
        wasOpeningChest = isOpeningChest;
        Predicate<Object> screenCheck = screen -> !(mc.currentScreen instanceof GuiCrafting)
                && !(mc.currentScreen instanceof GuiChat)
                && !(mc.currentScreen instanceof GuiFurnace)
                && !(mc.currentScreen instanceof GuiBrewingStand);

        return !isOpeningChest
                && mc.thePlayer.openContainer == mc.thePlayer.inventoryContainer
                && !mc.playerController.getIsHittingBlock()
                && !mc.playerController.getCurrentGameType().isCreative()
                && screenCheck.test(null)
                && !SilenceFix.instance.moduleManager.getModule(AutoGapple.class).state;
    }

    @EventTarget
    private void onMotion(EventMotion event) {
        if (!shouldProcess() || !event.isPre()) {
            return;
        }
        synchronized (this) {
            try {
                processing = true;
                if (mc.gameSettings.keyBindAttack.isKeyDown() && mc.objectMouseOver != null &&
                        mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    return;
                }
                if (ModuleManager.getModule(Scaffold.class).getState()) {
                    return;
                }
                Map<Integer, ItemStack> inventorySnapshot = new HashMap<>();
                for (int i = 9; i < InventoryUtil.END; i++) {
                    inventorySnapshot.put(i, mc.thePlayer.inventoryContainer.getSlot(i).getStack());
                }
                for (int i = 9; i < InventoryUtil.END; i++) {
                    ItemStack stack = inventorySnapshot.get(i);
                    if (stack == null || stack.getItem() == null) continue;

                    Item item = stack.getItem();
                    ItemStack currentStack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
                    if (currentStack == null || !ItemStack.areItemStacksEqual(stack, currentStack)) {
                        continue;
                    }
                    if (item instanceof ItemSword) {
                        PROCESSORS.get(ItemProcessorType.SWORD).process(i, stack);
                    } else if (item instanceof ItemArmor) {
                        PROCESSORS.get(ItemProcessorType.ARMOR).process(i, stack);
                    } else if (item instanceof ItemPickaxe) {
                        PROCESSORS.get(ItemProcessorType.PICKAXE).process(i, stack);
                    } else if (item instanceof ItemAxe) {
                        PROCESSORS.get(ItemProcessorType.AXE).process(i, stack);
                    } else if (item instanceof ItemAppleGold) {
                        PROCESSORS.get(ItemProcessorType.GAPPLE).process(i, stack);
                    } else if (item instanceof ItemBow) {
                        PROCESSORS.get(ItemProcessorType.BOW).process(i, stack);
                    } else if (item instanceof ItemBlock) {
                        PROCESSORS.get(ItemProcessorType.BLOCK).process(i, stack);
                    } else if (item instanceof ItemEnderPearl) {
                        PROCESSORS.get(ItemProcessorType.PEARL).process(i, stack);
                    } else if (item == Items.snowball || item == Items.egg) {
                        PROCESSORS.get(ItemProcessorType.THROWABLE).process(i, stack);
                    } else if (item instanceof ItemPotion) {
                        if (!InventoryUtil.isBuffPotion(stack)) {
                            dropItem(i);
                        }
                    } else if (!InventoryUtil.isGoodItem(stack)) {
                        dropItem(i);
                    }
                }
            } finally {
                processing = false;
            }
        }
    }

    private static class SwordProcessor implements ItemProcessor {
        @Override
        public boolean shouldProcess(ItemStack stack) {
            return stack.getItem() instanceof ItemSword;
        }

        @Override
        public void process(int slot, ItemStack stack) {
            if (stack.getItem() == Items.golden_sword) {
                int goldenSlot = GOLD_SWORD_SLOT + 36;
                ItemStack currentGolden = mc.thePlayer.inventoryContainer.getSlot(goldenSlot).getStack();

                if (slot != goldenSlot) {
                    int currentSharpness = currentGolden != null && currentGolden.getItem() == Items.golden_sword ?
                            EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, currentGolden) : -1;
                    int newSharpness = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack);

                    if (newSharpness > currentSharpness || currentGolden == null || currentGolden.getItem() != Items.golden_sword) {
                        if (currentGolden != null) {
                            moveOrDropItem(goldenSlot);
                        }
                        putItemInSlot(slot, GOLD_SWORD_SLOT);
                    } else {
                        dropItem(slot);
                    }
                }
                return;
            }

            int weaponSlot = SWORD_SLOT + 36;
            if (slot == weaponSlot) return;

            ItemStack currentWeapon = mc.thePlayer.inventoryContainer.getSlot(weaponSlot).getStack();
            float currentValue = currentWeapon != null ? getSwordValue(currentWeapon) : -1;
            float newValue = getSwordValue(stack);
            if (isOnlyGoodSword(stack)) {
                if (slot != weaponSlot) {
                    putItemInSlot(slot, SWORD_SLOT);
                }
                return;
            }

            if (newValue > currentValue) {
                if (currentWeapon != null) {
                    moveOrDropItem(weaponSlot);
                }
                putItemInSlot(slot, SWORD_SLOT);
            } else {
                dropItem(slot);
            }
        }


    }

    private static class ArmorProcessor implements ItemProcessor {
        @Override
        public boolean shouldProcess(ItemStack stack) {
            return stack.getItem() instanceof ItemArmor;
        }

        @Override
        public void process(int slot, ItemStack stack) {
            ItemArmor armor = (ItemArmor) stack.getItem();
            int armorSlot = 5 + armor.armorType;
            ItemStack equipped = mc.thePlayer.inventoryContainer.getSlot(armorSlot).getStack();

            int equippedValue = equipped != null && equipped.getItem() instanceof ItemArmor
                    ? getArmorProtection((ItemArmor) equipped.getItem(), equipped)
                    : -1;
            int newValue = getArmorProtection(armor, stack);

            if (equipped == null) {
                swapOFF(slot, armorSlot);
            } else if (newValue > equippedValue) {
                moveOrDropItem(armorSlot);
                swapOFF(slot, armorSlot);
            } else {
                dropItem(slot);
            }
        }
    }

    private static class PickaxeProcessor implements ItemProcessor {
        @Override
        public boolean shouldProcess(ItemStack stack) {
            return stack.getItem() instanceof ItemPickaxe;
        }

        @Override
        public void process(int slot, ItemStack stack) {
            int targetSlot = PICK_SLOT + 36;
            if (isOnlyGoodPickaxe(stack)) {
                if (slot != targetSlot) {
                    putItemInSlot(slot, PICK_SLOT);
                }
                return;
            }

            ItemStack targetStack = mc.thePlayer.inventoryContainer.getSlot(targetSlot).getStack();
            if (isNullItem(targetSlot)) {
                putItemInSlot(slot, PICK_SLOT);
                return;
            }
            if (stack.getItem() instanceof ItemPickaxe && targetStack.getItem() instanceof ItemPickaxe) {
                ItemPickaxe currentPickaxe = (ItemPickaxe) stack.getItem();
                ItemPickaxe targetPickaxe = (ItemPickaxe) targetStack.getItem();

                if (currentPickaxe.getToolMaterial() == targetPickaxe.getToolMaterial()) {
                    int currentDurability = stack.getMaxDamage() - stack.getItemDamage();
                    int targetDurability = targetStack.getMaxDamage() - targetStack.getItemDamage();

                    if (currentDurability > targetDurability) {
                        putItemInSlot(slot, PICK_SLOT);
                    } else if (slot != targetSlot) {
                        dropItem(slot);
                    }
                } else {
                    if (InventoryUtil.isBestTool(mc.thePlayer, stack)) {
                        putItemInSlot(slot, PICK_SLOT);
                    } else {
                        dropItem(slot);
                    }
                }
            } else {
                if (InventoryUtil.isBestTool(mc.thePlayer, stack)) {
                    putItemInSlot(slot, PICK_SLOT);
                } else {
                    dropItem(slot);
                }
            }
        }
    }


    private static class AxeProcessor implements ItemProcessor {
        @Override
        public boolean shouldProcess(ItemStack stack) {
            return stack.getItem() instanceof ItemAxe;
        }

        @Override
        public void process(int slot, ItemStack stack) {
            int targetSlot = AXE_SLOT + 36;
            if (isOnlyGoodAxe(stack)) {
                if (slot != targetSlot) {
                    putItemInSlot(slot, AXE_SLOT);
                }
                return;
            }

            ItemStack currentAxe = mc.thePlayer.inventoryContainer.getSlot(targetSlot).getStack();

            int currentSharpness = getSharpnessLevel(currentAxe);
            int newSharpness = getSharpnessLevel(stack);


            if (newSharpness >= 10) {
                if (currentAxe == null) {
                    putItemInSlot(slot, AXE_SLOT);
                }
                else if (currentSharpness >= 10) {
                    dropItem(slot);
                }
                else {
                    moveOrDropItem(targetSlot);
                    putItemInSlot(slot, AXE_SLOT);
                }
                return;
            }
            if (hasSharpnessXAxeInBackpack()) {
                dropItem(slot);
            }
            else if (currentAxe == null) {
                putItemInSlot(slot, AXE_SLOT);
            }
            else if (slot != targetSlot) {
                dropItem(slot);
            }
        }

        private int getSharpnessLevel(ItemStack stack) {
            return stack != null ? EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack) : -1;
        }


        private boolean hasSharpnessXAxeInBackpack() {
            for (int i = 9; i < 36; i++) {
                ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
                if (stack != null && stack.getItem() instanceof ItemAxe &&
                        getSharpnessLevel(stack) >= 10) {
                    return true;
                }
            }
            return false;
        }
    }

    private static boolean isOnlyGoodPickaxe(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemPickaxe)) return false;

        float selfValue = getPickaxeValue(stack);
        int betterCount = 0;

        for (int i = 0; i < InventoryUtil.END; i++) {
            ItemStack item = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (item != null && item != stack && item.getItem() instanceof ItemPickaxe) {
                if (getPickaxeValue(item) >= selfValue) {
                    betterCount++;
                }
            }
        }

        return betterCount == 0;
    }


    private static boolean isOnlyGoodAxe(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemAxe)) return false;

        float selfValue = getAxeValue(stack);
        int betterCount = 0;

        for (int i = 0; i < InventoryUtil.END; i++) {
            ItemStack item = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (item != null && item != stack && item.getItem() instanceof ItemAxe) {
                if (getAxeValue(item) >= selfValue) {
                    betterCount++;
                }
            }
        }

        return betterCount == 0;
    }

    private static float getPickaxeValue(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemPickaxe)) return 0;

        ItemPickaxe tool = (ItemPickaxe) stack.getItem();
        float base = switch (tool.getToolMaterial()) {
            case WOOD -> 1;
            case STONE -> 2;
            case IRON -> 3;
            case GOLD -> 2.5f;
            case EMERALD -> 4;
            default -> 0;
        };
        float durabilityRatio = 1f - (float) stack.getItemDamage() / stack.getMaxDamage();
        return base + durabilityRatio;
    }

    private static float getAxeValue(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemAxe)) return 0;

        ItemAxe tool = (ItemAxe) stack.getItem();
        int sharpness = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack);
        float base = switch (tool.getToolMaterial()) {
            case WOOD -> 1;
            case STONE -> 2;
            case IRON -> 3;
            case GOLD -> 2.5f;
            case EMERALD -> 4;
            default -> 0;
        };
        float durabilityRatio = 1f - (float) stack.getItemDamage() / stack.getMaxDamage();
        return base + durabilityRatio + sharpness * 0.5f;
    }



    private static class BowProcessor implements ItemProcessor {
        @Override
        public boolean shouldProcess(ItemStack stack) {
            return stack.getItem() instanceof ItemBow;
        }

        @Override
        public void process(int slot, ItemStack stack) {
            final int targetSlot = BOW_SLOT + 36;
            if (slot == targetSlot) return;

            ItemStack bestBow = getBestBow();
            if (bestBow == null) {
                putItemInSlot(slot, BOW_SLOT);
                return;
            }

            float bestValue = getBowValue(bestBow);
            float currentValue = getBowValue(stack);

            if (currentValue == bestValue && isSamePower(stack, bestBow)) {
                if (!isInHotbar(bestBow)) {
                    putItemInSlot(slot, BOW_SLOT);
                } else {
                    dropItem(slot);
                }
            } else if (currentValue == bestValue) {
                putItemInSlot(slot, BOW_SLOT);
            } else {
                dropItem(slot);
            }
        }

        private ItemStack getBestBow() {
            ItemStack best = null;
            float bestValue = -1;
            for (int i = 9; i < 45; i++) {
                ItemStack s = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
                if (s != null && s.getItem() instanceof ItemBow) {
                    float v = getBowValue(s);
                    if (v > bestValue) {
                        bestValue = v;
                        best = s;
                    }
                }
            }
            return best;
        }

        private float getBowValue(ItemStack stack) {
            float value = 0;
            value += EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, stack) * 100;
            value += EnchantmentHelper.getEnchantmentLevel(Enchantment.punch.effectId, stack) * 50;
            value += EnchantmentHelper.getEnchantmentLevel(Enchantment.flame.effectId, stack) * 80;
            value *= (1.0f - (float)stack.getItemDamage() / stack.getMaxDamage());
            return value;
        }

        private boolean isSamePower(ItemStack a, ItemStack b) {
            return EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, a) ==
                    EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, b);
        }

        private boolean isInHotbar(ItemStack stack) {
            for (int i = 36; i < 45; i++) {
                ItemStack s = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
                if (s != null && ItemStack.areItemStacksEqual(s, stack)) {
                    return true;
                }
            }
            return false;
        }
    }




    private static class GappleProcessor implements ItemProcessor {
        @Override
        public boolean shouldProcess(ItemStack stack) {
            return stack.getItem() instanceof ItemAppleGold;
        }

        @Override
        public void process(int slot, ItemStack stack) {
            final int targetSlot = GAPPLE_SLOT + 36;
            if (slot == targetSlot) return;

            ItemStack target = mc.thePlayer.inventoryContainer.getSlot(targetSlot).getStack();
            boolean isEnch = stack.getItemDamage() == 1;
            boolean targetEnch = target != null && target.getItem() instanceof ItemAppleGold && target.getItemDamage() == 1;
            boolean sameTypeAndCanMerge = target != null
                    && target.getItem() instanceof ItemAppleGold
                    && target.getItemDamage() == stack.getItemDamage()
                    && target.stackSize < target.getMaxStackSize();

            if (isEnch) {
                if (sameTypeAndCanMerge && mc.thePlayer.inventory.getItemStack() == null) {
                    windowClick(mc, slot, 0, InventoryUtil.ClickType.CLICK);
                    windowClick(mc, targetSlot, 0, InventoryUtil.ClickType.CLICK);
                    if (mc.thePlayer.inventory.getItemStack() != null) {
                        windowClick(mc, slot, 0, InventoryUtil.ClickType.CLICK);
                    }
                    return;
                }
                putItemInSlot(slot, GAPPLE_SLOT);
                return;
            }

            if (targetEnch) return;

            if (sameTypeAndCanMerge && mc.thePlayer.inventory.getItemStack() == null) {
                windowClick(mc, slot, 0, InventoryUtil.ClickType.CLICK);
                windowClick(mc, targetSlot, 0, InventoryUtil.ClickType.CLICK);
                if (mc.thePlayer.inventory.getItemStack() != null) {
                    windowClick(mc, slot, 0, InventoryUtil.ClickType.CLICK);
                }
                return;
            }

            putItemInSlot(slot, GAPPLE_SLOT);
        }
    }

    private static class BlockProcessor implements ItemProcessor {
        @Override
        public boolean shouldProcess(ItemStack stack) {
            return stack.getItem() instanceof ItemBlock;
        }

        @Override
        public void process(int slot, ItemStack stack) {
            if (InventoryUtil.isFullBlock(stack)) {
                int targetSlot = BLOCK_SLOT + 36;
                ItemStack targetStack = mc.thePlayer.inventoryContainer.getSlot(targetSlot).getStack();

                if (targetStack != null && targetStack.getItem() instanceof ItemBlock) {
                    if (targetStack.stackSize < stack.stackSize) {
                        putItemInSlot(slot, BLOCK_SLOT);
                    }
                } else if (isNullItem(targetSlot) || slot != targetSlot) {
                    putItemInSlot(slot, BLOCK_SLOT);
                }
            }
        }
    }

    private static class PearlProcessor implements ItemProcessor {
        @Override
        public boolean shouldProcess(ItemStack stack) {
            return stack.getItem() instanceof ItemEnderPearl;
        }

        @Override
        public void process(int slot, ItemStack stack) {
            int targetSlot = PEARL_SLOT + 36;
            if (isNullItem(targetSlot) || slot != targetSlot) {
                putItemInSlot(slot, PEARL_SLOT);
            }
        }
    }

    private static class ThrowableProcessor implements ItemProcessor {
        @Override
        public boolean shouldProcess(ItemStack stack) {
            return stack.getItem() == Items.snowball || stack.getItem() == Items.egg;
        }

        @Override
        public void process(int slot, ItemStack stack) {
            int targetSlot = THROWABLE_SLOT + 36;
            ItemStack targetStack = mc.thePlayer.inventoryContainer.getSlot(targetSlot).getStack();

            if (slot == targetSlot) return;

            if (targetStack == null || !isThrowable(targetStack)) {
                if (targetStack != null) {
                    moveOrDropItem(targetSlot);
                }
                putItemInSlot(slot, THROWABLE_SLOT);
                return;
            }

            if (stack.getItem() == Items.snowball && targetStack.getItem() == Items.egg) {
                if (moveToBackpack(targetSlot)) {
                    putItemInSlot(slot, THROWABLE_SLOT);
                }
            }
        }

        private boolean isThrowable(ItemStack stack) {
            return stack != null &&
                    (stack.getItem() == Items.snowball || stack.getItem() == Items.egg);
        }
    }

    private static void dropItem(int slot) {
        if (isSafeToOperate()) {
            windowClick(mc, slot, 1, InventoryUtil.ClickType.DROP_ITEM);
        }
    }

    private static boolean moveOrDropItem(int slot) {
        ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(slot).getStack();
        if (stack == null) return false;

        if (moveToBackpack(slot)) return true;

        if (stack.getItem() instanceof ItemSword ||
                stack.getItem() instanceof ItemTool ||
                stack.getItem() instanceof ItemArmor) {
            return false;
        }

        dropItem(slot);
        return false;
    }

    private static boolean moveToBackpack(int slot) {
        for (int i = 9; i < 36; i++) {
            if (mc.thePlayer.inventoryContainer.getSlot(i).getStack() == null) {
                windowClick(mc, slot, i, InventoryUtil.ClickType.SHIFT_CLICK);
                return true;
            }
        }
        return false;
    }

    private static float getSwordValue(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemSword)) return 0;

        float value = 0;
        Item item = stack.getItem();

        if (item == Items.diamond_sword) {
            value = 7000;
        } else if (item == Items.iron_sword) {
            value = 5000;
        } else if (item == Items.stone_sword) {
            value = 1000;
        } else if (item == Items.golden_sword) {
            value = 10;
        } else {
            value = 500;
        }

        int sharpness = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack);
        value += sharpness * 2500;

        int fireAspect = EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, stack);
        value += fireAspect * 300;

        int knockback = EnchantmentHelper.getEnchantmentLevel(Enchantment.knockback.effectId, stack);
        value += knockback * 150;

        int looting = EnchantmentHelper.getEnchantmentLevel(Enchantment.looting.effectId, stack);
        value += looting * 200;

        float durability = 0.5f + 0.5f * (1 - (float)stack.getItemDamage() / stack.getMaxDamage());
        value *= durability;

        return value;
    }

    private static boolean isOnlyGoodSword(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemSword)) return false;

        float threshold = 50;
        float selfValue = getSwordValue(stack);

        int betterOrEqualCount = 0;
        for (int i = 0; i < InventoryUtil.END; i++) {
            ItemStack item = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (item != null && item != stack && item.getItem() instanceof ItemSword) {
                if (getSwordValue(item) >= selfValue) {
                    betterOrEqualCount++;
                }
            }
        }

        return betterOrEqualCount == 0 && selfValue >= threshold;
    }


    private static boolean isNullItem(int slot) {
        return !mc.thePlayer.inventoryContainer.getSlot(slot).getHasStack();
    }

    private static void swapOFF(int slot, int shift) {
        if (isSafeToOperate()) {
            mc.playerController.windowClick(0, slot, 0, 0, mc.thePlayer);
            mc.playerController.windowClick(0, shift, 0, 0, mc.thePlayer);
        }
    }

    private static int getArmorProtection(ItemArmor armor, ItemStack stack) {
        int materialPriority = ARMOR_PRIORITY.getOrDefault(armor.getArmorMaterial().name(), 0);
        int protectionLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack);
        return materialPriority + protectionLevel * 10;
    }

    private static void putItemInSlot(final int slot, final int slotIn) {
        if (isSafeToOperate()) {
            windowClick(mc, slot, slotIn, InventoryUtil.ClickType.SWAP_WITH_HOT_BAR_SLOT);
        }
    }
}