package dev.xinxin.module;

import dev.xinxin.SilenceFix;
import dev.xinxin.SilenceFixSoundManager;
import dev.xinxin.event.EventManager;
import dev.xinxin.gui.notification.NotificationManager;
import dev.xinxin.gui.notification.NotificationType;
import dev.xinxin.module.modules.render.HUD;
import dev.xinxin.module.values.Value;
import dev.xinxin.utils.render.ProgressManager.ProgressBarManager;
import dev.xinxin.utils.render.animation.Animation;
import dev.xinxin.utils.render.animation.Direction;
import dev.xinxin.utils.render.animation.impl.DecelerateAnimation;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.potion.Potion;

import java.util.ArrayList;
import java.util.List;

public class Module {
    protected static final Minecraft mc = Minecraft.getMinecraft();
    public String name;
    public String suffix;
    public Category category;
    public boolean state = false;
    public boolean defaultOn = false;
    public int key = -1;
    private final Animation animation = new DecelerateAnimation(250, 1.0).setDirection(Direction.BACKWARDS);
    public double progress;
    public double animX = 0.0;
    public double animY = 0.0;
    private final List<Value<?>> T = new ArrayList();
    public String CnName;
    private boolean bindable = true; // 默认可绑定
    @Getter
    @Setter
    public String desc, cndesc;

    public boolean isBindable() {
        return bindable;
    }


    public void setBindable(boolean bindable) {
        this.bindable = bindable;
    }

    @Getter
    public float cGUIAnimation = 0f;

    public Module(String name, Category category,String CnName,String description,String 中文描述) {
        this.name = name;
        this.category = category;
        this.CnName = CnName;
        this.suffix = "";
        this.cndesc = 中文描述;
        this.desc = description;
    }

    public Module(String name, Category category,String CnName) {
        this.name = name;
        this.category = category;
        this.CnName = CnName;
        this.suffix = "";
        this.cndesc = "";
        this.desc = "";
    }

    public void toggle() {
        this.setState(!this.state);
    }

    public void onRegister() {
    }

    public void onEnable() {
    }

    public void onDisable() {
        if (mc.thePlayer != null) {  // Add null check
            mc.thePlayer.removePotionEffectClient(Potion.nightVision.id);
        }
    }

    public boolean isNull() {
        return Module.mc.thePlayer == null || Module.mc.theWorld == null;
    }



    public static <T extends Module> T getModule(Class<T> clazz) {
        return SilenceFix.instance.moduleManager.getModule(clazz);
    }

    public boolean getState() {
        return this.state;
    }

    public void setStateSilent(boolean state) {
        if (this.state == state) {
            return;
        }
        this.state = state;
        if (Module.mc.theWorld != null) {
            Module.mc.theWorld.playSound(Module.mc.thePlayer.posX, Module.mc.thePlayer.posY, Module.mc.thePlayer.posZ, "random.click", 0.5f, state ? 0.6f : 0.5f, false);
        }
        if (state) {
            EventManager.register(this);
            SilenceFix.instance.island.addIsland(this);
            NotificationManager.post(NotificationType.SUCCESS, (HUD.langModeValue.is("English") ?"Module" : "模块"), this.name + (HUD.langModeValue.is("English") ?" Open" : " 开启了"));
        } else {
            EventManager.unregister(this);
            SilenceFix.instance.island.addIsland(this);
            NotificationManager.post(NotificationType.DISABLE, (HUD.langModeValue.is("English") ?"Module" : "模块"), this.name + (HUD.langModeValue.is("English") ?" Close" : " 关闭了"));
        }
    }

    public void setState(boolean state) {
        if (this.state == state) {
            return;
        }
        this.state = state;
        if (Module.mc.theWorld != null) {
            if (state) {
                SilenceFix.instance.soundManager.playSound(SilenceFixSoundManager.SoundType.ENABLE, 1F);
            } else {
                SilenceFix.instance.soundManager.playSound(SilenceFixSoundManager.SoundType.DISABLE, 1F);
            }
        }
        if (state) {
            EventManager.register(this);

            SilenceFix.instance.island.addIsland(this);

            this.onEnable();
            NotificationManager.post(NotificationType.SUCCESS, (HUD.langModeValue.is("English") ? "Module" : "模块"), (HUD.langModeValue.is("English") ? this.name + " Enabled. " : "开启 " + this.CnName + ". "));
        } else {
            EventManager.unregister(this);

            SilenceFix.instance.island.addIsland(this);

            this.onDisable();
            ProgressBarManager.remove(this.name);
            NotificationManager.post(NotificationType.DISABLE, (HUD.langModeValue.is("English") ? "Module" : "模块"), (HUD.langModeValue.is("English") ? this.name + " Disabled. " : "关闭 " + this.CnName + ". "));
        }
    }

    public List<Value<?>> getValues() {
        return this.T;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public void setSuffix(Object obj) {
        String suffix = obj.toString();
        this.suffix = suffix.isEmpty() ? suffix : String.format("§f%s§7", net.minecraft.util.EnumChatFormatting.GRAY + suffix);
    }

    public boolean isDefaultOn() {
        return this.defaultOn;
    }

    public int getKey() {
        return this.key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public double getProgress() {
        return this.progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public double getAnimX() {
        return this.animX;
    }

    public void setAnimX(double animX) {
        this.animX = animX;
    }

    public double getAnimY() {
        return this.animY;
    }

    public void setAnimY(double animY) {
        this.animY = animY;
    }

    public String getName() {
        return this.name;
    }

    public String getCnName(){
        return this.CnName;
    }


    public Category getCategory() {
        return this.category;
    }

    public Animation getAnimation() {
        return this.animation;
    }
}

