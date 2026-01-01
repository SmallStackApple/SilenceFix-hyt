package dev.xinxin.gui.ui.modules;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.rendering.EventRender2D;
import dev.xinxin.event.rendering.EventShader;
import dev.xinxin.gui.ui.UiModule;
import dev.xinxin.module.modules.render.HUD;
import dev.xinxin.utils.render.fontRender.FontManager;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;

public class ArmorHud extends UiModule {
    public ArmorHud() {
        super("ArmorHud",20,20,68,42);
    }

    @EventTarget
    public void blur(EventShader event) {
        float w = 85;
        float h = 42;
        final float x = (float) this.getPosX();
        final float y = (float) this.getPosY();
    }

    @EventTarget
    public void onRender2D(final EventRender2D event) {
        this.width = 85;
        this.height = 42;

        float w = 85;
        float h = 42;
        final float x = (float) this.getPosX();
        final float y = (float) this.getPosY();
        if (HUD.langModeValue.is("English")) {
            FontManager.bold22.drawStringDynamic("Armor", x + 5f, y + 2f, 1, 6);
        } else FontManager.arial20.drawStringDynamic("装备显示", x + 5f, y + 2f, 1, 6);
        RenderItem renderItem = mc.getRenderItem();
        if (!mc.playerController.isInCreativeMode()) {
            GlStateManager.pushMatrix();
            RenderHelper.enableGUIStandardItemLighting();
            GlStateManager.enableDepth();

            double renderx = x - 7f;

            for (int index = 3; index >= 0; index--) {
                ItemStack stack = mc.thePlayer.inventory.armorInventory[index];
                if (stack != null) {
                    renderItem.renderItemIntoGUI(stack, (int) (renderx + 10), (int) y + 15);
                    renderx += 22;
                }
            }
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableDepth();
            GlStateManager.popMatrix();

        }
    }
}
