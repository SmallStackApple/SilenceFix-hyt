package dev.xinxin.module.modules.combat;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.misc.EventClick;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.values.BoolValue;
import dev.xinxin.module.values.NumberValue;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemSword;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.List;
import java.util.Random;

public class Reach
extends Module {
    public static NumberValue MinReach = new NumberValue("Min", 3.0, 0.0, 6.0, 0.1);
    public static NumberValue MaxReach = new NumberValue("Max", 3.0, 0.1, 6.0, 0.1);
    private final BoolValue RandomReach = new BoolValue("Random Reach", true);
    private final BoolValue weaponOnly = new BoolValue("Weapon Only", false);
    private final BoolValue movingOnly = new BoolValue("Moving Only", false);
    private final BoolValue sprintOnly = new BoolValue("Sprint Only", false);
    private final BoolValue hitThroughBlocks = new BoolValue("HitThroughBlocks", false);

    public Reach() {
        super("Reach", Category.Combat,"长臂猿");
    }

    public static double getRandomDoubleInRange(double minDouble, double maxDouble) {
        if (minDouble >= maxDouble) {
            return minDouble;
        }
        return minDouble + (new Random().nextDouble() * (maxDouble - minDouble));
    }

    public static Object[] doReach(double reachValue, double AABB) {
        reachValue = Math.min(reachValue, MaxReach.getValue());

        Entity target = mc.getRenderViewEntity();
        if (target == null || mc.theWorld == null) {
            return null;
        }Entity entity = null;
        Reach.mc.mcProfiler.startSection("pick");
        Vec3 targetEyes = target.getPositionEyes(0.0f);
        Vec3 targetLook = target.getLook(0.0f);
        Vec3 targetVector = targetEyes.addVector(targetLook.xCoord * reachValue, targetLook.yCoord * reachValue, targetLook.zCoord * reachValue);
        Vec3 targetVec = null;
        List<Entity> targetHitbox = Reach.mc.theWorld.getEntitiesWithinAABBExcludingEntity(target, target.getEntityBoundingBox().addCoord(targetLook.xCoord * reachValue, targetLook.yCoord * reachValue, targetLook.zCoord * reachValue).expand(1.0, 1.0, 1.0));
        double reaching = reachValue;
        for (Entity targetEntity : targetHitbox) {
            double targetHitVec;
            if (!targetEntity.canBeCollidedWith()) continue;
            float targetCollisionBorderSize = targetEntity.getCollisionBorderSize();
            AxisAlignedBB targetAABB = targetEntity.getEntityBoundingBox().expand(targetCollisionBorderSize, targetCollisionBorderSize, targetCollisionBorderSize);
            targetAABB = targetAABB.expand(AABB, AABB, AABB);
            MovingObjectPosition targetPosition = targetAABB.calculateIntercept(targetEyes, targetVector);
            if (targetAABB.isVecInside(targetEyes)) {
                if (!(0.0 < reaching) && reaching != 0.0) continue;
                entity = targetEntity;
                targetVec = targetPosition == null ? targetEyes : targetPosition.hitVec;
                reaching = 0.0;
                continue;
            }
            if (targetPosition == null || !((targetHitVec = targetEyes.distanceTo(targetPosition.hitVec)) < reaching) && reaching != 0.0) continue;
            if (targetEntity == target.ridingEntity) {
                if (reaching != 0.0) continue;
                entity = targetEntity;
                targetVec = targetPosition.hitVec;
                continue;
            }
            entity = targetEntity;
            targetVec = targetPosition.hitVec;
            reaching = targetHitVec;
        }
        if (reaching < reachValue && !(entity instanceof EntityLivingBase) && !(entity instanceof EntityItemFrame)) {
            entity = null;
        }
        Reach.mc.mcProfiler.endSection();
        if (entity == null || targetVec == null) {
            return null;
        }
        return new Object[]{entity, targetVec};
    }


    @EventTarget
    public void onClicked(EventClick ev) {
        BlockPos blocksReach;
        if (this.weaponOnly.getValue()) {
            if (Reach.mc.thePlayer.getCurrentEquippedItem() == null) {
                return;
            }
            if (!(Reach.mc.thePlayer.getCurrentEquippedItem().getItem() instanceof ItemSword) && !(Reach.mc.thePlayer.getCurrentEquippedItem().getItem() instanceof ItemAxe)) {
                return;
            }
        }
        if (this.movingOnly.getValue() && (double)Reach.mc.thePlayer.moveForward == 0.0 && (double)Reach.mc.thePlayer.moveStrafing == 0.0) {
            return;
        }
        if (this.sprintOnly.getValue() && !Reach.mc.thePlayer.isSprinting()) {
            return;
        }
        if (!this.hitThroughBlocks.getValue() && Reach.mc.objectMouseOver != null && (blocksReach = Reach.mc.objectMouseOver.getBlockPos()) != null && Reach.mc.theWorld.getBlockState(blocksReach).getBlock() != Blocks.air) {
            return;
        }
        double Reach2 = this.RandomReach.getValue() ?
                Reach.getRandomDoubleInRange(MinReach.getValue(), MaxReach.getValue()) :
                MinReach.getValue();
        Reach2 = Math.min(Reach2, MaxReach.getValue());

        Object[] reach = Reach.doReach(Reach2, 0.0);
        if (reach == null) {
            return;
        }
        Reach.mc.objectMouseOver = new MovingObjectPosition((Entity)reach[0], (Vec3)reach[1]);
        Reach.mc.pointedEntity = (Entity)reach[0];
    }
}

