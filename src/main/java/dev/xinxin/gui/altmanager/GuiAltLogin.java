package dev.xinxin.gui.altmanager;

import dev.xinxin.utils.render.fontRender.FontManager;
import dev.yalan.live.silencefix.LiveClient;
import dev.yalan.live.silencefix.netty.LiveProto;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.EnumChatFormatting;

import java.io.IOException;

public abstract class GuiAltLogin
extends GuiScreen {
    private final GuiScreen previousScreen;
    public GuiTextField username;
    public GuiButton getCookieButton;
    public volatile String status = (Object)((Object)EnumChatFormatting.YELLOW) + "Pending...";

    public GuiAltLogin(GuiScreen previousScreen) {
        this.previousScreen = previousScreen;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0: {
                if (!this.username.getText().isEmpty()) {
                    this.onLogin(this.username.getText());
                    break;
                }
                break;
            }
            case 1: {
                status = EnumChatFormatting.YELLOW + "获取Cookie中...";
                getCookieButton.enabled = false;
                LiveClient.INSTANCE.sendPacket(LiveProto.createGetNeteaseCookie());
                break;
            }
            case 2: {
                this.mc.displayGuiScreen(this.previousScreen);
                break;
            }
        }
    }

    public abstract void onLogin(String username);

    @Override
    public void drawScreen(int x2, int y2, float z) {
        this.drawDefaultBackground();
//        this.drawBackground(0);
        this.username.drawTextBox();
        FontManager.arial20.drawCenteredStringWithShadow("添加Cookie/角色", this.width / 2, 20.0f, -1);
        FontManager.arial20.drawCenteredStringWithShadow(this.status, this.width / 2, this.height / 4 + 24 + 38, -1);
        if (this.username.getText().isEmpty() && !this.username.isFocused()) {
            FontManager.arial20.drawStringWithShadow("添加Cookie/角色", this.width / 2 - 96, this.height / 4 + 24 + 72 - 4, -7829368);
        }
        super.drawScreen(x2, y2, z);
    }

    @Override
    public void initGui() {
        int var3 = this.height / 4 + 24;
        this.buttonList.add(new GuiButton(0, this.width / 2 - 100, var3 + 72 + 12, "添加"));
        this.buttonList.add(getCookieButton = new GuiButton(1, this.width / 2 - 100, var3 + 72 + 12 + 24, "获取"));
        this.buttonList.add(new GuiButton(2, this.width / 2 - 100, var3 + 72 + 12 + 24 * 2, "返回"));
        this.username = new GuiTextField(1, this.mc.fontRendererObj, this.width / 2 - 100, var3 + 72 - 12, 200, 20);
        this.username.setFocused(true);
        this.username.setMaxStringLength(Integer.MAX_VALUE);
    }

    @Override
    protected void keyTyped(char character, int key) throws IOException {
        super.keyTyped(character, key);
        if (character == '\t' && this.username.isFocused()) {
            this.username.setFocused(!this.username.isFocused());
        }
        if (character == '\r') {
            this.actionPerformed((GuiButton)this.buttonList.get(0));
        }
        this.username.textboxKeyTyped(character, key);
    }

    @Override
    protected void mouseClicked(int x2, int y2, int button) throws IOException {
        super.mouseClicked(x2, y2, button);
        this.username.mouseClicked(x2, y2, button);
    }

    @Override
    public void updateScreen() {
        this.username.updateCursorCounter();
    }
}

