package dev.xinxin.module.modules.player;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventMotion;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.values.NumberValue;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import java.util.Collection;

public class Gapple extends Module {
    public final NumberValue delayValue = new NumberValue("Delay", 500.0, 0.0, 15000.0, 1.0);
    public final NumberValue healValue = new NumberValue("Health", 12.0, 0.0, 20.0, 1.0);
    private long time = -1L;

    public Gapple() {
        super("AutoHead", Category.Misc,"自动金头");
    }


    @EventTarget
    public void onMotion(EventMotion event) {
        if (!this.hasTimePassed(this.delayValue.getValue().longValue())) {
            return;
        }
        if ((double)Gapple.mc.thePlayer.getHealth() <= this.healValue.getValue() && !this.hasRegeneration() && Gapple.findItem(36, 45, Items.skull) != -1) {
            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(Gapple.findItem(36, 45, Items.skull) - 36));
            mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(Gapple.mc.thePlayer.getHeldItem()));
            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(Gapple.mc.thePlayer.inventory.currentItem));
            this.reset();
        }
    }

    private boolean hasRegeneration() {
        Collection<PotionEffect> activeEffects = Gapple.mc.thePlayer.getActivePotionEffects();
        for (PotionEffect effect : activeEffects) {
            if (effect.getPotionID() != Potion.regeneration.id) continue;
            return true;
        }
        return false;
    }

    public boolean hasTimePassed(long MS) {
        return System.currentTimeMillis() >= this.time + MS;
    }

    public void reset() {
        this.time = System.currentTimeMillis();
    }

    public static int findItem(int startSlot, int endSlot, Item item) {
        for (int i = startSlot; i < endSlot; ++i) {
            ItemStack stack = Gapple.mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack == null || stack.getItem() != item) continue;
            return i;
        }
        return -1;
    }
}

