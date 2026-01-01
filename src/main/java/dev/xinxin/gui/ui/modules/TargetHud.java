package dev.xinxin.gui.ui.modules;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.rendering.EventRender2D;
import dev.xinxin.event.rendering.EventShader;
import dev.xinxin.event.world.EventTick;
import dev.xinxin.gui.ui.UiModule;
import dev.xinxin.module.ModuleManager;
import dev.xinxin.module.modules.combat.KillAura;
import dev.xinxin.module.modules.misc.AutoGapple;
import dev.xinxin.module.modules.render.HUD;
import dev.xinxin.utils.InventoryUtil;
import dev.xinxin.utils.TimerUtil;
import dev.xinxin.utils.render.*;
import dev.xinxin.utils.render.animation.impl.ContinualAnimation;
import dev.xinxin.utils.render.animation.Animation;
import dev.xinxin.utils.render.animation.Direction;
import dev.xinxin.utils.render.animation.impl.DecelerateAnimation;
import dev.xinxin.utils.render.animation.impl.EaseBackIn;
import dev.xinxin.utils.render.fontRender.FontManager;
import dev.xinxin.utils.render.fontRender.RapeMasterFontManager;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.UUID;

public class TargetHud extends UiModule {
    private boolean sentParticles;
    public final List<Particle> particles = new ArrayList<>();
    private final ContinualAnimation animation2 = new ContinualAnimation();
    private final ContinualAnimation animation3 = new ContinualAnimation();
    final DecimalFormat DF_1 = new DecimalFormat("0.0");
    final DecimalFormat DF_0 = new DecimalFormat("0");
    private final Animation openAnimation = new DecelerateAnimation(175, 0.5);
    public static EntityLivingBase target;
    private final Animation animation = new EaseBackIn(500, 1.0, 1.5f);

    public ContinualAnimation animatedX = new ContinualAnimation();
    public ContinualAnimation animatedY = new ContinualAnimation();
    public ContinualAnimation animatedW = new ContinualAnimation();
    public ContinualAnimation animatedH = new ContinualAnimation();

    public static float tabX;
    public static float tabY;
    public static float tabW;
    public static float tabH;
    public static boolean island = false;

    private final Map<Entity, ContinualAnimation> animXMap = new HashMap<>();
    private final Map<Entity, ContinualAnimation> animYMap = new HashMap<>();
    private final Map<Entity, ContinualAnimation> animWMap = new HashMap<>();
    private final Map<Entity, ContinualAnimation> animHMap = new HashMap<>();

    private final Map<UUID, Integer> gappleCache = new HashMap<>();
    private final Map<UUID, Long> gappleSeenAt = new HashMap<>();
    private static final long GAPPLE_CACHE_TTL_MS = 90000L;

    public TargetHud() {
        super("TargetHud", 50.0, 50.0, 200.0, 120.0);
    }

    @EventTarget
    public void onTick(EventTick event) {
        KillAura aura = ModuleManager.getModule(KillAura.class);
        if (aura != null && !aura.getState()) {
            island = false;
        }
        if (KillAura.target != null) {
            this.target = KillAura.target;
            island = true;
        }
        if (aura != null && !aura.getState()) {
            island = false;
        } else if (KillAura.target == null) {
            island = false;
        }

        if (mc.currentScreen instanceof GuiChat) {
            this.target = mc.thePlayer;
        }
        if (island){
            tabX = (float) getPosX();
            tabY = (float) getPosY();
            tabW = (float) getWidth();
            tabH = (float) getHeight();
        }

        animXMap.keySet().removeIf(e -> e.isDead || !KillAura.targets.contains(e));
        animYMap.keySet().removeIf(e -> e.isDead || !KillAura.targets.contains(e));
        animWMap.keySet().removeIf(e -> e.isDead || !KillAura.targets.contains(e));
        animHMap.keySet().removeIf(e -> e.isDead || !KillAura.targets.contains(e));

        gappleCache.keySet().removeIf(uuid -> mc.theWorld == null || mc.theWorld.getPlayerEntityByUUID(uuid) == null);
        gappleSeenAt.keySet().removeIf(uuid -> !gappleCache.containsKey(uuid));
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        float x = (float) this.getPosX();
        float y = (float) this.getPosY();

        if (mc.currentScreen instanceof GuiChat) {
            this.render(x, y, 1.0f, mc.thePlayer, false);
            return;
        }

        if (HUD.multi_targetHUD.getValue()) {
            if (!KillAura.targets.isEmpty()) {
                int count = 0;
                for (int i = 0; i < KillAura.targets.size(); ++i) {
                    final Entity target = KillAura.targets.get(i);
                    if (count <= 4) {
                        this.render(x, y, 1.0f, (EntityLivingBase)target, false);
                        x += width + 10;
                        ++count;
                    }
                }
            }
            return;
        }

        if (island) {
            this.render(x, y, 1.0f, this.target, false);
        }
        GlStateManager.resetColor();
        GlStateManager.disableLighting();
    }

    @EventTarget
    public void shader(EventShader event) {
        float x = (float) this.getPosX();
        float y = (float) this.getPosY();

        if (mc.currentScreen instanceof GuiChat) {
            this.render(x, y, 1.0f, mc.thePlayer, true);
            return;
        }

        if (HUD.multi_targetHUD.getValue()) {
            if (!KillAura.targets.isEmpty()) {
                int count = 0;
                for (int i = 0; i < KillAura.targets.size(); ++i) {
                    final Entity target = KillAura.targets.get(i);
                    if (count <= 4) {
                        this.render(x, y, 1.0f, (EntityLivingBase)target, true);
                        x += width + 10;
                        ++count;
                    }
                }
            }
            return;
        }

        if (island) {
            this.render(x, y, 1.0f, this.target, true);
        }
        GlStateManager.resetColor();
        GlStateManager.disableLighting();
    }

    protected void renderPlayer2D(float x, float y, float width, float height, AbstractClientPlayer player) {
        GLUtil.startBlend();
        mc.getTextureManager().bindTexture(player.getLocationSkin());
        Gui.drawScaledCustomSizeModalRect(x, y, 8.0F, 8.0F, 8, 8, width, height, 64.0F, 64.0F);
        GLUtil.endBlend();
    }

    public void runToXy(float realX, float realY) {
        animatedX.animate(realX, 10);
        animatedY.animate(realY, 10);
    }

    private void runToXyWH(Entity entity, float x, float y, float w, float h) {
        animXMap.putIfAbsent(entity, new ContinualAnimation());
        animYMap.putIfAbsent(entity, new ContinualAnimation());
        animWMap.putIfAbsent(entity, new ContinualAnimation());
        animHMap.putIfAbsent(entity, new ContinualAnimation());

        animXMap.get(entity).animate(x, 5);
        animYMap.get(entity).animate(y, 5);
    }

    public void runToWH(float realX, float realY) {
        animatedW.animate(realX, 30);
        animatedH.animate(realY, 30);
    }

    private int getObservedGappleCount(EntityLivingBase e) {
        if (e == null) return 0;
        if (e instanceof EntityPlayer) {
            UUID id = e.getUniqueID();
            ItemStack held = e.getHeldItem();
            if (held != null && held.getItem() == Items.golden_apple) {
                int count = held.stackSize;
                gappleCache.put(id, count);
                gappleSeenAt.put(id, System.currentTimeMillis());
                return count;
            }
            Integer cached = gappleCache.get(id);
            Long ts = gappleSeenAt.get(id);
            if (ts != null && System.currentTimeMillis() - ts > GAPPLE_CACHE_TTL_MS) {
                gappleCache.remove(id);
                gappleSeenAt.remove(id);
                return 0;
            }
            return cached != null ? cached : 0;
        } else {
            ItemStack held = e.getHeldItem();
            return (held != null && held.getItem() == Items.golden_apple) ? held.stackSize : 0;
        }
    }

    public void render(float x2, float y2, float alpha, EntityLivingBase target, boolean blur) {
        if (target == null) return;

        int gapplecount = getObservedGappleCount(target);

        Color firstColor = HUD.color(1);
        Color secondColor = HUD.color(6);

        if (HUD.targetHud.getValue()) {
            String playername = target.getName();

            String HealthAndGapple = "H:" + DF_0.format(target.getHealth()) + "  G:" + gapplecount;

            float hurtTime = (target.hurtTime == 0 ? 0 : (target.hurtTime - mc.timer.renderPartialTicks) * 0.5f);

            RapeMasterFontManager regular = FontManager.navenRegular18;
            RapeMasterFontManager bold = FontManager.navenbold18;

            float h = 40, x = x2, y = y2, head = 24, gap = 5, gap2 = 3;
            float nameW = regular.getStringWidth(playername);
            float nameH = regular.getHeight();

            float w = Math.max(120, gap * 4 + head + nameW + bold.getStringWidth(HealthAndGapple)
                    + ((gap * 4 + head + nameW + bold.getStringWidth(HealthAndGapple) > 120) ? gap : 0));
            float seperation = 16;
            float healthBarWidth = w - 10;

            if (!(mc.currentScreen instanceof GuiChat)) {
                if (HUD.multi_targetHUD.getValue()) {
                    runToXyWH(target, x, y, w, h);
                    x = animXMap.get(target).getOutput();
                    y = animYMap.get(target).getOutput();
                } else {
                    runToXy(x, y);
                    runToWH(w, h);
                    w = animatedW.getOutput();
                    h = animatedH.getOutput();
                    x = animatedX.getOutput();
                    y = animatedY.getOutput();
                }
            } else {
                runToXy(x, y);
            }

            if (w < 5 || h < 5) return;
            width = w;
            height = 40;
            final float healthPercent = (target.getHealth() + target.getAbsorptionAmount())
                    / (target.getMaxHealth() + target.getAbsorptionAmount());
            final float var = healthBarWidth * healthPercent;
            target.animatedHealthBar = AnimationUtil.smooth(target.animatedHealthBar, var, 0.4f);

            Color background = blur ? new Color(0, 0, 0, 220) : new Color(0, 0, 0, 80);
            RoundedUtils.drawRound(x, y, w, h, 5, background);
            if (blur) return;
            GlStateManager.pushMatrix();
            if (target instanceof AbstractClientPlayer) {
                StencilUtil.write(false);
                RenderUtil.renderRoundedRect(x + gap , y + gap, head - hurtTime, head - hurtTime, 2, -1);
                StencilUtil.erase(true);
                RenderUtil.color(ColorUtil.mixColors(Color.RED, Color.WHITE, hurtTime / 9).getRGB());
                renderPlayer2D(x + gap, y + gap, head - hurtTime, head - hurtTime, (AbstractClientPlayer) target);
                StencilUtil.dispose();
            } else {
                RoundedUtils.drawRound(x + gap, y + gap, head - hurtTime,  head - hurtTime, 2, new Color(0, 0, 0, 220));
                FontManager.arial32.drawStringWithShadow("?", x + gap + head / 3 - hurtTime/2 , y + gap + head / 7 - hurtTime/2, -1);
            }

            regular.drawString(playername, x + gap + head + gap, y + gap, firstColor.getRGB());
            FontManager.navenRegular16.drawString(HealthAndGapple, x + gap + head + gap + nameW + gap , y + gap + 1, firstColor.getRGB());

            RoundedUtils.drawGradientRoundLR(x + 5, y + gap2 + nameH + seperation, healthBarWidth, 3, 1,
                    new Color(40, 40, 40, 150), new Color(40, 40, 40, 150));
            RoundedUtils.drawGradientRoundLR(x + 5, y + gap2 + nameH + seperation, target.animatedHealthBar, 3, 1,
                    firstColor, secondColor);

            GlStateManager.pushMatrix();
            try {
                RenderHelper.enableGUIStandardItemLighting();
                for (int i = 0; i <= 3; i++) {
                    if (target.getCurrentArmor(i) == null) continue;
                    RenderUtil.resetColor();
                    GLUtil.startBlend();
                    RenderUtil.color(-1);
                    mc.getRenderItem().renderItemAndEffectIntoGUI(
                            target.getCurrentArmor(i),
                            (int) (x + gap + head + gap -2.5 + (seperation * (3 - i))),
                            (int) (y + gap2 + nameH - 2)
                    );
                    GLUtil.endBlend();
                }
                if (target.getHeldItem() != null) {
                    GLUtil.startBlend();
                    RenderUtil.resetColor();
                    RenderUtil.color(-1);
                    mc.getRenderItem().renderItemAndEffectIntoGUI(
                            target.getHeldItem(),
                            (int) (x + gap + head + gap -2.5 + (seperation * 4)),
                            (int) (y + gap2 + nameH - 2)
                    );
                    GLUtil.endBlend();
                }
            } finally {
                RenderHelper.disableStandardItemLighting();
                GlStateManager.popMatrix();
            }
            GlStateManager.popMatrix();
        }
    }

    public static class Particle {
        public float x, y, adjustedX, adjustedY, deltaX, deltaY, size, opacity;
        public Color color;

        public void render2D() {
            RoundedUtils.round(this.x + this.adjustedX, this.y + this.adjustedY, this.size, this.size, 12.0f, this.color);
        }
        public void updatePosition() {
            for (int i = 1; i <= 2; ++i) {
                this.adjustedX += this.deltaX;
                this.adjustedY += this.deltaY;
                this.deltaY *= 0.97f;
                this.deltaX *= 0.97f;
                this.opacity -= 1.0f;
                if (this.opacity < 1.0f) this.opacity = 1.0f;
            }
        }
        public void init(float x2, float y2, float deltaX, float deltaY, float size, Color color) {
            this.x = x2;
            this.y = y2;
            this.deltaX = deltaX;
            this.deltaY = deltaY;
            this.size = size;
            this.opacity = 254.0f;
            this.color = color;
        }
    }
}
