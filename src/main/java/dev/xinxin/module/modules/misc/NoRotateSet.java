package dev.xinxin.module.modules.misc;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventPacketReceive;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.values.BoolValue;
import dev.xinxin.utils.RotationComponent;
import dev.xinxin.utils.client.PacketUtil;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

public class NoRotateSet
extends Module {
    private final BoolValue confirmValue = new BoolValue("Confirm", true);
    private final BoolValue illegalRotationValue = new BoolValue("ConfirmIllegalRotation", false);
    private final BoolValue noZeroValue = new BoolValue("NoZero", false);

    public NoRotateSet() {
        super("NoRotateSet", Category.Misc,"无转头动作");
    }

    @EventTarget
    public void onPacket(EventPacketReceive event) {
        Packet<?> packet = event.getPacket();
        if (NoRotateSet.mc.thePlayer == null) {
            return;
        }
        if (packet instanceof S08PacketPlayerPosLook) {
            if (((Boolean)this.noZeroValue.getValue()).booleanValue() && ((S08PacketPlayerPosLook)packet).getYaw() == 0.0f && ((S08PacketPlayerPosLook)packet).getPitch() == 0.0f) {
                return;
            }
            if ((((Boolean)this.illegalRotationValue.getValue()).booleanValue() || ((S08PacketPlayerPosLook)packet).getPitch() <= 90.0f && ((S08PacketPlayerPosLook)packet).getPitch() >= -90.0f && RotationComponent.lastServerRotation != null && ((S08PacketPlayerPosLook)packet).getYaw() != RotationComponent.lastServerRotation.x && ((S08PacketPlayerPosLook)packet).getPitch() != RotationComponent.lastServerRotation.y) && ((Boolean)this.confirmValue.getValue()).booleanValue()) {
                PacketUtil.send(new C03PacketPlayer.C05PacketPlayerLook(((S08PacketPlayerPosLook)packet).getYaw(), ((S08PacketPlayerPosLook)packet).getPitch(), NoRotateSet.mc.thePlayer.onGround));
            }
            ((S08PacketPlayerPosLook)packet).yaw = NoRotateSet.mc.thePlayer.rotationYaw;
            ((S08PacketPlayerPosLook)packet).pitch = NoRotateSet.mc.thePlayer.rotationPitch;
        }
    }
}

