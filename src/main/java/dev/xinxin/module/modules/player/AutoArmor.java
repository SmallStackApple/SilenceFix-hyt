package dev.xinxin.module.modules.player;

import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;

public class AutoArmor extends Module {
    public AutoArmor() {
        super("AutoArmor", Category.Player,"整理装备");
    }


    public static void getBestArmor() {
        for (int type = 1; type < 5; ++type) {
            if (AutoArmor.mc.thePlayer.inventoryContainer.getSlot(4 + type).getHasStack()) {
                ItemStack is = AutoArmor.mc.thePlayer.inventoryContainer.getSlot(4 + type).getStack();
                if (AutoArmor.isBestArmor(is, type)) continue;
            }
            for (int i = 9; i < 45; ++i) {
                ItemStack is;
                if (!AutoArmor.mc.thePlayer.inventoryContainer.getSlot(i).getHasStack() || !AutoArmor.isBestArmor(is = AutoArmor.mc.thePlayer.inventoryContainer.getSlot(i).getStack(), type) || !(AutoArmor.getProtection(is) > 0.0f))
                    continue;

                shiftClick(i, type + 4);
            }
        }
    }

    public static boolean isBestArmor(ItemStack stack, int type){
        float prot = getProtection(stack);
        String strType = "";
        if(type == 1){
            strType = "helmet";
        }else if(type == 2){
            strType = "chestplate";
        }else if(type == 3){
            strType = "leggings";
        }else if(type == 4){
            strType = "boots";
        }
        if(!stack.getUnlocalizedName().contains(strType)){
            return false;
        }
        for (int i = 5; i < 45; i++) {
            if (mc.thePlayer.inventoryContainer.getSlot(i).getHasStack()) {
                ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
                if(getProtection(is) > prot && is.getUnlocalizedName().contains(strType))
                    return false;
            }
        }
        return true;
    }
    public static void shiftClick(int slot, int invSlot) {
        mc.thePlayer.sendQueue.addToSendQueue(new C0FPacketConfirmTransaction(mc.thePlayer.inventoryContainer.windowId, mc.thePlayer.inventoryContainer.transactionID, true));
        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, slot, 0, 0, mc.thePlayer);
        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, invSlot, 0, 0, mc.thePlayer);

    }


    public static float getProtection(ItemStack stack) {
        float prot = 0.0f;
        if (stack.getItem() instanceof ItemArmor) {
            ItemArmor armor = (ItemArmor)stack.getItem();

            switch (armor.getArmorMaterial().name()) {
                case "LEATHER": // 皮革甲
                    prot += 1;
                    break;
                case "IRON": // 铁甲
                    prot += 4;
                    break;
                case "DIAMOND": // 钻石甲
                    prot += 6;
                    break;
                case "GOLD": // 黄金甲
                    prot += 2;
                    break;
                case "CHAINMAIL": // 锁链甲
                    prot += 3;
                    break;
                default:
                    prot += 3;
                    break;
            }

            NBTTagList tagList = stack.getEnchantmentTagList();
            if (tagList != null) {
                for (int i = 0; i < tagList.tagCount(); ++i) {
                    NBTTagCompound tagCompound = tagList.getCompoundTagAt(i);
                    short enchantmentId = tagCompound.getShort("id");

                    switch (enchantmentId) {
                        case 0: // 保护
                            prot += 6;
                            break;
                        case 3: // 爆炸保护
                        case 1: // 火焰保护
                            prot += 1;
                            break;
                        default:
                            prot += 0;
                            break;
                    }
                }
            }
        }

        int protectionLevel = EnchantmentHelper.getEnchantmentLevel((Enchantment.protection.effectId), stack);

        if (protectionLevel >= 1 && protectionLevel <= 2) {
            prot += 3.0f;
        } else if (protectionLevel >= 3 && protectionLevel <= 4) {
            prot += 4.0f;
        } else if (protectionLevel == 5) {
            prot += 5.0f;
        }

        return prot;
    }
    public enum EMode {
        XinXin,
        OpenInv,
        FakeInv

    }
}

