package dev.xinxin.module.modules.render;

import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.values.NumberValue;

public class KeepFov extends Module {
    public KeepFov() {
        super("KeepFov", Category.Render, "保持视角");
    }

    public final NumberValue fov = new NumberValue("Fov", 1.0, 0.1, 2.0, 0.1);
}
