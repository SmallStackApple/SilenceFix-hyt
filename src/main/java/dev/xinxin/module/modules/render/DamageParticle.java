package dev.xinxin.module.modules.render;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ibm.icu.math.BigDecimal;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.rendering.EventRender3D;
import dev.xinxin.event.world.EventUpdate;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.modules.combat.KillAura;
import dev.xinxin.module.values.NumberValue;
import dev.xinxin.utils.render.RoundedUtils;
import dev.xinxin.utils.render.fontRender.FontManager;
import dev.xinxin.utils.vec.Vector3d;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.List;
import java.util.Map;

public class DamageParticle extends Module {

    public DamageParticle() {
        super("Damage Particles", Category.Render, "伤害显示");
    }

    private final NumberValue deleteAfter = new NumberValue("Remove Ticks", 15, 1, 60, 1);
    private final NumberValue particleCount = new NumberValue("Dot Count", 8, 0, 20, 1);

    private final Map<Integer, Float> hpData = Maps.newHashMap();
    private final List<Particle> particles = Lists.newArrayList();

    @EventTarget
    public void onPreUpdate(EventUpdate event) {
        for (Particle p : particles) {
            if (p != null) p.ticks++;
        }

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase ent)) continue;
            double lastHp = hpData.getOrDefault(ent.getEntityId(), ent.getMaxHealth());
            hpData.put(ent.getEntityId(), ent.getHealth());
            if (lastHp == ent.getHealth()) continue;

            double diff = ent.getHealth() - lastHp;

            Color color;
            if (diff > 0) {
                color = Color.GREEN; // 回血绿色
            } else {
                color = Color.YELLOW; // 掉血黄色
            }

            Vector3d textPos = new Vector3d(
                    entity.posX + Math.random() * 0.5 * (Math.random() > 0.5 ? -1 : 1),
                    entity.getEntityBoundingBox().minY + (entity.getEntityBoundingBox().maxY - entity.getEntityBoundingBox().minY) * 0.5,
                    entity.posZ + Math.random() * 0.5 * (Math.random() > 0.5 ? -1 : 1)
            );
            if (entity != KillAura.target){
                continue;
            }

            Vector3d explosionCenter = new Vector3d(
                    entity.posX,
                    entity.getEntityBoundingBox().minY + (entity.getEntityBoundingBox().maxY - entity.getEntityBoundingBox().minY) * 0.5,
                    entity.posZ
            );

            double str = new BigDecimal(Math.abs(lastHp - ent.getHealth())).setScale(1, 4).doubleValue();
            particles.add(new Particle(String.valueOf(str), textPos, explosionCenter, color, particleCount.getValue().intValue()));
        }
    }


    @Override
    public void onDisable() {
        particles.clear();
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        boolean canClear = true;
        for (Particle p : particles) {
            if (p == null || p.ticks > deleteAfter.getValue()) continue;
            canClear = false;

            // 渲染文字
            GlStateManager.pushMatrix();
            GlStateManager.translate(p.textPos.getX() - RenderManager.getRenderPosX(), p.textPos.getY() - RenderManager.getRenderPosY(), p.textPos.getZ() - RenderManager.getRenderPosZ());
            GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0f, 1.0f, 0.0f);
            GlStateManager.rotate(mc.getRenderManager().playerViewX, mc.gameSettings.thirdPersonView == 2 ? -1.0f : 1.0f, 0.0f, 0.0f);
            GlStateManager.scale(-0.03, -0.03, 0.03);
            GL11.glDepthMask(false);
            FontManager.navenRegular30.drawStringWithShadow(
                    p.str,
                    (float) (-FontManager.navenRegular30.getStringWidth(p.str) / 2.0),
                    -FontManager.navenRegular30.getStringHeight() + 1,
                    p.color.getRGB()
            );
            GL11.glDepthMask(true);
            GlStateManager.popMatrix();

            // 渲染子粒子
            float progress = p.ticks / (deleteAfter.getValue().floatValue()*2);
            boolean fading = progress > 0.3f;

            for (Scatter s : p.scatters) {
                Vector3d pos = s.getInterpolatedPos(progress); // 缓动插值位置

                float alpha = 1.0f;
                if (fading) {
                    float fadeProgress = (progress - 0.3f) / 0.3f;
                    alpha = 1.0f - fadeProgress;
                }

                Color fadeColor = new Color(
                        p.color.getRed(),
                        p.color.getGreen(),
                        p.color.getBlue(),
                        (int) (Math.max(0f, Math.min(alpha, 1f)) * 255)
                );

                GlStateManager.pushMatrix();
                GlStateManager.translate(pos.getX() - RenderManager.getRenderPosX(), pos.getY() - RenderManager.getRenderPosY(), pos.getZ() - RenderManager.getRenderPosZ());
                GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0f, 1.0f, 0.0f);
                GlStateManager.rotate(mc.getRenderManager().playerViewX, mc.gameSettings.thirdPersonView == 2 ? -1.0f : 1.0f, 0.0f, 0.0f);
                GlStateManager.scale(-0.03, -0.03, 0.03);
                RoundedUtils.drawRound(-1, -1, 2, 2, 1, fadeColor);
                GlStateManager.popMatrix();
            }
        }

        if (canClear) {
            particles.clear();
        }
    }

    public static class Particle {
        public final String str;
        public final Vector3d textPos;
        public final Color color;
        public int ticks;
        public final List<Scatter> scatters = new java.util.ArrayList<>();

        public Particle(String str, Vector3d textPos, Vector3d explosionCenter, Color color, int count) {
            this.str = str;
            this.textPos = textPos;
            this.color = color;
            this.ticks = 0;

        }
    }

    private static class Scatter {
        public final Vector3d startPos;
        public final Vector3d offset;

        public Scatter(Vector3d startPos, Vector3d offset) {
            this.startPos = startPos;
            this.offset = offset;
        }

        public Vector3d getInterpolatedPos(float progress) {
            float eased = 1 - (float) Math.pow(1 - progress, 3); // EaseOutCubic
            return new Vector3d(
                    startPos.getX() + offset.getX() * eased,
                    startPos.getY() + offset.getY() * eased,
                    startPos.getZ() + offset.getZ() * eased
            );
        }
    }
}
