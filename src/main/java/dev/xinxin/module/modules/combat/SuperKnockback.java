package dev.xinxin.module.modules.combat;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.attack.EventAttack;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.values.BoolValue;
import dev.xinxin.module.values.ModeValue;
import dev.xinxin.utils.player.MoveUtil;
import net.minecraft.network.play.client.C0BPacketEntityAction;

public class SuperKnockback
extends Module {
    private final ModeValue<KnockBackMode> modeValue = new ModeValue("Mode", KnockBackMode.values(), KnockBackMode.Vanilla);
    private final BoolValue onlyMoveValue = new BoolValue("OnlyMove", true);
    private final BoolValue onlyGroundValue = new BoolValue("OnlyGround", false);

    public SuperKnockback() {
        super("SuperKnockback", Category.Combat,"超级击退");
    }

    @EventTarget
    public void onAttack(EventAttack event) {
        if (!MoveUtil.isMoving() && this.onlyMoveValue.getValue().booleanValue() || !SuperKnockback.mc.thePlayer.onGround && this.onlyGroundValue.getValue().booleanValue()) {
            return;
        }
        switch (this.modeValue.getValue()) {
            case Vanilla: {
                if (SuperKnockback.mc.thePlayer.isSprinting()) {
                    mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(SuperKnockback.mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                }
                mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(SuperKnockback.mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(SuperKnockback.mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(SuperKnockback.mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(SuperKnockback.mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(SuperKnockback.mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                SuperKnockback.mc.thePlayer.setSprinting(true);
                SuperKnockback.mc.thePlayer.serverSprintState = true;
                break;
            }
            case SneakPacket: {
                if (SuperKnockback.mc.thePlayer.isSprinting()) {
                    SuperKnockback.mc.thePlayer.setSprinting(true);
                }
                mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(SuperKnockback.mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(SuperKnockback.mc.thePlayer, C0BPacketEntityAction.Action.START_SNEAKING));
                mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(SuperKnockback.mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(SuperKnockback.mc.thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING));
                SuperKnockback.mc.thePlayer.serverSprintState = true;
                break;
            }
            case ExtraPacket: {
                if (SuperKnockback.mc.thePlayer.isSprinting()) {
                    SuperKnockback.mc.thePlayer.setSprinting(true);
                }
                mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(SuperKnockback.mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(SuperKnockback.mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(SuperKnockback.mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(SuperKnockback.mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                SuperKnockback.mc.thePlayer.setSprinting(true);
                SuperKnockback.mc.thePlayer.serverSprintState = true;
            }
        }
    }

    public enum KnockBackMode {
        Vanilla,
        SneakPacket,
        ExtraPacket

    }
}

