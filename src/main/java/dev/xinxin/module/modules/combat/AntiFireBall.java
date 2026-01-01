package dev.xinxin.module.modules.combat;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventMotion;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.utils.client.PacketUtil;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C0APacketAnimation;

public class AntiFireBall extends Module {
    public AntiFireBall() {
        super("AntiFireBall", Category.Combat, "防火球");
    }
    @EventTarget
    public void onMotion(EventMotion event) {
if (event.isPre()) {
    mc.theWorld.getEntities(EntityFireball.class, entityFireball -> entityFireball.getDistanceSq(mc.thePlayer) <= 36).forEach(entityFireball -> {
        PacketUtil.sendPacket(new C0APacketAnimation());
        PacketUtil.sendPacket(new C02PacketUseEntity(entityFireball, C02PacketUseEntity.Action.ATTACK));
    });
}
    }
}
