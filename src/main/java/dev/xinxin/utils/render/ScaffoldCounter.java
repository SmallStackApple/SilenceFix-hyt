package dev.xinxin.utils.render;

import dev.xinxin.SilenceFix;
import dev.xinxin.module.ModuleManager;
import dev.xinxin.module.modules.world.Scaffold;
import dev.xinxin.utils.render.animation.Animation;
import dev.xinxin.utils.render.animation.Direction;
import dev.xinxin.utils.render.animation.impl.EaseBackIn;
import dev.xinxin.utils.render.fontRender.FontManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Arrays;

public class ScaffoldCounter {
    private static final Animation introAnimation = new EaseBackIn(300, 1.0, 1.2f);
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static float dragX = -1, dragY = -1;
    private static float offsetX = 0, offsetY = 120; // 默认在准星下方120像素
    private static boolean isDragging = false;

    public static void drawDefault(float centerX,float y,float a,float sf) {
        Scaffold scaffoldModule = ModuleManager.getModule(Scaffold.class);
        if (scaffoldModule != null && !scaffoldModule.getState()) return;
        int alpha = (int) a;
        final int barHeight = Math.round(22);
        final float radius  = 6f;
        introAnimation.setDirection(Direction.FORWARDS);
        GL11.glPushMatrix();
        int blockCount = 0;
        if (scaffoldModule != null) {
            blockCount = calculateBlockCount(scaffoldModule);
        }
        String text = "剩余: " + blockCount;
        ItemStack currentBlock = getCurrentBlockStack(scaffoldModule);
        boolean hasValidBlock = false;
        if (scaffoldModule != null) {
            hasValidBlock = currentBlock.getItem() instanceof ItemBlock && scaffoldModule.isValid(currentBlock.getItem());
        }
        float textWidth = FontManager.arial18.getStringWidth(text);
        float bgWidth = Math.max(80f, textWidth + (hasValidBlock ? 30f : 15f));
//        bgWidth = Math.round(bgWidth * sf);

        float x = centerX - bgWidth / 2f;
        y = y - 10;
        handleDragging(x, y, bgWidth, barHeight);
        RoundedUtils.drawRound(x, y, bgWidth, barHeight, radius, new Color(0, 0, 0, 90 * alpha)); // 降低透明度，保持清晰但不过于沉重
        RoundedUtils.drawRoundOutline(x, y, bgWidth, barHeight, radius, 1f,
                new Color(0, 0, 0, 40 * alpha), // 更加简洁的边框颜色
                isDragging ? new Color(150, 200, 255, 150 * alpha) : new Color(80, 80, 80, 150 * alpha)); // 更柔和的高亮
        int rgbColor = new Color(255, 255, 255, 255 * alpha).getRGB(); // 使用白色为例
        FontManager.arial18.drawStringWithShadow(text,
                centerX - textWidth / 2f - (hasValidBlock ? 8f : 0f),
                y + 11f - FontManager.arial18.getHeight() / 2f,
                rgbColor); // 使用 RGB 整数值而不是 Color 对象
        if (hasValidBlock) {
            drawBlockIcon(currentBlock, centerX + textWidth / 2f + 5f, y + 3f); // 调整图标位置
        }

        GL11.glPopMatrix();
    }

    private static void handleDragging(float x, float y, float width, float height) {
        if (mc.currentScreen != null) {
            isDragging = false;
            return;
        }
        int mouseX = mc.mouseHelper.deltaX * mc.displayWidth / mc.displayWidth;
        int mouseY = mc.mouseHelper.deltaY * mc.displayHeight / mc.displayHeight;
        ScaledResolution sr = new ScaledResolution(mc);
        float scaledMouseX = mouseX / (float)sr.getScaleFactor();
        float scaledMouseY = mouseY / (float)sr.getScaleFactor();
        boolean mouseOver = scaledMouseX >= x && scaledMouseX <= x + width &&
                scaledMouseY >= y && scaledMouseY <= y + height;
        if (mouseOver && mc.gameSettings.keyBindAttack.isKeyDown() && !isDragging) {
            isDragging = true;
            dragX = scaledMouseX - offsetX;
            dragY = scaledMouseY - offsetY;
        }
        if (isDragging && mc.gameSettings.keyBindAttack.isKeyDown()) {
            offsetX = scaledMouseX - dragX;
            offsetY = scaledMouseY - dragY;
        } else {
            isDragging = false;
        }
    }

    private static void drawBlockIcon(ItemStack stack, float x, float y) {
        RoundedUtils.drawRound(x, y, 16f, 16f, 3f, new Color(30, 30, 30, 150)); // 更简洁的背景颜色
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        mc.getRenderItem().renderItemIntoGUI(stack, (int)x, (int)y);
        GlStateManager.disableBlend();
    }

    private static ItemStack getCurrentBlockStack(Scaffold scaffold) {
        try {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(scaffold.getBlockSlot());
            return stack != null ? stack : new ItemStack(Blocks.barrier);
        } catch (Exception e) {
            return new ItemStack(Blocks.barrier);
        }
    }

    private static int calculateBlockCount(Scaffold scaffold) {
        int[] blockCountArray = new int[]{scaffold.getBlockCount()};
        return Arrays.stream(blockCountArray).sum();
    }
    public static void drawDefaultBloom() {}
    public static void drawClassic() {}
    public static void drawClassicBloom() {}
}
