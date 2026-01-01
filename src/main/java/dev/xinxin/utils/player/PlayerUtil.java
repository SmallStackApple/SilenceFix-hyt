package dev.xinxin.utils.player;

import dev.xinxin.SilenceFix;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.potion.Potion;
import net.minecraft.util.*;
import net.minecraft.world.World;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Comparator;

import static dev.xinxin.utils.misc.MinecraftInstance.mc;
import static dev.xinxin.utils.player.SlotUtil.canHeldItemHarvest;
import static dev.xinxin.utils.player.SlotUtil.getToolDigEfficiency;

public final class PlayerUtil {
    public static int getSpeedPotion() {
        return SilenceFix.mc.thePlayer.isPotionActive(Potion.moveSpeed) ? SilenceFix.mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1 : 0;
    }

    public static boolean isVoid() {
        for (double posY = SilenceFix.mc.thePlayer.posY; posY > 0.0; posY -= 1.0) {
            if (SilenceFix.mc.theWorld.getBlockState(new BlockPos(SilenceFix.mc.thePlayer.posX, posY, SilenceFix.mc.thePlayer.posZ)).getBlock() instanceof BlockAir) continue;
            return false;
        }
        return true;
    }

    public static float getPlayerRelativeBlockHardness(EntityPlayer playerIn, World worldIn, BlockPos pos, int slot) {
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        float f = block.getBlockHardness(worldIn, pos);
        return f < 0.0f ? 0.0f : (!canHeldItemHarvest(block, slot) ? getToolDigEfficiency(block, slot) / f / 100.0f : getToolDigEfficiency(block, slot) / f / 30.0f);
    }

    public static boolean isSafeLandingPosition(Vec3 landingPos) {
        BlockPos blockBelow = new BlockPos(landingPos.xCoord, landingPos.yCoord - 1, landingPos.zCoord);
        Block block = mc.theWorld.getBlockState(blockBelow).getBlock();
        return block.getMaterial().isSolid();
    }

    public static boolean isOverVoid(EntityPlayer player, double checkDepth) {
        for (double y = player.posY; y > 0; y -= 1.0) {
            BlockPos pos = new BlockPos(player.posX, y, player.posZ);
            if (!player.worldObj.isAirBlock(pos)) {
                return false; // 检测到非空气方块，不是虚空
            }
        }
        return true; // 一直检测到世界底部都是空气，判定为虚空
    }

    public static boolean canBeSeen(Entity e) {
        Vec3 vec1 = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);

        AxisAlignedBB box = e.getEntityBoundingBox();
        Vec3 vec2 = new Vec3(e.posX, e.posY + (e.getEyeHeight() / 1.32F), e.posZ);
        double minx = e.posX - 0.3;
        double maxx = e.posX + 0.3;
        double miny = e.posY;
        double maxy = e.posY + Math.abs(e.posY - box.maxY);
        double minz = e.posZ - 0.3;
        double maxz = e.posZ + 0.3;
        boolean see = mc.theWorld.rayTraceBlocks(vec1, vec2) == null;
        if (see)
            return true;
        vec2 = new Vec3(maxx, miny, minz);
        see = mc.theWorld.rayTraceBlocks(vec1, vec2) == null;
        if (see)
            return true;
        vec2 = new Vec3(minx, miny, minz);
        see = mc.theWorld.rayTraceBlocks(vec1, vec2) == null;

        if (see)
            return true;
        vec2 = new Vec3(minx, miny, maxz);
        see = mc.theWorld.rayTraceBlocks(vec1, vec2) == null;
        if (see)
            return true;
        vec2 = new Vec3(maxx, miny, maxz);
        see = mc.theWorld.rayTraceBlocks(vec1, vec2) == null;
        if (see)
            return true;

        vec2 = new Vec3(maxx, maxy, minz);
        see = mc.theWorld.rayTraceBlocks(vec1, vec2) == null;

        if (see)
            return true;
        vec2 = new Vec3(minx, maxy, minz);

        see = mc.theWorld.rayTraceBlocks(vec1, vec2) == null;
        if (see)
            return true;
        vec2 = new Vec3(minx, maxy, maxz - 0.1);
        see = mc.theWorld.rayTraceBlocks(vec1, vec2) == null;
        if (see)
            return true;
        vec2 = new Vec3(maxx, maxy, maxz);
        see = mc.theWorld.rayTraceBlocks(vec1, vec2) == null;
        return see;
    }
    public static boolean isHoldingPotionAndSword(ItemStack stack, boolean checkSword, boolean checkPotionFood) {
        if (stack == null) {
            return false;
        }
        if (stack.getItem() instanceof ItemAppleGold && checkPotionFood) {
            return true;
        }
        if (stack.getItem() instanceof ItemPotion && checkPotionFood) {
            return !ItemPotion.isSplash(stack.getMetadata());
        }
        if (stack.getItem() instanceof ItemFood && checkPotionFood) {
            return true;
        }
        if (stack.getItem() instanceof ItemSword && checkSword) {
            return true;
        }
        if (stack.getItem() instanceof ItemBow) {
            return checkPotionFood;
        }
        return stack.getItem() instanceof ItemBucketMilk && checkPotionFood;
    }

    public static boolean isBlockBlacklisted(Item item) {
        return item instanceof ItemAnvilBlock || item.getUnlocalizedName().contains("sand") || item.getUnlocalizedName().contains("gravel") || item.getUnlocalizedName().contains("ladder") || item.getUnlocalizedName().contains("tnt") || item.getUnlocalizedName().contains("chest") || item.getUnlocalizedName().contains("web");
    }

    public static BlockPos getBlockCorner(BlockPos start, BlockPos end) {
        for (int x2 = 0; x2 <= 1; ++x2) {
            for (int y2 = 0; y2 <= 1; ++y2) {
                for (int z = 0; z <= 1; ++z) {
                    BlockPos pos = new BlockPos(end.getX() + x2, end.getY() + y2, end.getZ() + z);
                    if (PlayerUtil.isBlockBetween(start, pos)) continue;
                    return pos;
                }
            }
        }
        return null;
    }

    public static boolean isBlockBetween(BlockPos start, BlockPos end) {
        int startX = start.getX();
        int startY = start.getY();
        int startZ = start.getZ();
        int endX = end.getX();
        int endY = end.getY();
        int endZ = end.getZ();
        double diffX = endX - startX;
        double diffY = endY - startY;
        double diffZ = endZ - startZ;
        double x2 = startX;
        double y2 = startY;
        double z = startZ;
        double STEP = 0.1;
        int STEPS = (int)Math.max(Math.abs(diffX), Math.max(Math.abs(diffY), Math.abs(diffZ))) * 4;
        for (int i = 0; i < STEPS - 1; ++i) {
            BlockPos pos;
            Block block;
            if ((x2 += diffX / (double)STEPS) == (double)endX && (y2 += diffY / (double)STEPS) == (double)endY && (z += diffZ / (double)STEPS) == (double)endZ || (block = SilenceFix.mc.theWorld.getBlockState(pos = new BlockPos(x2, y2, z)).getBlock()).getMaterial() == Material.air || block.getMaterial() == Material.water || block instanceof BlockVine || block instanceof BlockLadder) continue;
            return true;
        }
        return false;
    }

    public static float getMoveYaw(float yaw) {
        Vector2f from = new Vector2f((float) SilenceFix.mc.thePlayer.lastTickPosX, (float) SilenceFix.mc.thePlayer.lastTickPosZ);
        Vector2f to = new Vector2f((float) SilenceFix.mc.thePlayer.posX, (float) SilenceFix.mc.thePlayer.posZ);
        Vector2f diff = new Vector2f(to.x - from.x, to.y - from.y);
        double x2 = diff.x;
        double z = diff.y;
        if (x2 != 0.0 && z != 0.0) {
            yaw = (float)Math.toDegrees((Math.atan2(-x2, z) + (double)MathHelper.PI2) % (double)MathHelper.PI2);
        }
        return yaw;
    }

    public static boolean isBlockUnder(double height) {
        return PlayerUtil.isBlockUnder(height, true);
    }

    public static boolean isBlockUnder(double height, boolean boundingBox) {
        if (boundingBox) {
            int offset = 0;
            while ((double)offset < height) {
                AxisAlignedBB bb = SilenceFix.mc.thePlayer.getEntityBoundingBox().offset(0.0, -offset, 0.0);
                if (!SilenceFix.mc.theWorld.getCollidingBoundingBoxes(SilenceFix.mc.thePlayer, bb).isEmpty()) {
                    return true;
                }
                offset += 2;
            }
        } else {
            int offset = 0;
            while ((double)offset < height) {
                if (PlayerUtil.blockRelativeToPlayer(0.0, -offset, 0.0).isFullBlock()) {
                    return true;
                }
                ++offset;
            }
        }
        return false;
    }

    public static Block blockRelativeToPlayer(double offsetX, double offsetY, double offsetZ) {
        return SilenceFix.mc.theWorld.getBlockState(new BlockPos(SilenceFix.mc.thePlayer).add(offsetX, offsetY, offsetZ)).getBlock();
    }

    public static Block block(double x2, double y2, double z) {
        return SilenceFix.mc.theWorld.getBlockState(new BlockPos(x2, y2, z)).getBlock();
    }

    public static EnumFacingOffset getEnumFacing(Vec3 position) {
        for (int x2 = -1; x2 <= 1; x2 += 2) {
            if (PlayerUtil.block(position.xCoord + (double)x2, position.yCoord, position.zCoord) instanceof BlockAir) continue;
            if (x2 > 0) {
                return new EnumFacingOffset(EnumFacing.WEST, new Vec3(x2, 0.0, 0.0));
            }
            return new EnumFacingOffset(EnumFacing.EAST, new Vec3(x2, 0.0, 0.0));
        }
        for (int y2 = -1; y2 <= 1; y2 += 2) {
            if (PlayerUtil.block(position.xCoord, position.yCoord + (double)y2, position.zCoord) instanceof BlockAir || y2 >= 0) continue;
            return new EnumFacingOffset(EnumFacing.UP, new Vec3(0.0, y2, 0.0));
        }
        for (int z2 = -1; z2 <= 1; z2 += 2) {
            if (PlayerUtil.block(position.xCoord, position.yCoord, position.zCoord + (double)z2) instanceof BlockAir) continue;
            if (z2 < 0) {
                return new EnumFacingOffset(EnumFacing.SOUTH, new Vec3(0.0, 0.0, z2));
            }
            return new EnumFacingOffset(EnumFacing.NORTH, new Vec3(0.0, 0.0, z2));
        }
        return null;
    }

    public static Vec3 getPlacePossibility(double offsetX, double offsetY, double offsetZ, int range) {
        ArrayList<Vec3> possibilities = new ArrayList<Vec3>();
        for (int x2 = -range; x2 <= range; ++x2) {
            for (int y2 = -range; y2 <= range; ++y2) {
                for (int z = -range; z <= range; ++z) {
                    Block block = PlayerUtil.blockRelativeToPlayer(x2, y2, z);
                    if (block instanceof BlockAir) continue;
                    for (int x22 = -1; x22 <= 1; x22 += 2) {
                        possibilities.add(new Vec3(SilenceFix.mc.thePlayer.posX + (double)x2 + (double)x22, SilenceFix.mc.thePlayer.posY + (double)y2, SilenceFix.mc.thePlayer.posZ + (double)z));
                    }
                    for (int y22 = -1; y22 <= 1; y22 += 2) {
                        possibilities.add(new Vec3(SilenceFix.mc.thePlayer.posX + (double)x2, SilenceFix.mc.thePlayer.posY + (double)y2 + (double)y22, SilenceFix.mc.thePlayer.posZ + (double)z));
                    }
                    for (int z2 = -1; z2 <= 1; z2 += 2) {
                        possibilities.add(new Vec3(SilenceFix.mc.thePlayer.posX + (double)x2, SilenceFix.mc.thePlayer.posY + (double)y2, SilenceFix.mc.thePlayer.posZ + (double)z + (double)z2));
                    }
                }
            }
        }
        possibilities.removeIf(vec3 -> SilenceFix.mc.thePlayer.getDistance(vec3.xCoord, vec3.yCoord, vec3.zCoord) > 5.0 || !(PlayerUtil.block(vec3.xCoord, vec3.yCoord, vec3.zCoord) instanceof BlockAir));
        if (possibilities.isEmpty()) {
            return null;
        }
        possibilities.sort(Comparator.comparingDouble(vec3 -> {
            double d0 = offsetX - vec3.xCoord;
            double d1 = offsetY - vec3.yCoord;
            double d2 = offsetZ - vec3.zCoord;
            return MathHelper.sqrt_double(d0 * d0 + d1 * d1 + d2 * d2);
        }));
        return (Vec3)possibilities.get(0);
    }

    public static int findSoup() {
        for (int i = 36; i < 45; ++i) {
            ItemStack itemStack = SilenceFix.mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (itemStack == null || !itemStack.getItem().equals(Items.mushroom_stew) || itemStack.stackSize <= 0 || !(itemStack.getItem() instanceof ItemFood)) continue;
            return i;
        }
        return -1;
    }

    public static int findItem(int startSlot, int endSlot, Item item) {
        for (int i = startSlot; i < endSlot; ++i) {
            ItemStack stack = SilenceFix.mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack == null || stack.getItem() != item) continue;
            return i;
        }
        return -1;
    }

    public static boolean hasSpaceHotbar() {
        for (int i = 36; i < 45; ++i) {
            ItemStack itemStack = SilenceFix.mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (itemStack != null) continue;
            return true;
        }
        return false;
    }

    public static boolean isOnGround(double height) {
        return !SilenceFix.mc.theWorld.getCollidingBoundingBoxes(SilenceFix.mc.thePlayer, SilenceFix.mc.thePlayer.getEntityBoundingBox().offset(0.0, -height, 0.0)).isEmpty();
    }

    public static Block getBlock(BlockPos pos) {
        return SilenceFix.mc.theWorld.getBlockState(pos).getBlock();
    }

    public static Block getBlock(double d2, double d22, double d3) {
        return SilenceFix.mc.theWorld.getBlockState(new BlockPos(d2, d22, d3)).getBlock();
    }

    public static boolean isOnGround(Entity entity, double height) {
        return !SilenceFix.mc.theWorld.getCollidingBoundingBoxes(entity, entity.getEntityBoundingBox().offset(0.0, -height, 0.0)).isEmpty();
    }

    public static boolean colorTeam(EntityPlayer sb) {
        String targetName = sb.getDisplayName().getFormattedText().replace("§r", "");
        String clientName = SilenceFix.mc.thePlayer.getDisplayName().getFormattedText().replace("§r", "");
        return targetName.startsWith("§" + clientName.charAt(1));
    }

    public static boolean armorTeam(EntityPlayer entityPlayer) {
        if (SilenceFix.mc.thePlayer.inventory.armorInventory[3] != null && entityPlayer.inventory.armorInventory[3] != null) {
            ItemStack myHead = SilenceFix.mc.thePlayer.inventory.armorInventory[3];
            ItemArmor myItemArmor = (ItemArmor)myHead.getItem();
            ItemStack entityHead = entityPlayer.inventory.armorInventory[3];
            ItemArmor entityItemArmor = (ItemArmor)entityHead.getItem();
            if (String.valueOf(entityItemArmor.getColor(entityHead)).equals("10511680")) {
                return true;
            }
            return myItemArmor.getColor(myHead) == entityItemArmor.getColor(entityHead);
        }
        return false;
    }

    public static boolean scoreTeam(EntityPlayer entityPlayer) {
        return SilenceFix.mc.thePlayer.isOnSameTeam(entityPlayer);
    }

    public static boolean MovementInput() {
        return SilenceFix.mc.gameSettings.keyBindForward.isKeyDown() || SilenceFix.mc.gameSettings.keyBindLeft.isKeyDown() || SilenceFix.mc.gameSettings.keyBindRight.isKeyDown() || SilenceFix.mc.gameSettings.keyBindBack.isKeyDown();
    }

    public static boolean isInLiquid() {
        if (SilenceFix.mc.thePlayer.isInWater()) {
            return true;
        }
        boolean inLiquid = false;
        int y2 = (int) SilenceFix.mc.thePlayer.getEntityBoundingBox().minY;
        for (int x2 = MathHelper.floor_double(SilenceFix.mc.thePlayer.getEntityBoundingBox().minX); x2 < MathHelper.floor_double(SilenceFix.mc.thePlayer.getEntityBoundingBox().maxX) + 1; ++x2) {
            for (int z = MathHelper.floor_double(SilenceFix.mc.thePlayer.getEntityBoundingBox().minZ); z < MathHelper.floor_double(SilenceFix.mc.thePlayer.getEntityBoundingBox().maxZ) + 1; ++z) {
                Block block = SilenceFix.mc.theWorld.getBlockState(new BlockPos(x2, y2, z)).getBlock();
                if (block == null || block.getMaterial() == Material.air) continue;
                if (!(block instanceof BlockLiquid)) {
                    return false;
                }
                inLiquid = true;
            }
        }
        return inLiquid;
    }

    public static boolean isOnLiquid() {
        AxisAlignedBB boundingBox = SilenceFix.mc.thePlayer.getEntityBoundingBox();
        if (boundingBox == null) {
            return false;
        }
        boundingBox = boundingBox.contract(0.01, 0.0, 0.01).offset(0.0, -0.01, 0.0);
        boolean onLiquid = false;
        int y2 = (int)boundingBox.minY;
        for (int x2 = MathHelper.floor_double(boundingBox.minX); x2 < MathHelper.floor_double(boundingBox.maxX + 1.0); ++x2) {
            for (int z = MathHelper.floor_double(boundingBox.minZ); z < MathHelper.floor_double(boundingBox.maxZ + 1.0); ++z) {
                Block block = SilenceFix.mc.theWorld.getBlockState(new BlockPos(x2, y2, z)).getBlock();
                if (block == Blocks.air) continue;
                if (!(block instanceof BlockLiquid)) {
                    return false;
                }
                onLiquid = true;
            }
        }
        return onLiquid;
    }

    private PlayerUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}

