package dev.xinxin.utils.component;

import dev.xinxin.SilenceFix;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventMotion;

public final class FallDistanceComponent {
    public static float distance;
    private float lastDistance;

    @EventTarget
    private void onMotion(EventMotion event) {
        if (event.isPre()) {
            float fallDistance = SilenceFix.mc.thePlayer.fallDistance;
            if (fallDistance == 0.0f) {
                distance = 0.0f;
            }
            distance += fallDistance - this.lastDistance;
            this.lastDistance = fallDistance;
        }
    }
}

