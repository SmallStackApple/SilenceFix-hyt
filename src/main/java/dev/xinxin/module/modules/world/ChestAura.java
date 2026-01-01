package dev.xinxin.module.modules.world;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.rendering.EventRender3D;
import dev.xinxin.event.world.EventUpdate;
import dev.xinxin.event.world.EventWorldLoad;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.modules.combat.AutoProjectile;
import dev.xinxin.module.modules.combat.KillAura;
import dev.xinxin.module.modules.misc.AutoGapple;
import dev.xinxin.module.modules.misc.AutoReport;
import dev.xinxin.module.modules.player.AutoPotion;
import dev.xinxin.module.modules.player.Blink;
import dev.xinxin.module.modules.player.ChestStealer;
import dev.xinxin.module.modules.player.InvCleaner;
import dev.xinxin.module.values.BoolValue;
import dev.xinxin.module.values.NumberValue;
import dev.xinxin.utils.Rotation;
import dev.xinxin.utils.RotationComponent;
import dev.xinxin.utils.client.StopWatch;
import dev.xinxin.utils.player.RotationUtil;
import dev.xinxin.utils.render.RenderUtil;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.inventory.GuiBrewingStand;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiFurnace;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemSword;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class ChestAura extends Module {
    private final NumberValue range = new NumberValue("Range", 3.0, 1.0, 7.0, 0.1);
    public final BoolValue thoughtWall = new BoolValue("ThoughtWall", false);
    public Rotation needRot;
    public boolean working = false;
    private EnumFacing needFacing;
    private final StopWatch verifyTimer = new StopWatch();
    private BlockPos lastOpenedContainer = null;
    private final StopWatch msTimer = new StopWatch();

    public static Set<BlockPos> confirmedOpenedContainers = new HashSet<>();

    public ChestAura() {
        super("ChestAura", Category.Player, "箱子光环","Automatically opens chests around you.","自动开启邻近容器。");
    }

    @Override
    public void onDisable() {
        confirmedOpenedContainers.clear();
        lastOpenedContainer = null;
        super.onDisable();
    }

    @EventTarget
    public void onPre(EventUpdate e) {
        needRot = null;
        if (mc.thePlayer.isUsingItem() &&
                mc.thePlayer.getHeldItem() != null &&
                mc.thePlayer.getHeldItem().getItem() instanceof ItemSword) {
            return;
        }

        if (AutoProjectile.isThrowing) return;
        if (getModule(AutoPotion.class).isThrowing()) return;
        if (mc.thePlayer.isUsingItem() && mc.thePlayer.getHeldItem() != null &&
                mc.thePlayer.getHeldItem().getItem() instanceof ItemBow) {
            return;
        }
        if (getModule(Scaffold.class).getState()
                || mc.currentScreen != null
                || getModule(Blink.class).getState()
                || getModule(AutoGapple.class).getState()
                || AutoReport.currentTarget != null
                || !KillAura.targets.isEmpty()) {
            return;
        }
        if (lastOpenedContainer != null && verifyTimer.finished(500L)) {
            if (!isContainerActuallyOpen(lastOpenedContainer)) {
                confirmedOpenedContainers.remove(lastOpenedContainer);
            }
            lastOpenedContainer = null;
        }
        if (ChestStealer.isStealing){
            return;
        }


        BlockPos nearestContainer = findNearestContainer();
        if (nearestContainer == null || !msTimer.finished(300L)) return;

        if (RotationComponent.lastRotation == null) {
            RotationComponent.lastRotation = new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        }
        working = true;
        RotationComponent.setRotation(needRot, 180f, true);

        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.inventory.getCurrentItem(),
                nearestContainer, needFacing, get_vec_position(nearestContainer, needFacing))) {

            lastOpenedContainer = nearestContainer;
            verifyTimer.reset();
            ChestStealer.currentContainerPos = nearestContainer;
            ChestStealer.working = true;
            ChestStealer.isStealing = true;
            ChestStealer.stealCompleteTimer.reset();

            InvCleaner invCleaner = getModule(InvCleaner.class);
            if (invCleaner != null) {
                invCleaner.chestOpenTimer.reset();
            }
        }
        if (lastOpenedContainer != null && mc.currentScreen instanceof GuiChest) {
            confirmedOpenedContainers.add(lastOpenedContainer);
            lastOpenedContainer = null;
        }

        msTimer.reset();
    }



    private BlockPos findNearestContainer() {
        BlockPos nearestContainer = null;
        double nearestDistance = Double.MAX_VALUE;

        for (float y = range.getValue().floatValue(); y >= -range.getValue().floatValue(); y -= 1.0f) {
            for (float x = -range.getValue().floatValue(); x <= range.getValue().floatValue(); x += 1.0f) {
                for (float z = -range.getValue().floatValue(); z <= range.getValue().floatValue(); z += 1.0f) {
                    BlockPos pos = new BlockPos(mc.thePlayer.posX + x, mc.thePlayer.posY + y, mc.thePlayer.posZ + z);
                    if (checkContainerOpenable(pos)) {
                        MovingObjectPosition mop = mc.theWorld.rayTraceBlocks(
                                new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ),
                                new Vec3(pos).add(new Vec3(0.5, 0, 0.5)),
                                false, true, true);

                        double dist = mc.thePlayer.getDistance(pos.getX(), pos.getY(), pos.getZ());
                        if (dist < 4.5 && !confirmedOpenedContainers.contains(pos) &&
                                dist <= nearestDistance && isContainer(mc.theWorld.getBlockState(pos).getBlock()) &&
                                mop != null && (mc.theWorld.getBlockState(pos).getBlock() instanceof BlockBrewingStand
                                || mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) &&
                                (mop.getBlockPos().equals(pos) || thoughtWall.getValue())) {
                            nearestDistance = dist;
                            nearestContainer = pos;
                            float[] r = RotationUtil.getRotationBlock(pos);
                            needRot = new Rotation(r[0], r[1]);
                            needFacing = mop.sideHit;
                        }
                    }
                }
            }
        }
        return nearestContainer;
    }

    private boolean isContainerActuallyOpen(BlockPos pos) {
        if (mc.currentScreen != null && pos.equals(lastOpenedContainer)) {
            return true;
        }
        if (ChestStealer.working && pos.equals(ChestStealer.currentContainerPos)) {
            return true;
        }
        if (mc.currentScreen instanceof GuiChest) {
            TileEntity tileEntity = (TileEntity) ((GuiChest) mc.currentScreen).lowerChestInventory;
            if (tileEntity != null && pos.equals(tileEntity.getPos())) {
                return true;
            }
        }
        if (mc.currentScreen instanceof GuiFurnace) {
            TileEntity tileEntity = (TileEntity) ((GuiFurnace) mc.currentScreen).tileFurnace;
            if (tileEntity != null && pos.equals(tileEntity.getPos())) {
                return true;
            }
        }
        if (mc.currentScreen instanceof GuiBrewingStand) {
            TileEntity tileEntity = (TileEntity) ((GuiBrewingStand) mc.currentScreen).tileBrewingStand;
            if (tileEntity != null && pos.equals(tileEntity.getPos())) {
                return true;
            }
        }
        return confirmedOpenedContainers.contains(pos);
    }

    private boolean isContainer(Block block) {
        return block instanceof BlockChest || block instanceof BlockFurnace || block instanceof BlockBrewingStand;
    }

    private boolean checkContainerOpenable(BlockPos blockPos) {
        IBlockState blockState = mc.theWorld.getBlockState(blockPos);
        if (blockState.getBlock() instanceof BlockBrewingStand) return true;
        if (!(blockState.getBlock() instanceof BlockChest)) return true;
        IBlockState upBlockState = mc.theWorld.getBlockState(blockPos.add(0, 1, 0));
        return !upBlockState.getBlock().isFullBlock() || upBlockState.getBlock() instanceof BlockGlass;
    }

    @EventTarget
    public void onWorld(EventWorldLoad e) {
        confirmedOpenedContainers.clear();
        lastOpenedContainer = null;
    }

    @EventTarget
    public void onRender(EventRender3D event) {
        for (TileEntity tileEntity : mc.theWorld.loadedTileEntityList) {
            if (!(tileEntity instanceof TileEntityChest ||
                    tileEntity instanceof TileEntityFurnace ||
                    tileEntity instanceof TileEntityBrewingStand)) {
                continue;
            }

            BlockPos pos = tileEntity.getPos();
            Color color;
            if (mc.currentScreen instanceof GuiChest && pos.equals(ChestStealer.currentContainerPos)) {
                color = new Color(6, 226, 246, 132);
            } else if (confirmedOpenedContainers.contains(pos)) {
                color = new Color(241, 9, 36, 132);
            } else {
                color = new Color(239, 239, 238, 132);
            }

            if (mc.thePlayer.getDistance(pos.getX(), pos.getY(), pos.getZ()) < 20.0) {
                RenderUtil.drawBlockBox(pos, color, false);
            }
        }
    }


    public static Vec3 get_vec_position(BlockPos pos, EnumFacing face) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        switch (face) {
            case NORTH: z -= 0.5; break;
            case SOUTH: z += 0.5; break;
            case EAST:  x += 0.5; break;
            case WEST:  x -= 0.5; break;
            case UP:    y += 0.5; break;
            case DOWN:  y -= 0.5; break;
        }
        return new Vec3(x, y, z);
    }
}
