package dev.xinxin.utils;


import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventMotion;

import static dev.xinxin.SilenceFix.mc;

public final class FallDistanceManager {

    public float distance;
    private float lastDistance;

    @EventTarget
    private void onMotion(EventMotion event) {
        if (event.isPre()) {
            final float fallDistance = mc.thePlayer.fallDistance;

            if (fallDistance == 0) {
                distance = 0;
            }

            distance += fallDistance - lastDistance;
            lastDistance = fallDistance;
        }
    }
}
