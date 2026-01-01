package dev.xinxin.utils;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.xinxin.client.viaversion.vialoadingbase.ViaLoadingBase;
import com.xinxin.client.viaversion.vialoadingbase.model.ComparableProtocolVersion;
import com.xinxin.client.viaversion.viamcp.ViaMCP;
import lombok.experimental.UtilityClass;
import net.minecraft.block.Block;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;


@UtilityClass
public class BoundingBoxFixer {
    public void fixLadder(Block block, EnumFacing facing, float defaultThickness) {
        if (facing == null) {
            throw new IllegalArgumentException("Facing cannot be null");
        }

        float thicknessValue = getThicknessValue(ViaLoadingBase.getInstance().getTargetVersion(), defaultThickness);
        AxisAlignedBB bounds = calculateLadderBounds(facing, thicknessValue);
        setBlockBounds(block, bounds);
    }


    private float getThicknessValue(ComparableProtocolVersion protocolVersion, float defaultThickness) {
        switch (protocolVersion.getVersion()) {
            case ViaMCP.NATIVE_VERSION:
                return defaultThickness;
            default:
                return 0.1875F;
        }
    }

    private AxisAlignedBB calculateLadderBounds(EnumFacing facing, float thickness) {
        switch (facing) {
            case NORTH:
                return new AxisAlignedBB(0.0F, 0.0F, 1.0F - thickness, 1.0F, 1.0F, 1.0F);
            case SOUTH:
                return new AxisAlignedBB(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, thickness);
            case WEST:
                return new AxisAlignedBB(1.0F - thickness, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
            case EAST:
                return new AxisAlignedBB(0.0F, 0.0F, 0.0F, thickness, 1.0F, 1.0F);
            case UP:
            case DOWN:
                throw new IllegalArgumentException("Invalid facing for a ladder: " + facing);
            default:
                throw new IllegalArgumentException("Unexpected facing: " + facing);
        }
    }

    public AxisAlignedBB fixCarpet() {
        return new AxisAlignedBB(0.0F, 0.0F, 0.0F, 1.0F, 0.0625F, 1.0F);
    }
    public AxisAlignedBB flyLilypad(double x1, double y1, double z1, double x2, double y2, double z2, BlockPos pos) {
        ComparableProtocolVersion targetVersion = ViaLoadingBase.getInstance().getTargetVersion();
        boolean isVersionConditionMet = targetVersion.isNewerThan(ProtocolVersion.v1_8);

        if (isVersionConditionMet) {
            return new AxisAlignedBB(pos.getX() + 0.0625D, pos.getY() + 0.0D, pos.getZ() + 0.0625D, pos.getX() + 0.9375D, pos.getY() + 0.09375D, pos.getZ() + 0.9375D);
        } else {
            return new AxisAlignedBB(x1, y1, z1, x2, y2, z2);
        }
    }

    public float fixFarmland() {
        ComparableProtocolVersion targetVersion = ViaLoadingBase.getInstance().getTargetVersion();
        boolean isVersionConditionMet = targetVersion.isNewerThan(ProtocolVersion.v1_8);
        return isVersionConditionMet ? 0.9375F : 1.0F;
    }


    public float fixCollisionBorderSize() {
        ComparableProtocolVersion targetVersion = ViaLoadingBase.getInstance().getTargetVersion();
        boolean isVersionConditionMet = targetVersion.isNewerThan(ProtocolVersion.v1_8);
        return isVersionConditionMet ? 0.0F : 0.1F;
    }

    private void setBlockBounds(Block block, AxisAlignedBB bounds) {
        block.setBlockBounds(bounds);
    }
}
