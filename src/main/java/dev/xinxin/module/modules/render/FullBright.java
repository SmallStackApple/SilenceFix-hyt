package dev.xinxin.module.modules.render;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventTick;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

public class FullBright extends Module {
    public FullBright() {
        super("FullBright", Category.Render, "夜视效果");
    }

    @EventTarget
    public void onTick(EventTick event) {
        FullBright.mc.thePlayer.addPotionEffect(new PotionEffect(Potion.nightVision.id, 1337, 5));
    }

    @Override
    public void onDisable() {
        if (FullBright.mc.thePlayer.isPotionActive(Potion.nightVision)) {
            FullBright.mc.thePlayer.removePotionEffectClient(Potion.nightVision.id);
        }
    }
}