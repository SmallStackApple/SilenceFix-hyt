package dev.xinxin.module.modules.render;

import dev.xinxin.event.EventTarget;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.values.ModeValue;
import dev.xinxin.module.values.NumberValue;
import net.minecraft.entity.SwingAnimationEvent;

public class BlockHit
        extends Module {
            public static final ModeValue<swords> blockAnimation = new ModeValue("Block Anim", swords.values(), swords.Smooth);
            public static final NumberValue x = new NumberValue("X", 0, -50, 50, 1);
            public static final NumberValue y = new NumberValue("Y", 0, -50, 50, 1);
            public static final NumberValue size = new NumberValue("Size", 0, -50, 50, 1);
            public final NumberValue swingSpeed = new NumberValue("Swing Speed", 1, -200, 50, 1);
    public static final NumberValue SpeedRotate = new NumberValue("Rotate Speed", 1f, 0f, 10f, 1);
    public static final NumberValue customRotate1 = new NumberValue("CustomRotateXAxis", 0, -180, 180, 1);
    public static final NumberValue customRotate2 = new NumberValue("CustomRotateYAxis", 0, -180, 180, 1);
    public static final NumberValue customRotate3 = new NumberValue("CustomRotateZAxis", 0, -180, 180, 1);

    public static final ModeValue<RotateMode> transformFirstPersonRotate = new ModeValue("RotateMode", RotateMode.values(), RotateMode.None);

    public BlockHit() {
                super("BlockAnimations", Category.Render,"防砍动画");
}
    public enum RotateMode {
        RotateY,
        RotateXY,
        Custom,
        None
    }
    public enum swords {
        None,
        Slide,
        OldVer,
        Middle,
        Smooth,
        Sigma,
        Boop,
        Slide2,
        Old,
        Exhi,
        Swong,
        Stab,
        XinXin,
        Stella
    }
    @EventTarget
    public void onSwing(SwingAnimationEvent event) {
        int swingAnimationEnd = event.getAnimationEnd();

        swingAnimationEnd *= (-swingSpeed.getValue().floatValue() / 100f) + 1f;

        event.setAnimationEnd(swingAnimationEnd);
    }
}

