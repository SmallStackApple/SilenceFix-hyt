package dev.xinxin.module.modules.player;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventPacketReceive;
import dev.xinxin.event.world.EventTick;
import dev.xinxin.event.world.EventWorldLoad;
import dev.xinxin.gui.notification.NotificationManager;
import dev.xinxin.gui.notification.NotificationType;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.modules.combat.AutoProjectile;
import dev.xinxin.module.modules.combat.KillAura;
import dev.xinxin.module.modules.world.Scaffold;
import dev.xinxin.module.values.BoolValue;
import dev.xinxin.utils.RotationComponent;
import dev.xinxin.utils.player.BlockUtil;
import dev.xinxin.utils.player.PlayerUtil;
import dev.xinxin.utils.player.RotationNew;
import dev.xinxin.utils.player.RotationUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockBed;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.*;
import dev.xinxin.utils.vec.Vector3d;

public class Breaker extends Module {
    private static final BoolValue whiteListOwnBed = new BoolValue("WhiteList OwnBed", true);

    private Vector3d block;
    private Vector3d lastBlock;
    private Vector3d home;
    private double damage;                // 用 double
    private EnumFacing hitSide;           // 记录命中的面
    private BlockPos targetPos;           // 记录目标方块
    boolean checkHome;

    public Breaker() {
        super("Breaker", Category.Player,"挖床器");
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (KillAura.target != null) return;
        if (AutoProjectile.targetRotation != null) return;
        if (getModule(Scaffold.class).state) return;

        this.lastBlock = this.block;
        this.block = this.block();
        if (this.block == null) return;

        BlockPos bp = new BlockPos(block.getX(), block.getY(), block.getZ());
        this.targetPos = bp;

        mc.thePlayer.updateTool(bp);

        RotationNew rots = RotationUtil.getRotationBlock(bp, 0.4f);
        RotationComponent.setRotation(rots, 180, true);

        MovingObjectPosition mop = RotationUtil.rayCast(rots, 4.5);
        this.hitSide = (mop != null && mop.getBlockPos() != null) ? mop.sideHit : EnumFacing.UP;

        if (this.lastBlock == null || !this.lastBlock.equals(this.block)) {
            this.damage = 0.0;
        }

        this.destroy();
    }

    public Vector3d block() {
        if (this.home != null && mc.thePlayer.getDistanceSq(this.home.getX(), this.home.getY(), this.home.getZ()) < 1337.0 && whiteListOwnBed.getValue()) {
            return null;
        }
        Vector3d pos = null;
        for (int x = -5; x <= 5; ++x) {
            for (int y = -5; y <= 5; ++y) {
                for (int z = -5; z <= 5; ++z) {
                    MovingObjectPosition movingObjectPosition;
                    Block block = BlockUtil.blockRelativeToPlayer(x, y, z);
                    Vector3d position = new Vector3d(mc.thePlayer.posX + (double) x, mc.thePlayer.posY + (double) y, mc.thePlayer.posZ + (double) z);
                    if (!(block instanceof BlockBed) || (movingObjectPosition = RotationUtil.rayCast(new RotationNew(RotationUtil.calculate(position)), 4.5)) == null || movingObjectPosition.hitVec.distanceTo(new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)) > 4.5)
                        continue;
                    BlockPos blockPos = movingObjectPosition.getBlockPos();
                    if (blockPos != null && !blockPos.equalsVector(position)) {
                        pos = new Vector3d(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                        continue;
                    }
                    var addVec = position;
                    double hardness = Double.MAX_VALUE;
                    boolean empty = false;
                    for (int addX = -1; addX <= 1; ++addX) {
                        for (int addY = 0; addY <= 1; ++addY) {
                            for (int addZ = -1; addZ <= 1; ++addZ) {
                                Block possibleBlock;
                                if (empty || mc.thePlayer.getDistanceSq(position.getX() + (double) addX, position.getY() + (double) addY, position.getZ() + (double) addZ) + 4.0 > 20.25 || Math.abs(addX) + Math.abs(addY) + Math.abs(addZ) != 1 || (possibleBlock = BlockUtil.block(position.getX() + (double) addX, position.getY() + (double) addY, position.getZ() + (double) addZ)) instanceof BlockBed)
                                    continue;
                                if (possibleBlock instanceof BlockAir) {
                                    empty = true;
                                    continue;
                                }
                                double possibleHardness = possibleBlock.getBlockHardness();
                                if (!(possibleHardness < hardness)) continue;
                                hardness = possibleHardness;
                                addVec = position.add(new Vector3d(addX, addY, addZ));
                            }
                        }
                    }
                    if (!empty) {
                        if (addVec.equals(position)) {
                            return null;
                        }
                        return addVec;
                    }
                    return position;
                }
            }
        }
        return pos;
    }

//    public void updateDamage(BlockPos blockPos, double hardness) {
//        this.damage += (int) hardness;
//        mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), blockPos, (int)(this.damage * 10.0 - 1.0));
//    }

    public void destroy() {
        if (this.targetPos == null) return;
        double hardness = PlayerUtil.getPlayerRelativeBlockHardness(
                mc.thePlayer, mc.theWorld, this.targetPos, mc.thePlayer.inventory.currentItem);

        // 首次开始破坏
        if (this.damage == 0.0) {
            mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(
                    C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, this.targetPos, this.hitSide));
        }

        // 叠加进度（关键修复）
        this.damage += hardness;

        // 发送客户端破坏动画进度（0~9）
        int stage = Math.max(0, Math.min(9, (int)(this.damage * 10.0) - 1));
        mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), this.targetPos, stage);

        // 进度满：收尾
        if (this.damage >= 1.0) {
            mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(
                    C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, this.targetPos, this.hitSide));
            mc.playerController.onPlayerDestroyBlock(this.targetPos, this.hitSide);
            mc.thePlayer.swingItem();
            this.damage = 0.0;
            return;
        }

        // 还没满，挥一下动画即可
        mc.thePlayer.swingItem();
    }

    // 新增：限制只在进世界后的短时间内识别一次
    private long homeDetectDeadlineMs = 0L;

    @EventTarget
    public void onWorldLoad(EventWorldLoad e) {
        this.checkHome = true;
        this.homeDetectDeadlineMs = System.currentTimeMillis() + 5000; // 进世界后5秒内生效
        this.home = null; // 可选：重置旧的home
    }

    @EventTarget
    public void onPacketReceiveSync(EventPacketReceive event) {
        if (!checkHome || System.currentTimeMillis() > homeDetectDeadlineMs) return;

        if (event.getPacket() instanceof S08PacketPlayerPosLook s08) {
            double d = mc.thePlayer.getDistance(s08.getX(), s08.getY(), s08.getZ());
            // 大位移一般是重生/回岛；阈值你原来用40没问题
            if (d > 40.0) {
                // 在目标点附近找真正的床
                BlockPos bed = findNearestBedAround(new BlockPos(s08.getX(), s08.getY(), s08.getZ()), 4);
                if (bed != null) {
                    this.home = new dev.xinxin.utils.vec.Vector3d(bed.getX(), bed.getY(), bed.getZ());
                } else {
                    // 找不到就退而求其次用传送点
                    this.home = new dev.xinxin.utils.vec.Vector3d(s08.getX(), s08.getY(), s08.getZ());
                }
                NotificationManager.post(NotificationType.SUCCESS, "Breaker", "Your bed has been whitelisted!");
                this.checkHome = false; // 关键：只识别一次
            }
        }
    }

    // 在pos周围r半径内找最近的床（优先）
    private BlockPos findNearestBedAround(BlockPos center, int r) {
        BlockPos best = null;
        double bestDist2 = Double.MAX_VALUE;
        for (int x = -r; x <= r; x++) {
            for (int y = -2; y <= 2; y++) { // 床高度范围小一点够了
                for (int z = -r; z <= r; z++) {
                    BlockPos p = center.add(x, y, z);
                    Block b = mc.theWorld.getBlockState(p).getBlock();
                    if (b instanceof net.minecraft.block.BlockBed) {
                        double d2 = center.distanceSq(p);
                        if (d2 < bestDist2) {
                            bestDist2 = d2;
                            best = p;
                        }
                    }
                }
            }
        }
        return best;
    }
}