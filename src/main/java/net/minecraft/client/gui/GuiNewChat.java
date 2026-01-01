package net.minecraft.client.gui;

import com.google.common.collect.Lists;
import dev.xinxin.SilenceFix;
import dev.xinxin.gui.ui.modules.ShaderElement;
import dev.xinxin.module.modules.world.AutoLFix;
import dev.xinxin.module.modules.world.Spammer;
import dev.xinxin.utils.render.RenderUtil;
import dev.xinxin.utils.render.RoundedUtils;
import dev.xinxin.utils.render.fontRender.FontManager;
import dev.yalan.live.silencefix.LiveComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import java.util.Iterator;
import java.util.List;

public class GuiNewChat
        extends Gui {
    private static final Logger logger = LogManager.getLogger();
    private final Minecraft mc;
    private final List<String> sentMessages = Lists.newArrayList();
    private final List<ChatLine> chatLines = Lists.newArrayList();
    private final List<ChatLine> drawnChatLines = Lists.newArrayList();
    private int scrollPos;
    private boolean isScrolled;

    public GuiNewChat(Minecraft mcIn) {
        this.mc = mcIn;
    }

    public void drawChat(int updateCounter) {
        if (this.mc.gameSettings.chatVisibility != EntityPlayer.EnumChatVisibility.HIDDEN) {
            int i = this.getLineCount();
            boolean flag = false;
            int j2 = 0;
            int k2 = this.drawnChatLines.size();
            float f = this.mc.gameSettings.chatOpacity * 0.9f + 0.1f;
            if (k2 > 0) {
                if (this.getChatOpen()) {
                    flag = true;
                }
                float f1 = this.getChatScale();
                int l2 = MathHelper.ceiling_float_int((float)this.getChatWidth() / f1);
                int fontH = FontManager.navenRegular18.getHeight();
                int visible = computeVisibleCount(updateCounter, i, f);
                if (visible > 0) {
                //    drawChatBackground(f, f1, l2, fontH, visible);
                }
                GlStateManager.pushMatrix();
                GlStateManager.translate(2.0f, 20.0f, 0.0f);
                GlStateManager.scale(f1, f1, 1.0f);
                String self = this.mc.getSession().getUsername();
                for (int i1 = 0; i1 + this.scrollPos < this.drawnChatLines.size() && i1 < i; ++i1) {
                    int j1;
                    ChatLine chatline = this.drawnChatLines.get(i1 + this.scrollPos);
                    if (chatline == null || (j1 = updateCounter - chatline.getUpdatedCounter()) >= 200 && !flag) continue;
                    double d0 = (double)j1 / 200.0;
                    d0 = 1.0 - d0;
                    d0 *= 10.0;
                    d0 = MathHelper.clamp_double(d0, 0.0, 1.0);
                    d0 *= d0;
                    int l1 = (int)(255.0 * d0);
                    if (flag) {
                        l1 = 255;
                    }
                    l1 = (int)((float)l1 * f);
                    ++j2;
                    if (l1 <= 3) continue;
                    int i2 = 0;
                    int j22 = -i1 * fontH;
                    String raw = replaceChatMessage(chatline.getChatComponent().getFormattedText());
                    boolean isSelf = raw.contains("<" + self + ">") || raw.startsWith(self + ":") || raw.startsWith(self + "：");
                    String s2 = isSelf ? ("§f" + net.minecraft.util.EnumChatFormatting.getTextWithoutFormattingCodes(raw)) : raw;
                    GlStateManager.enableBlend();
                    FontManager.navenRegular18.drawStringWithShadow(s2, i2, j22 - fontH, 0xFFFFFF + (l1 << 24));
                    GlStateManager.disableAlpha();
                    GlStateManager.disableBlend();
                }
                GlStateManager.popMatrix();
                GlStateManager.pushMatrix();
                GlStateManager.translate(2.0f, 20.0f, 0.0f);
                GlStateManager.scale(f1, f1, 1.0f);
                if (flag) {
                    int k22 = FontManager.navenRegular18.getHeight();
                    GlStateManager.translate(-3.0f, 0.0f, 0.0f);
                    int l22 = k2 * k22 + k2;
                    int i3 = j2 * k22 + j2;
                    int j3 = this.scrollPos * i3 / k2;
                    int k1 = i3 * i3 / l22;
                    if (l22 != i3) {
                        int k3 = j3 > 0 ? 170 : 96;
                        int l3 = this.isScrolled ? 0xCC3333 : 0x3333AA;
                        GuiNewChat.drawRect(0.0, -j3, 2.0, -j3 - k1, l3 + (k3 << 24));
                    }
                }
                GlStateManager.popMatrix();
            }
        }
    }

    private int computeVisibleCount(int updateCounter, int maxLines, float opacity) {
        int visible = 0;
        int fontH = FontManager.navenRegular18.getHeight();
        for (int i1 = 0; i1 + this.scrollPos < this.drawnChatLines.size() && i1 < maxLines; ++i1) {
            ChatLine chatlineC = this.drawnChatLines.get(i1 + this.scrollPos);
            if (chatlineC == null) continue;
            int age = updateCounter - chatlineC.getUpdatedCounter();
            if (age >= 200 && !this.getChatOpen()) continue;
            double d0c = (double)age / 200.0;
            d0c = 1.0 - d0c;
            d0c *= 10.0;
            d0c = MathHelper.clamp_double(d0c, 0.0, 1.0);
            d0c *= d0c;
            int a = (int)(255.0 * d0c);
            if (this.getChatOpen()) a = 255;
            a = (int)(a * opacity);
            if (a > 3) ++visible;
        }
        return visible;
    }















    private String replaceChatMessage(String msg) {
        for (NetworkPlayerInfo networkPlayerInfo : mc.getNetHandler().getPlayerInfoMap()) {
            final String mcName = networkPlayerInfo.getGameProfile().getName();

            if (mcName.length() >= 5 && networkPlayerInfo.liveUser != null) {
                msg = msg.replace(mcName, String.format("%s§f(§b%s§f-%s§f)",
                        mcName,
                        networkPlayerInfo.liveUser.getName(),
                        networkPlayerInfo.liveUser.getRank()));
            }
        }

        return msg;
    }

    public void clearChatMessages() {
        this.drawnChatLines.clear();
        this.chatLines.clear();
        this.sentMessages.clear();
    }

    public void printChatMessage(IChatComponent chatComponent) {
        if (chatComponent.getUnformattedText().equals("请先打开协管模式")) return;
        if (chatComponent.getUnformattedText().equals("请先输入/volunteeron打开协管模式")) return;
        this.printChatMessageWithOptionalDeletion(chatComponent, 0);
    }

  /*  private final Map<String, Long> lastMessageTime = new HashMap<>();
    private final Map<String, Integer> timeoutCount = new HashMap<>();
    private final Map<String, Integer> recoveryCount = new HashMap<>();
    private final Set<String> blacklist = new HashSet<>();*/

    public void printChatMessageWithOptionalDeletion(IChatComponent chatComponent, int chatLineId) {
        String message = chatComponent.getUnformattedText();

        if (message.equals("请先打开协管模式")) return;
        if (message.equals("请先输入/volunteeron打开协管模式")) return;


        final boolean spammerFakeEnabled = SilenceFix.instance.moduleManager.getModule(Spammer.class).fake.getValue();

        for (String ad : Spammer.getCurrentAds()) {
            if (message.contains(ad)) {
                final String playerName = LiveComponent.extractPlayerId(message);

                if (playerName != null) {
                    final EntityPlayer player = mc.theWorld.getPlayerEntityByName(playerName);

                    if (player != null) {
                        player.isSilenceUser = true;
                    }
                }

                if (spammerFakeEnabled) {
                    return;
                }
            }
        }
    /*    for (String ad : Spammer.getCurrentAds()) {
            if (message.contains(ad)) {
                final String playerName = IRCComponent.extractPlayerId(message);


                if (playerName != null) {
                    final EntityPlayer player = mc.theWorld.getPlayerEntityByName(playerName);
                    long now = System.currentTimeMillis();
                    long last = lastMessageTime.getOrDefault(playerName, 0L);
                    long delta = now - last;

                    boolean wasBlacklisted = blacklist.contains(playerName);
                    boolean isValidNow = delta <= 3500;

                    if (wasBlacklisted) {
                        if (isValidNow) {
                            blacklist.remove(playerName);
                            recoveryCount.put(playerName, 1);
                            HelperUtil.sendMessage("§e" + playerName + " 正在尝试成为何树友");
                        } else {
                            continue;
                        }
                    } else {
                        if (!isValidNow && last != 0) {
                            int count = timeoutCount.getOrDefault(playerName, 0) + 1;
                            timeoutCount.put(playerName, count);

                            if (count == 1) {
                                HelperUtil.sendMessage("§e" + playerName + " 成为何树友失败（1/3）");
                            } else if (count == 2) {
                                HelperUtil.sendMessage("§6" + playerName + " 被删除何树友（2/3）");
                                IRCComponent.silenceUsers.remove(playerName);

                            } else if (count >= 3) {
                                blacklist.add(playerName);
                                timeoutCount.remove(playerName);
                                recoveryCount.remove(playerName);
                                HelperUtil.sendMessage("§c" + playerName + " 被拉黑，不再视为何树友！");
                                IRCComponent.silenceUsers.remove(playerName);
                            }
                        } else {
                            timeoutCount.remove(playerName);

                            IRCComponent.silenceUsers.add(playerName);


                            int recover = recoveryCount.getOrDefault(playerName, 0) + 1;
                            if (recover >= 3) {
                                HelperUtil.sendMessage("§a" + playerName + " 已成为何树友！");
                                recoveryCount.remove(playerName);
                                timeoutCount.remove(playerName);
                            } else {
                                recoveryCount.put(playerName, recover);
                            }
                        }
                    }

                    lastMessageTime.put(playerName, now);
                }

                if (spammerFakeEnabled) {
                    return;
                }
            }
        }
*/

        if (SilenceFix.instance.moduleManager.getModule(AutoLFix.class).fake.getValue()) {
            for (String taunt : AutoLFix.messages) {
                if (message.contains(taunt)) {
                    return;
                }
            }
        }

        this.setChatLine(chatComponent, chatLineId, this.mc.ingameGUI.getUpdateCounter(), false);
    }

    private void setChatLine(IChatComponent chatComponent, int chatLineId, int updateCounter, boolean displayOnly) {
        if (chatLineId != 0) {
            this.deleteChatLine(chatLineId);
        }
        int i = MathHelper.floor_float((float)this.getChatWidth() / this.getChatScale());
        List<IChatComponent> list = GuiUtilRenderComponents.splitText(chatComponent, i, this.mc.fontRendererObj, false, false);
        boolean flag = this.getChatOpen();
        for (IChatComponent ichatcomponent : list) {
            if (flag && this.scrollPos > 0) {
                this.isScrolled = true;
                this.scroll(1);
            }
            this.drawnChatLines.add(0, new ChatLine(updateCounter, ichatcomponent, chatLineId));
        }
        while (this.drawnChatLines.size() > 100) {
            this.drawnChatLines.remove(this.drawnChatLines.size() - 1);
        }
        if (!displayOnly) {
            this.chatLines.add(0, new ChatLine(updateCounter, chatComponent, chatLineId));
            while (this.chatLines.size() > 100) {
                this.chatLines.remove(this.chatLines.size() - 1);
            }
        }
    }

    public void refreshChat() {
        this.drawnChatLines.clear();
        this.resetScroll();
        for (int i = this.chatLines.size() - 1; i >= 0; --i) {
            ChatLine chatline = this.chatLines.get(i);
            this.setChatLine(chatline.getChatComponent(), chatline.getChatLineID(), chatline.getUpdatedCounter(), true);
        }
    }

    public List<String> getSentMessages() {
        return this.sentMessages;
    }

    public void addToSentMessages(String message) {
        if (this.sentMessages.isEmpty() || !this.sentMessages.get(this.sentMessages.size() - 1).equals(message)) {
            this.sentMessages.add(message);
        }
    }

    public void resetScroll() {
        this.scrollPos = 0;
        this.isScrolled = false;
    }

    public void scroll(int amount) {
        this.scrollPos += amount;
        int i = this.drawnChatLines.size();
        if (this.scrollPos > i - this.getLineCount()) {
            this.scrollPos = i - this.getLineCount();
        }
        if (this.scrollPos <= 0) {
            this.scrollPos = 0;
            this.isScrolled = false;
        }
    }

    public IChatComponent getChatComponent(int mouseX, int mouseY) {
        if (!this.getChatOpen()) {
            return null;
        }
        ScaledResolution scaledresolution = new ScaledResolution(this.mc);
        int i = scaledresolution.getScaleFactor();
        float f = this.getChatScale();
        int j2 = mouseX / i - 3;
        int k2 = mouseY / i - 27;
        j2 = MathHelper.floor_float((float)j2 / f);
        k2 = MathHelper.floor_float((float)k2 / f);
        if (j2 >= 0 && k2 >= 0) {
            int l2 = Math.min(this.getLineCount(), this.drawnChatLines.size());
            if (j2 <= MathHelper.floor_float((float)this.getChatWidth() / this.getChatScale()) && k2 < FontManager.navenRegular18.getHeight() * l2 + l2) {
                int i1 = k2 / FontManager.navenRegular18.getHeight() + this.scrollPos;
                if (i1 >= 0 && i1 < this.drawnChatLines.size()) {
                    ChatLine chatline = this.drawnChatLines.get(i1);
                    int j1 = 0;
                    for (IChatComponent ichatcomponent : chatline.getChatComponent()) {
                        if (!(ichatcomponent instanceof ChatComponentText) || (j1 += FontManager.navenRegular18.getStringWidth(GuiUtilRenderComponents.func_178909_a(((ChatComponentText)ichatcomponent).getChatComponentText_TextValue(), false))) <= j2) continue;
                        return ichatcomponent;
                    }
                }
                return null;
            }
            return null;
        }
        return null;
    }


    public boolean getChatOpen() {
        return this.mc.currentScreen instanceof GuiChat;
    }

    public void deleteChatLine(int id) {
        Iterator<ChatLine> iterator = this.drawnChatLines.iterator();
        while (iterator.hasNext()) {
            ChatLine chatline = iterator.next();
            if (chatline.getChatLineID() != id) continue;
            iterator.remove();
        }
        iterator = this.chatLines.iterator();
        while (iterator.hasNext()) {
            ChatLine chatline1 = iterator.next();
            if (chatline1.getChatLineID() != id) continue;
            iterator.remove();
            break;
        }
    }

    public int getChatWidth() {
        return GuiNewChat.calculateChatboxWidth(this.mc.gameSettings.chatWidth);
    }

    public int getChatHeight() {
        return GuiNewChat.calculateChatboxHeight(this.getChatOpen() ? this.mc.gameSettings.chatHeightFocused : this.mc.gameSettings.chatHeightUnfocused);
    }

    public float getChatScale() {
        return this.mc.gameSettings.chatScale;
    }

    public static int calculateChatboxWidth(float scale) {
        int i = 320;
        int j2 = 40;
        return MathHelper.floor_float(scale * (float)(i - j2) + (float)j2);
    }

    public static int calculateChatboxHeight(float scale) {
        int i = 180;
        int j2 = 20;
        return MathHelper.floor_float(scale * (float)(i - j2) + (float)j2);
    }

    public int getLineCount() {
        return this.getChatHeight() / FontManager.navenRegular18.getHeight();
    }

}

