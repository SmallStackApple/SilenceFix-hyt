package dev.xinxin.module.modules.misc;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.rendering.EventRender2D;
import dev.xinxin.event.world.EventWorldLoad;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.utils.render.BezierUtil;
import dev.xinxin.utils.render.ProgressManager.ProgressBarManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;


public class BalancedTimer extends Module {
    private static BalancedTimer INSTANCE;
    public static final int RELEASE_SPEED = 5;
    private final BezierUtil yAnimation = new BezierUtil(4, 0);
    private final BezierUtil xAnimation = new BezierUtil(4, 0);
    public static int balance = 0;
    public static Stage stage = Stage.IDLE;


    public BalancedTimer() {
        super("Timer", Category.Misc,"时间管理大师");
        INSTANCE = this;
    }
    float present;

    @EventTarget
    public void onIsland(dev.xinxin.gui.Island.AddIslandEvent event) {
        if (event.getState() != dev.xinxin.gui.Island.AddIslandEvent.EventState.PRE) return;

        int tasks = mc.scheduledTasks.size();
        if (tasks <= 0) return;

        float target = Math.max(0f, Math.min(1f, tasks / 200f));
        present = dev.xinxin.utils.render.AnimationUtil.smooth(present, target, 0.1f);

        dev.xinxin.SilenceFix.instance.island.addIsland(
                () -> dev.xinxin.utils.render.fontRender.FontManager.icontestFont35.drawString("", 12, 14, -1),
                "Timer",
                tasks + " Packets",
                present
        );
    }



    @EventTarget
    public void onWorld(EventWorldLoad event) {
        balance = 0;
        stage = Stage.IDLE;

    }

    public static void preTick() {
        if (INSTANCE == null || mc.thePlayer == null || !INSTANCE.state) {
            stage = Stage.IDLE;
            mc.getTimer().tickLength = 50F;
            balance = 0;
            return;
        }

        if ((Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_1)) || Mouse.isButtonDown(3)) {
            mc.theWorld.skiptick++;
            stage = Stage.STORE;
            mc.getTimer().tickLength = 50f;
            balance = 0;
        } else if (((Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_2)) || Mouse.isButtonDown(4)) && mc.scheduledTasks.size() > 0) {
            stage = Stage.RELEASE;
            mc.getTimer().tickLength = 50F / RELEASE_SPEED;
            balance++;
        } else {
            stage = Stage.IDLE;
            mc.getTimer().tickLength = 50F;
            balance = 0;
        }

        if (mc.scheduledTasks.size() > 0) {
            INSTANCE.yAnimation.update(0);
        } else {
            INSTANCE.yAnimation.update(-30);
            INSTANCE.xAnimation.update(0);
        }
    }



    public enum Stage {
        STORE("Save"),
        IDLE("XinXin"),
        RELEASE("Release");

        private final String display;

        Stage(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
        @Override
        public String toString() {
            return display;
        }

    }

    public static boolean isInstanceEnabled() {
        return INSTANCE.state;
    }
}
