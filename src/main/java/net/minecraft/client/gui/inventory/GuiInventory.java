package net.minecraft.client.gui.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.achievement.GuiAchievements;
import net.minecraft.client.gui.achievement.GuiStats;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

public class GuiInventory extends InventoryEffectRenderer {
    private float oldMouseX;
    private float oldMouseY;
    private float openProgress = 0.0f;
    private boolean isClosing = false;
    public static boolean preventImmediateClose = false;

    private static final float OPEN_SPEED = 0.015f;  // 更慢的打开速度
    private static final float CLOSE_SPEED = 0.015f; // 更慢的关闭速度

    public GuiInventory(EntityPlayer p_i1094_1_) {
        super(p_i1094_1_.inventoryContainer);
        this.allowUserInput = true;
        int diff = ThreadLocalRandom.current().nextInt(1, 6);
        Mouse.setCursorPosition((int)(Display.getWidth() / 2 + diff), (int)(Display.getHeight() / 2 + diff));
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        if (!isClosing && GuiInventory.preventImmediateClose && this.mc.currentScreen == null) {
            this.mc.displayGuiScreen(this);
            isClosing = true;
            GuiInventory.preventImmediateClose = false;
        }
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.openProgress = 0.0f;
        if (this.mc.playerController.isInCreativeMode()) {
            GuiInventory.preventImmediateClose = true;
            this.mc.displayGuiScreen(new GuiContainerCreative(this.mc.thePlayer));
        } else {
            super.initGui();
        }
    }

    @Override
    public void handleInput() throws IOException {
        super.handleInput();

        if (!isClosing && mc.gameSettings.keyBindInventory.isPressed()) {
            isClosing = true;
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindInventory.getKeyCode(), false);
            KeyBinding.unPressAllKeys();
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRendererObj.drawString(I18n.format("container.crafting", new Object[0]), 86, 16, 0x404040);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (!isClosing) {
            openProgress += OPEN_SPEED;
            if (openProgress > 1f) openProgress = 1f;
        } else {
            openProgress -= CLOSE_SPEED;
            if (openProgress <= 0f) {
                if (this.mc.currentScreen == this) {
                    this.mc.displayGuiScreen(null);
                }
                return;
            }
        }

        float eased = openProgress * openProgress * (3f - 2f * openProgress);

        int slideOffset = (int)((1 - eased) * -100);
        int originalGuiTop = this.guiTop;
        this.guiTop += slideOffset;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, eased);

        super.drawScreen(mouseX, mouseY, partialTicks);

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();

        this.guiTop = originalGuiTop;
        this.oldMouseX = mouseX;
        this.oldMouseY = mouseY;
    }

    @Override
    public void onGuiClosed() {
        if (!isClosing) {
            isClosing = true;
        } else {
            super.onGuiClosed();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1 && !isClosing) {
            isClosing = true;
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        this.mc.getTextureManager().bindTexture(inventoryBackground);
        int i = this.guiLeft;
        int j2 = this.guiTop;
        this.drawTexturedModalRect(i, j2, 0, 0, this.xSize, this.ySize);
        GuiInventory.drawEntityOnScreen(i + 51, j2 + 75, 30, (float)(i + 51) - this.oldMouseX, (float)(j2 + 75 - 50) - this.oldMouseY, this.mc.thePlayer);
    }

    public static void drawEntityOnScreen(int posX, int posY, int scale, float mouseX, float mouseY, EntityLivingBase ent) {
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate(posX, posY, 50.0f);
        GlStateManager.scale(-scale, scale, scale);
        GlStateManager.rotate(180.0f, 0.0f, 0.0f, 1.0f);
        float f = ent.renderYawOffset;
        float f1 = ent.rotationYaw;
        float f2 = ent.rotationPitch;
        float f3 = ent.prevRotationYawHead;
        float f4 = ent.rotationYawHead;
        GlStateManager.rotate(135.0f, 0.0f, 1.0f, 0.0f);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.rotate(-135.0f, 0.0f, 1.0f, 0.0f);
        GlStateManager.rotate(-((float)Math.atan(mouseY / 40.0f)) * 20.0f, 1.0f, 0.0f, 0.0f);
        ent.renderYawOffset = (float)Math.atan(mouseX / 40.0f) * 20.0f;
        ent.rotationYaw = (float)Math.atan(mouseX / 40.0f) * 40.0f;
        ent.rotationPitch = -((float)Math.atan(mouseY / 40.0f)) * 20.0f;
        ent.rotationYawHead = ent.rotationYaw;
        ent.prevRotationYawHead = ent.rotationYaw;
        GlStateManager.translate(0.0f, 0.0f, 0.0f);
        RenderManager rendermanager = Minecraft.getMinecraft().getRenderManager();
        rendermanager.setPlayerViewY(180.0f);
        rendermanager.setRenderShadow(false);
        rendermanager.renderEntityWithPosYaw(ent, 0.0, 0.0, 0.0, 0.0f, 1.0f);
        rendermanager.setRenderShadow(true);
        ent.renderYawOffset = f;
        ent.rotationYaw = f1;
        ent.rotationPitchHead = ent.rotationPitch = f2;
        ent.prevRotationYawHead = f3;
        ent.rotationYawHead = f4;
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            this.mc.displayGuiScreen(new GuiAchievements(this, this.mc.thePlayer.getStatFileWriter()));
        }
        if (button.id == 1) {
            this.mc.displayGuiScreen(new GuiStats(this, this.mc.thePlayer.getStatFileWriter()));
        }
    }
}