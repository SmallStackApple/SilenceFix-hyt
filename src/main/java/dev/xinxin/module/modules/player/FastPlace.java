package dev.xinxin.module.modules.player;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventTick;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.values.NumberValue;
import net.minecraft.item.ItemBlock;

public class FastPlace
        extends Module {
    private final NumberValue ticks = new NumberValue("Ticks", 0.0, 0.0, 4.0, 1.0);

    public FastPlace() {
        super("FastPlace", Category.Player,"快速放置");
    }

    @EventTarget
    public void onTick(EventTick e) {
        if (mc.currentScreen != null) {
            return;
        }
        if(mc.thePlayer.getHeldItem() != null &&
                mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock) {
            mc.rightClickDelayTimer = Math.min(mc.rightClickDelayTimer, this.ticks.getValue().intValue());
        }

    }
}

