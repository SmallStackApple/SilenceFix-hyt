package net.netease.gui;

import dev.xinxin.utils.render.animation.impl.ContinualAnimation;
import lombok.Getter;
import lombok.Setter;
import org.lwjgl.input.Mouse;

/**
 * @author ByteBreaker
 * create 19/09/2023
 */
@Getter
@Setter
public class Scroll {
    private float target, maxTarget;
    private final ContinualAnimation scrollAnim = new ContinualAnimation();

    public void use() {
        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            if (wheel > 0) {
                target += 15;
            } else target -= 15;
        }
        target = -Math.max(0, Math.min(-target, maxTarget));
    }

    public void animate() {
        scrollAnim.animate(target, 22);
    }

    public float getAnimationTarget() {
        return scrollAnim.getOutput();
    }
}
