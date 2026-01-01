package dev.xinxin.module.modules.movement;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventStrafe;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.utils.player.MoveUtil;

public class Strafe
extends Module {
    public Strafe() {
        super("Strafe", Category.Movement,"灵活动作");
    }

    @Override
    public void onDisable() {
        Strafe.mc.timer.timerSpeed = 1.0f;
    }

    @EventTarget
    public void onStrafe(EventStrafe event) {
        MoveUtil.strafe();
    }
}

