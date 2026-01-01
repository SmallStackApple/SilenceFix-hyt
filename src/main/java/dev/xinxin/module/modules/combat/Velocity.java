package dev.xinxin.module.modules.combat;

import com.xinxin.client.viaversion.viamcp.fixes.AttackOrder;
import dev.xinxin.SilenceFix;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventPacketReceive;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.RotationManager;
import dev.xinxin.module.modules.world.Scaffold;
import dev.xinxin.module.values.ModeValue;
import dev.xinxin.utils.RayCastUtil;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public class Velocity extends Module {

    private final ModeValue<VelocityModes> mode = new ModeValue<>("Mode", VelocityModes.values(), VelocityModes.Grim);
    private static final double WARNING_DISTANCE = 2.95;
    private long lastAttackerCheckTime = 0;

    enum VelocityModes {
        Grim
    }

    public Velocity() {
        super("Velocity", Category.Combat, "反击退");
    }

    @EventTarget
    public void onPacket(EventPacketReceive event) {
        if (SilenceFix.instance.moduleManager.getModule(Scaffold.class).state) return;
        if (mc.thePlayer == null) return;

        if (event.getPacket() instanceof S12PacketEntityVelocity wrapper) {
            if (wrapper.getEntityID() == mc.thePlayer.getEntityId()) {
                EntityLivingBase attacker = findRealAttacker();
                if (attacker != null) {
                    showAttackDistance(attacker);
                } else {
                }
                if (mode.is("Grim")) {
                    MovingObjectPosition position = RayCastUtil.rayCast(SilenceFix.instance.rotationManager.rotation,3.0f);
                    EntityLivingBase target = null;

                    if (position != null && position.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
                        target = (EntityLivingBase) position.entityHit;
                    }
                    if (KillAura.target != null) {
                        target = KillAura.target;
                    }

                    if (target != null) {
                        handleVelocity(wrapper, target);
                        event.setCancelled(true);
                    } else {
                    }
                }
            }
        }
    }
    private EntityLivingBase findRealAttacker() {
        if (System.currentTimeMillis() - lastAttackerCheckTime < 100) return null;
        lastAttackerCheckTime = System.currentTimeMillis();

        return mc.theWorld.getEntitiesWithinAABB(EntityLivingBase.class,
                        mc.thePlayer.getEntityBoundingBox().expand(5, 3, 5),
                        entity -> entity != mc.thePlayer &&
                                entity.getDistanceToEntity(mc.thePlayer) <= 3.0 &&
                                entity.canEntityBeSeen(mc.thePlayer))
                .stream()
                .min((e1, e2) -> Float.compare(
                        e1.getDistanceToEntity(mc.thePlayer),
                        e2.getDistanceToEntity(mc.thePlayer)))
                .orElse(null);
    }

    private void showAttackDistance(EntityLivingBase attacker) {
        Vec3 playerEyes = mc.thePlayer.getPositionEyes(1.0F);
        Vec3 attackerEyes = attacker.getPositionEyes(1.0F);
        double distance = playerEyes.distanceTo(attackerEyes);

        if (distance > WARNING_DISTANCE) {
            String message = EnumChatFormatting.GRAY + "[对方封号距离] " + EnumChatFormatting.RED +
                    String.format("%.2f", distance) + "m" +
                    EnumChatFormatting.GRAY + " (安全: " + WARNING_DISTANCE + "m)";
            mc.thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }

    private void handleVelocity(S12PacketEntityVelocity packet, EntityLivingBase target) {
        boolean needToggleSprint = !mc.thePlayer.serverSprintState;
        if (needToggleSprint) {
            mc.thePlayer.sendQueue.addToSendQueue(
                    new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING)
            );
        }

        for (int i = 0; i <= 5; i++) {
            AttackOrder.sendFixedAttack(mc.thePlayer, target);
        }

        mc.thePlayer.setSprinting(true);
        double motionX = (packet.getMotionX() / 8000.0D) * 0.07765d;
        double motionY = (packet.getMotionY() / 8000.0D);
        double motionZ = (packet.getMotionZ() / 8000.0D) * 0.07765d;

        mc.thePlayer.setVelocity(motionX, motionY, motionZ);

        if (needToggleSprint) {
            mc.thePlayer.sendQueue.addToSendQueue(
                    new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING)
            );
        }
    }
}