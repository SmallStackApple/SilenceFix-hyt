package dev.xinxin.module.modules.render;

import dev.xinxin.SilenceFix;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.misc.EventKey;
import dev.xinxin.event.rendering.EventRender2D;
import dev.xinxin.event.rendering.EventShader;
import dev.xinxin.gui.notification.Notification;
import dev.xinxin.gui.notification.NotificationManager;
import dev.xinxin.gui.ui.UiModule;
import dev.xinxin.gui.ui.modules.*;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.ModuleManager;
import dev.xinxin.module.values.BoolValue;
import dev.xinxin.module.values.ColorValue;
import dev.xinxin.module.values.ModeValue;
import dev.xinxin.module.values.NumberValue;
import dev.xinxin.utils.render.AnimationUtil;
import dev.xinxin.utils.render.RenderUtil;
import dev.xinxin.utils.render.RoundedUtils;
import dev.xinxin.utils.render.animation.Animation;
import dev.xinxin.utils.render.animation.Direction;
import dev.xinxin.utils.render.fontRender.FontManager;
import dev.xinxin.utils.render.fontRender.RapeMasterFontManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;
import net.netease.gui.GermGameGui;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;
public class HUD
extends Module {
    public static final ModeValue<LanguageMode> langModeValue = new ModeValue<>("Language",LanguageMode.values(),LanguageMode.Chinese);
    public static final ModeValue<CapeMode> capeMode = new ModeValue<>("Cape Styles",CapeMode.values(),CapeMode.MODE1);
    public static BoolValue arraylist = new BoolValue("Arraylist", true);
    public static final BoolValue importantModules = new BoolValue("Arraylist-Important", false, () -> arraylist.getValue());
    public static final ModeValue<ModuleList.ANIM> animation = new ModeValue("Arraylist-Animation", ModuleList.ANIM.values(), ModuleList.ANIM.ScaleIn, () -> arraylist.getValue());
    public static BoolValue targetHud = new BoolValue("TargetHUD", true);
    public static BoolValue multi_targetHUD = new BoolValue("Multi TargetHUD", true,()->targetHud.value);
    public static ColorValue mainColor = new ColorValue("First Color", Color.white.getRGB());
    public static ColorValue mainColor2 = new ColorValue("Second Color", Color.white.getRGB());
    public static BoolValue islandLogo = new BoolValue("Island Logo", true);
    public static BoolValue potionInfo = new BoolValue("Potion", false);
    public static BoolValue notifications = new BoolValue("Notification", false);
    public static BoolValue Debug = new BoolValue("Debug", false);
    public static BoolValue session = new BoolValue("Session", true);
    public static BoolValue inventory = new BoolValue("Inventory", true);
    public static BoolValue armorhud = new BoolValue("ArmorHUD", true);
    public static BoolValue blockrate = new BoolValue("blockrateHUD", true);
    public static BoolValue Scoreboard = new BoolValue("ScoreboardHUD", true);
    public static BoolValue TargetBlockRotehud = new BoolValue("TargetBlockRoteHUD", true);


    private float leftY;
    public int offsetValue = 0;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");


    public HUD() {
        super("HUD", Category.Render,"视觉菜单");
        this.setState(true);
    }

    public static Color color(int tick) {
        return new Color(RenderUtil.colorSwitch(mainColor.getColorC(), mainColor2.getColorC(), 2000.0f, -(tick * 200) / 40, 75L, 2.0));
    }

    public static RapeMasterFontManager getFont() {
        return FontManager.harmonybold18;
    }

    @EventTarget
    public void onRender(EventRender2D e) {
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.currentScreen instanceof GermGameGui) {
            PostProcessing.INSTANCE.blurScreen();
        }

        UiModule Arraylist = SilenceFix.instance.uiManager.getModule(ModuleList.class);
        Arraylist.setState(arraylist.getValue());
        this.drawNotifications();

        if (langModeValue.is("English")){
            SilenceFix.NAME = "SilenceFixPro";
        } else SilenceFix.NAME = "欣欣内部客户端";

        UiModule Thud = SilenceFix.instance.uiManager.getModule(TargetHud.class);
        Thud.setState(targetHud.getValue());
        UiModule potionHUD = SilenceFix.instance.uiManager.getModule(PotionsInfo.class);
        potionHUD.setState(potionInfo.getValue());
        UiModule DebugHud = SilenceFix.instance.uiManager.getModule(Debug.class);
        DebugHud.setState(Debug.getValue());
        UiModule sessionHUD = SilenceFix.instance.uiManager.getModule(Session.class);
        sessionHUD.setState(session.getValue());
        UiModule InventoryHUD = SilenceFix.instance.uiManager.getModule(InventoryHUD.class);
        InventoryHUD.setState(inventory.getValue());
        UiModule BlockRate = SilenceFix.instance.uiManager.getModule(BlockRote.class);
        BlockRate.setState(blockrate.getValue());
        UiModule ArmorHud = SilenceFix.instance.uiManager.getModule(ArmorHud.class);
        ArmorHud.setState(armorhud.getValue());
        UiModule ScoreboardHud = SilenceFix.instance.uiManager.getModule(Scoreboard.class);
        ScoreboardHud.setState(Scoreboard.getValue());//TargetBlockRote
        UiModule TargetBlockRoteHud = SilenceFix.instance.uiManager.getModule(TargetBlockRote.class);
        TargetBlockRoteHud.setState(TargetBlockRotehud.getValue());
    }

    public void drawNotifications() {
        ScaledResolution sr = new ScaledResolution(mc);
        float yOffset = 0.0f;
        NotificationManager.setToggleTime(2.0f);
        for (Notification notification : NotificationManager.getNotifications()) {
            Animation animation = notification.getAnimation();
            animation.setDirection(notification.getTimerUtil().hasTimeElapsed((long)notification.getTime()) ? Direction.BACKWARDS : Direction.FORWARDS);
            if (animation.finished(Direction.BACKWARDS)) {
                NotificationManager.getNotifications().remove(notification);
                continue;
            }
            animation.setDuration(200);
            int actualOffset = 3;
            int notificationHeight = 23;
            int notificationWidth = FontManager.arial20.getStringWidth(notification.getDescription()) + 25;
            float x2 = (float)((double)sr.getScaledWidth() - (double)(notificationWidth + 5) * animation.getOutput());
            float y2 = (float)sr.getScaledHeight() - (yOffset + 18.0f + (float)this.offsetValue + (float)notificationHeight + 15.0f);
            notification.drawLettuce(x2, y2, notificationWidth, notificationHeight);
            yOffset = (float)((double)yOffset + (double)(notificationHeight + actualOffset) * animation.getOutput() + 2);
        }
    }

    public enum LanguageMode {
        Chinese,
        English
    }

    public enum CapeMode {
        MODE1,
        MODE2,
        MODE3,
        danzai,
        danzai1,
        DEFAULT
    }

    public enum Section {
        TYPES,
        MODULES

    }
}

