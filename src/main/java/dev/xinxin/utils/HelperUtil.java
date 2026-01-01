package dev.xinxin.utils;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class HelperUtil {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean isValid(final Item item) {
        return item instanceof ItemBlock && !invalidBlocks.contains(((ItemBlock) (item)).getBlock());
    }

    private static final List<Block> invalidBlocks = Arrays.asList(Blocks.enchanting_table, Blocks.chest, Blocks.ender_chest,
            Blocks.trapped_chest, Blocks.anvil, Blocks.sand, Blocks.web, Blocks.torch,
            Blocks.crafting_table, Blocks.furnace, Blocks.waterlily, Blocks.dispenser,
            Blocks.stone_pressure_plate, Blocks.wooden_pressure_plate, Blocks.noteblock,
            Blocks.dropper, Blocks.tnt, Blocks.standing_banner, Blocks.wall_banner, Blocks.redstone_torch, Blocks.crafting_table);

    public static int getBlockSlot() {
        for (int i = 0; i < 9; ++i) {
            if (!mc.thePlayer.inventoryContainer.getSlot(i + 36).getHasStack()) continue;
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i + 36).getStack();
            if (stack != null && isValid(stack.getItem())) {
                return i;
            }
        }
        return 0;
    }

    public static EntityTNTPrimed findThreateningTnt(double angleDeg, double minSpeed, int maxAheadTicks, double maxScanRange) {
        if (mc.theWorld == null || mc.thePlayer == null) return null;

        final double maxRangeSq = maxScanRange * maxScanRange;
        EntityTNTPrimed best = null;
        double bestDistSq = Double.MAX_VALUE;

        for (Object obj : mc.theWorld.loadedEntityList) {
            if (!(obj instanceof EntityTNTPrimed tnt)) continue;

            double d2 = mc.thePlayer.getDistanceSqToEntity(tnt);
            if (d2 > maxRangeSq) continue;

            if (isTntHeadingToTarget(tnt, mc.thePlayer, angleDeg, minSpeed, maxAheadTicks)) {
                if (d2 < bestDistSq) {
                    bestDistSq = d2;
                    best = tnt;
                }
            }
        }
        return best;
    }

    public static List<BlockPos> getFourDirectionBlocks() {
        List<BlockPos> result = new ArrayList<>();
        if (mc.theWorld == null || mc.thePlayer == null) return result;

        BlockPos base = new BlockPos(mc.thePlayer);

        BlockPos[] directions = new BlockPos[] {
                new BlockPos(1, 0, 0),
                new BlockPos(-1, 0, 0),
                new BlockPos(0, 0, 1),
                new BlockPos(0, 0, -1)
        };

        for (BlockPos dir : directions) {
            for (int dy = 0; dy < 2; dy++) {
                BlockPos pos = base.add(dir.getX(), dy, dir.getZ());
                if (isReplaceable(pos) && hasSolidNeighbor(pos)) {
                    result.add(pos);
                }
            }
        }
        return result;
    }

    private static boolean isReplaceable(BlockPos pos) {
        IBlockState state = mc.theWorld.getBlockState(pos);
        return state.getBlock().isReplaceable(mc.theWorld, pos);
    }

    private static boolean hasSolidNeighbor(BlockPos pos) {
        for (EnumFacing f : EnumFacing.values()) {
            BlockPos n = pos.offset(f);
            IBlockState ns = mc.theWorld.getBlockState(n);
            Block nb = ns.getBlock();
            if (nb != Blocks.air && nb.canCollideCheck(ns, false)) return true;
        }
        return false;
    }

    public static boolean isTntHeadingToTarget(Entity tnt, EntityLivingBase target, double angleDeg, double minSpeed, int maxAheadTicks) {
        if (tnt == null || target == null || tnt.isDead) return false;

        final Vec3 tntPos = new Vec3(tnt.posX, tnt.posY, tnt.posZ);
        final Vec3 toTarget = new Vec3(target.posX - tnt.posX,
                                       (target.posY + target.getEyeHeight()) - (tnt.posY + tnt.height * 0.5),
                                       target.posZ - tnt.posZ);

        final Vec3 vel = new Vec3(tnt.motionX, tnt.motionY, tnt.motionZ);
        final double speed = vel.lengthVector();
        if (speed < minSpeed) return false;

        final double cosTheta = dotSafe(normalizeSafe(vel), normalizeSafe(toTarget));
        final double cosLimit = Math.cos(Math.toRadians(angleDeg));
        if (cosTheta < cosLimit) return false;

        final double nowDistSq = toTarget.lengthVector() * toTarget.lengthVector();
        double bestFutureDistSq = nowDistSq;
        for (int i = 1; i <= Math.max(1, maxAheadTicks); i++) {
            final double fx = tnt.posX + tnt.motionX * i;
            final double fy = tnt.posY + tnt.motionY * i;
            final double fz = tnt.posZ + tnt.motionZ * i;
            final double dx = target.posX - fx;
            final double dy = (target.posY + target.getEyeHeight()) - (fy + tnt.height * 0.5);
            final double dz = target.posZ - fz;
            final double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 < bestFutureDistSq) bestFutureDistSq = d2;
        }

        return bestFutureDistSq < nowDistSq * 0.98; // 允许一点浮动
    }

    private static Vec3 normalizeSafe(Vec3 v) {
        double len = v.lengthVector();
        return (len > 1.0E-7) ? new Vec3(v.xCoord / len, v.yCoord / len, v.zCoord / len) : new Vec3(0, 0, 0);
    }

    private static double dotSafe(Vec3 a, Vec3 b) {
        return a.xCoord * b.xCoord + a.yCoord * b.yCoord + a.zCoord * b.zCoord;
    }

    public static class TntIncomingInfo {
        public final Vec3 incomingDir;   // TNT -> Player 的单位向量（水平为主）
        public final float yaw;          // 面向 TNT 来向的 yaw
        public final float reversedYaw;  // 反向 yaw = yaw + 180（已规范化）
        public final List<BlockPos> shieldPositions; // 按优先级排序的可放置点（近→远）

        public TntIncomingInfo(Vec3 incomingDir, float yaw, float reversedYaw, List<BlockPos> shieldPositions) {
            this.incomingDir = incomingDir;
            this.yaw = yaw;
            this.reversedYaw = reversedYaw;
            this.shieldPositions = shieldPositions;
        }
    }

    /** 传入最近/最威胁的 TNT，返回来向、反向 yaw、以及可放置的挡位 */
    public static TntIncomingInfo computeTntIncomingAndShield(EntityTNTPrimed tnt) {
        if (mc.theWorld == null || mc.thePlayer == null || tnt == null) return null;

        final EntityLivingBase player = mc.thePlayer;

        // 1) 取来向：优先用 TNT 速度（更真实），速度过小则用位置差向量
        Vec3 v = new Vec3(tnt.motionX, 0.0, tnt.motionZ);
        if (v.lengthVector() < 1.0E-3) {
            v = new Vec3(player.posX - tnt.posX, 0.0, player.posZ - tnt.posZ);
        }
        Vec3 incoming = normalizeHorizontal(v); // TNT -> Player 的水平单位向量

        // 2) 由来向算 yaw（面向来向）；反向 yaw = yaw + 180 并规范化
        float yaw = yawFromDir(incoming);
        float reversedYaw = normalizeYaw(yaw + 180.0f);

        // 3) 生成可挡位：在玩家附近，沿“来向的反方向”推若干格，挑可放置点（两层）
        List<BlockPos> shield = gatherShieldPositions(incoming);

        return new TntIncomingInfo(incoming, yaw, reversedYaw, shield);
    }

    private static Vec3 normalizeHorizontal(Vec3 v) {
        double len = Math.sqrt(v.xCoord * v.xCoord + v.zCoord * v.zCoord);
        return (len > 1.0E-7) ? new Vec3(v.xCoord / len, 0.0, v.zCoord / len) : new Vec3(0, 0, 0);
    }

    /** 经典 MC 1.8 计算：x/z -> yaw（面向该向量）*/
    private static float yawFromDir(Vec3 dir) {
        // 玩家 yaw 定义：0=南(+Z)，±180=北(-Z)，-90=东(+X)，+90=西(-X)
        // 用 atan2(z, x) * 180 / PI - 90
        double yaw = Math.toDegrees(Math.atan2(dir.zCoord, dir.xCoord)) - 90.0;
        return normalizeYaw((float) yaw);
    }

    private static float normalizeYaw(float yaw) {
        yaw %= 360.0f;
        if (yaw >= 180.0f) yaw -= 360.0f;
        if (yaw < -180.0f) yaw += 360.0f;
        return yaw;
    }

    /** TNT 与玩家 3D 距离 ≤ range 时返回 true（用平方距离避免 sqrt） */
    public static boolean isTntWithin(EntityTNTPrimed tnt, double range) {
        if (mc.theWorld == null || mc.thePlayer == null || tnt == null || tnt.isDead) return false;
        double r2 = range * range;
        return mc.thePlayer.getDistanceSqToEntity(tnt) <= r2;
    }

    /** 水平距离 ≤ rangeHz 且 |dy| ≤ maxDY 时返回 true（更贴近 PvP 实战） */
    public static boolean isTntWithinHorizontal(EntityTNTPrimed tnt, double rangeHz, double maxDY) {
        if (mc.theWorld == null || mc.thePlayer == null || tnt == null || tnt.isDead) return false;

        double dx = tnt.posX - mc.thePlayer.posX;
        double dz = tnt.posZ - mc.thePlayer.posZ;
        double dy = Math.abs((tnt.posY + tnt.height * 0.5) - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight()));

        double hz2 = dx * dx + dz * dz;
        double r2  = rangeHz * rangeHz;

        return hz2 <= r2 && dy <= maxDY;
    }

    /** 距离在 [minRange, maxRange] 区间内返回 true（区间触发） */
    public static boolean isTntBetween(EntityTNTPrimed tnt, double minRange, double maxRange) {
        if (mc.theWorld == null || mc.thePlayer == null || tnt == null || tnt.isDead) return false;

        double d2 = mc.thePlayer.getDistanceSqToEntity(tnt);
        double min2 = minRange * minRange;
        double max2 = maxRange * maxRange;

        return d2 >= min2 && d2 <= max2;
    }


    /** 生成玩家附近用于“顶挡”的候选点，按近→远排序 */
    private static List<BlockPos> gatherShieldPositions(Vec3 incoming) {
        List<BlockPos> result = new ArrayList<>();
        if (mc.theWorld == null || mc.thePlayer == null) return result;

        // 我们想在“来向的反方向（也就是把 TNT 推来的方向挡住）”放方块
        // 反方向向量：
        Vec3 away = new Vec3(-incoming.xCoord, 0.0, -incoming.zCoord);

        BlockPos base = new BlockPos(mc.thePlayer); // 脚下格
        // r = 1~3：越靠近玩家优先级越高
        for (int r = 1; r <= 3; r++) {
            int offX = (int) Math.round(away.xCoord * r);
            int offZ = (int) Math.round(away.zCoord * r);

            // 两层（脚下一层与头上一层），更稳
            for (int dy = 0; dy <= 1; dy++) {
                BlockPos pos = base.add(offX, dy, offZ);
                if (isReplaceable(pos) && hasSolidNeighbor(pos)) {
                    result.add(pos);
                }
            }

            // 同时考虑左右微偏移，增强贴脸覆盖（±90° 侧偏）
            int sideX = (int) Math.round(-away.zCoord); // 旋转 90°
            int sideZ = (int) Math.round(away.xCoord);

            for (int side = -1; side <= 1; side += 2) {
                int sx = offX + side * sideX;
                int sz = offZ + side * sideZ;
                for (int dy = 0; dy <= 1; dy++) {
                    BlockPos pos = base.add(sx, dy, sz);
                    if (isReplaceable(pos) && hasSolidNeighbor(pos)) {
                        result.add(pos);
                    }
                }
            }
        }

        // 去重（可能重合）
        List<BlockPos> dedup = new ArrayList<>();
        for (BlockPos p : result) if (!dedup.contains(p)) dedup.add(p);
        return dedup;
    }

}
