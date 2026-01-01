package dev.xinxin.module.modules.render;


import dev.xinxin.event.EventTarget;
import dev.xinxin.event.rendering.EventRender3D;
import dev.xinxin.event.world.EventUpdate;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.values.BoolValue;
import dev.xinxin.module.values.ColorValue;
import dev.xinxin.module.values.ModeValue;
import dev.xinxin.module.values.NumberValue;
import dev.xinxin.utils.TimerUtil;
import dev.xinxin.utils.player.BlockUtil;
import dev.xinxin.utils.render.ColorUtil;
import dev.xinxin.utils.render.RenderUtil;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class BlockESP extends Module {
    public BlockESP() {
        super("BlockESP", Category.Render,"方块显示");
    }
    private final ModeValue<renderMode> modeValue = new ModeValue("Mode", (Enum[])renderMode.values(), (Enum)renderMode.Box);
    public enum renderMode {
        Box,
        TwoD,
        Outline;
    }
    private final NumberValue radiusValue = new NumberValue("Radius", 40, 5, 120, 1);
    public ColorValue renderColor = new ColorValue("RenderColor", Color.WHITE.getRGB());
    private final BoolValue colorRainbow = new BoolValue("Rainbow", false);
    private final TimerUtil searchTimer = new TimerUtil();
    private final List<BlockPos> posList = new ArrayList<>();
    private Thread thread;



    @EventTarget
    public void onUpdate(EventUpdate event){
        if (searchTimer.delay(1000L) && (thread == null || !thread.isAlive())) {
            final int radius = radiusValue.getValue().intValue();
            final Block selectedBlock = Block.getBlockById(26);

            if (selectedBlock == Blocks.air)
                return;

            thread = new Thread(() -> {
                final List<BlockPos> blockList = new ArrayList<>();

                for (int x = -radius; x < radius; x++) {
                    for (int y = radius; y > -radius; y--) {
                        for (int z = -radius; z < radius; z++) {
                            final int xPos = ((int) mc.thePlayer.posX + x);
                            final int yPos = ((int) mc.thePlayer.posY + y);
                            final int zPos = ((int) mc.thePlayer.posZ + z);

                            final BlockPos blockPos = new BlockPos(xPos, yPos, zPos);
                            final Block block = BlockUtil.getBlock(blockPos);
                            if (block == selectedBlock)
                                blockList.add(blockPos);
                        }
                    }
                }

                searchTimer.reset();

                synchronized (posList) {
                    posList.clear();
                    posList.addAll(blockList);
                }
            }, "BlockESP-BlockFinder");
            thread.start();
        }
    }

    @EventTarget
    public void onRender3D(EventRender3D event){
        synchronized (posList) {
            final Color color = colorRainbow.getValue() ? ColorUtil.rainbow() : RenderUtil.getColor(renderColor.getValue());

            for (final BlockPos blockPos : posList) {
                if (modeValue.is("Box")) {
                    RenderUtil.drawBlockBox(blockPos, color, false);
                } else if (modeValue.is("TwoD")) {
                    RenderUtil.draw2D(blockPos, color.getRGB(), Color.BLACK.getRGB());
                } else if (modeValue.is("Outline")) {
                    RenderUtil.drawBlockBox(blockPos, color, false);
                    RenderUtil.renderOne();
                    RenderUtil.drawBlockBox(blockPos, color, false);
                    RenderUtil.renderTwo();
                    RenderUtil.drawBlockBox(blockPos, color, false);
                    RenderUtil.renderThree();
                    RenderUtil.renderFour(color.getRGB());
                    RenderUtil.drawBlockBox(blockPos, color, true);
                    RenderUtil.renderFive();
                }
            }
        }
    }
}
