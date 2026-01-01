package dev.xinxin.module.modules.render;

import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.values.BoolValue;
import dev.xinxin.module.values.NumberValue;

public final class Camera
extends Module {
    public BoolValue motionCamera = new BoolValue("Motion Camera", false);
    public final NumberValue interpolation = new NumberValue("Motion Interpolation", 0.15, 0, 0.5,.01,() -> motionCamera.getValue());

    public final BoolValue noFovValue = new BoolValue("NoFov", false);
    public final NumberValue fovValue = new NumberValue("Fov", 1.0, 0.0, 4.0, 0.1);

    public Camera() {
        super("Camera", Category.Render,"角度透视");
    }
}

