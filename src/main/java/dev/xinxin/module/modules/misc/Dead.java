package dev.xinxin.module.modules.misc;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventUpdate;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;

public class Dead extends Module {


    public Dead() {
        super("Dead", Category.Misc,"死亡攻击");
    }

    @EventTarget
    public void onUpdate(EventUpdate eventUpdate) {
        if (mc.thePlayer.isDead) {
            mc.thePlayer.isDead = false;
        }
    }
}