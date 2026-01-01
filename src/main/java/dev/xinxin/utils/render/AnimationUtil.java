package dev.xinxin.utils.render;

import dev.xinxin.module.modules.render.HUD;

public class AnimationUtil {
    private long previousTime = System.nanoTime() / 10000L;
    public static long deltaTime = 500L;

    public static float moveUD(float current, float end, float smoothSpeed, float minSpeed) {
        float movement = (end - current) * smoothSpeed;

        if (movement > 10.0F) {
            movement = Math.max(minSpeed, movement);
            movement = Math.min(end - current, movement);
        } else if (movement < 10.0F) {
            movement = Math.min(-minSpeed, movement);
            movement = Math.max(end - current, movement);
        }

        return current + movement;
    }

    public void resetTime() {
        long currentTime = System.nanoTime() / 10000L;
        deltaTime = currentTime - this.previousTime;
        this.previousTime = currentTime;
    }

    public double animate(double value, double target, double speed, boolean tBTV) {
        if (deltaTime <= 1L) {
            deltaTime = 500L;
        }
        speed = speed * 10 / 4.0;
        double increment = speed * (double)deltaTime / 500.0;
        double returnValue = value + (target - value) * increment / 200.0;
        if (Math.abs(target - returnValue) < 0.05 * (4.0 / 10) || (tBTV ? returnValue > target : target > returnValue)) {
            return target;
        }
        return returnValue;
    }

    public double animateNoFast(double value, double target, double speed) {
        if (deltaTime <= 1L) {
            deltaTime = 500L;
        }
        speed = speed * 10 / 4.0;
        double increment = speed * (double)deltaTime / 500.0;
        double returnValue = value + (target - value) * increment / 200.0;
        if (Math.abs(target - returnValue) < 0.05 * (4.0 / 10)) {
            return target;
        }
        return returnValue;
    }

    public static float animate(float value, float target, float speed) {
        float increment;
        float returnValue;
        if (deltaTime <= 1L) {
            deltaTime = 500L;
        }
        if ((double)Math.abs(target - (returnValue = value + (target - value) * (increment = (speed *= 70.0f) * (float)deltaTime / 500.0f) / 200.0f)) < 0.05 * (4.0 / 10)) {
            return target;
        }
        return returnValue;
    }

    public static float getAnimationState(float animation, float finalState, float speed) {
        final float add = (deltaTime * (speed / 1000f));
        if (animation < finalState) {
            if (animation + add < finalState) {
                animation += add;
            } else {
                animation = finalState;
            }
        } else if (animation - add > finalState) {
            animation -= add;
        } else {
            animation = finalState;
        }
        return animation;
    }
    public double animateRO(double value, double target, double speed) {
        double increment;
        double returnValue;
        if (deltaTime <= 1L) {
            deltaTime = 500L;
        }
        if (Math.abs(target - (returnValue = value + (target - value) * (increment = speed * (double)deltaTime / 500.0) / 200.0)) < 0.05) {
            return target;
        }
        return returnValue;
    }


    public static float smooth(float current, float target, float speed) {
        boolean larger = target > current;
        if (speed < 0.0) {
            speed = 0.0F;
        } else if (speed > 1.0) {
            speed = 1.0F;
        }
        float dif = Math.max(target, current) - Math.min(target, current);
        float factor = dif * speed;
        current = larger ? current + factor : current - factor;
        return current;
    }
    
    public static float animateSmooth(float current, float target, float speed) {
        boolean larger;
        if (current == target) {
            return current;
        }
        boolean bl = larger = target > current;
        if (speed < 0.0f) {
            speed = 0.0f;
        } else if (speed > 1.0f) {
            speed = 1.0f;
        }
        double dif = Math.max((double)target, (double)current) - Math.min((double)target, (double)current);
        double factor = dif * (double)speed;
        if (factor < 0.1) {
            factor = 0.1;
        }
        if (larger) {
            if ((current += (float)factor) >= target) {
                current = target;
            }
        } else if (target < current && (current -= (float)factor) <= target) {
            current = target;
        }
        return current;
    }
}

