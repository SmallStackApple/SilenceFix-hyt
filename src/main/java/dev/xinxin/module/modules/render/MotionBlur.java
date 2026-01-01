package dev.xinxin.module.modules.render;

import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.values.NumberValue;

public class MotionBlur
extends Module {
    public final NumberValue blurAmount = new NumberValue("Amount", 7.0, 0.0, 10.0, 0.1);

    public MotionBlur() {
        super("MotionBlur", Category.Render,"动态模糊");
    }
}

