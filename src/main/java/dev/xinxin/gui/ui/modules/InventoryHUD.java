package dev.xinxin.gui.ui.modules;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.rendering.EventRender2D;
import dev.xinxin.event.rendering.EventShader;
import dev.xinxin.gui.ui.UiModule;
import dev.xinxin.module.modules.render.HUD;
import dev.xinxin.utils.render.RoundedUtils;
import dev.xinxin.utils.render.fontRender.FontManager;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;

import java.awt.*;

public class InventoryHUD extends UiModule {
    public InventoryHUD() {
        super("InventoryHUD", 10.0, 100.0, 180.0, 80.0);
    }

    @EventTarget
    public void onShader(EventShader e) {
        double x = this.getPosX();
        double y = this.getPosY();
        int paddingX = 6;
        int paddingY = 8 + FontManager.Tahoma18.getHeight();
        float width = 172 + paddingX * 2;
        float height = 60 + FontManager.Tahoma18.getHeight() + 16;
        RoundedUtils.drawRound((float) x, (float) y, width, height, 8.0f, new Color(31, 31, 31, 230));
        RoundedUtils.drawRoundOutline((float) x, (float) y, width, height,
                8.0f, 1.0f, new Color(0, 0, 0, 0), new Color(58, 58, 58, 160));
        drawLine(x, y + 12.0, 2.0, 12.0, HUD.color(0));
    }

    @EventTarget
    public void onRender2D(EventRender2D e) {
        double x = this.getPosX();
        double y = this.getPosY();
        int paddingX = 6;
        int paddingY = 8 + FontManager.Tahoma18.getHeight();
        float width = 172 + paddingX * 2;
        float height = 60 + FontManager.Tahoma18.getHeight() + 16;

        RoundedUtils.drawRound((float) x, (float) y, width, height, 8.0f, new Color(31, 31, 31, 230));
        drawLine(x, y + 12.0, 2.0, 12.0, HUD.color(0));
        if (HUD.langModeValue.is("English")) {
            FontManager.Tahoma18.drawStringDynamic("Inventory", x + 26.0, y + 14.0, 1, 6);
        } else {
            FontManager.chineseFont18.drawStringDynamic("背包显示", x + 26.0, y + 11.0, 1, 6);
        }
        FontManager.icon22.drawStringDynamic("a", x + 10.0, y + FontManager.icon22.getMiddleOfBox(height) + -23, 1, 6);
        ItemStack[] inventory = InventoryHUD.mc.thePlayer.inventory.mainInventory;
        boolean hasItems = false;

        for (int i = 9; i < inventory.length; ++i) {
            ItemStack stack = inventory[i];
            if (stack == null) continue;
            hasItems = true;
            int itemX = (int) x + (i - 9) % 9 * 18 + paddingX + 2;
            int itemY = (int) y + (i - 9) / 9 * 18 + paddingY + 8;
            drawItemStack(stack, itemX, itemY);
        }
        if (!hasItems) {
            String emptyText = HUD.langModeValue.is("English") ? "Empty" : "空";
            int textWidth = FontManager.chineseFont16.getStringWidth(emptyText);
            int textX = (int) x + (int) (width / 2.0f) - textWidth / 2;
            int textY = (int) y + (int) (height / 2.0f) - FontManager.chineseFont16.getHeight() / 2;
            FontManager.chineseFont16.drawStringWithShadow(emptyText, textX, textY, new Color(255, 255, 255, 200).getRGB());
        }
    }



    public void drawLine(double x, double y, double width, double height, Color color) {
        RoundedUtils.drawRound((float) x, (float) y, (float) width, (float) height,
                Math.min((float) width, (float) height) / 2, color);
    }

    private void drawItemStack(ItemStack stack, int x, int y) {
        RenderItem itemRender = mc.getRenderItem();
        GlStateManager.pushMatrix();
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableColorMaterial();
        GlStateManager.enableLighting();
        itemRender.zLevel = 200.0f;
        itemRender.renderItemAndEffectIntoGUI(stack, x, y);
        itemRender.renderItemOverlays(mc.fontRendererObj, stack, x, y);
        itemRender.zLevel = 0.0f;
        GlStateManager.popMatrix();
        GlStateManager.disableLighting();
    }
}
