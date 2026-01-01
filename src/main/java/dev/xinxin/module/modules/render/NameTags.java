package dev.xinxin.module.modules.render;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.rendering.EventRender2D;
import dev.xinxin.event.rendering.EventRender3D;
import dev.xinxin.event.rendering.EventRenderNameTag;
import dev.xinxin.event.rendering.EventShader;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.ModuleManager;
import dev.xinxin.module.modules.combat.KillAura;
import dev.xinxin.module.modules.misc.Teams;
import dev.xinxin.module.values.BoolValue;
import dev.xinxin.utils.ESPUtil;
import dev.xinxin.utils.HYTUtils;
import dev.xinxin.utils.render.fontRender.FontManager;
import dev.xinxin.utils.render.fontRender.RapeMasterFontManager;
import dev.yalan.live.silencefix.HackerPlayer;
import dev.yalan.live.silencefix.LiveClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.util.vector.Vector4f;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class NameTags extends Module {
    private final Map<Entity, Vector4f> entityPosition = new HashMap<>();
    private final Map<EntityItem, Vector4f> itemPosition = new HashMap<>();
    private final DecimalFormat distanceFormat = new DecimalFormat("0.0");

    public BoolValue playersValue = new BoolValue("Players", true);
    public BoolValue animalsValue = new BoolValue("Animals", true);
    public BoolValue mobsValue = new BoolValue("Mobs", false);
    public BoolValue invisibleValue = new BoolValue("Invisible", false);

    public NameTags() {
        super("NameTags", Category.Render, "名字透视");
    }

    @EventTarget
    public void onRender3DEvent(EventRender3D event) {
        entityPosition.clear();
        itemPosition.clear();

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (shouldRender(entity) && ESPUtil.isInView(entity)) {
                entityPosition.put(entity, ESPUtil.getEntityPositionsOn2D(entity));
            }

            if (shouldRenderItem(entity) && ESPUtil.isInView(entity)) {
                itemPosition.put((EntityItem) entity, ESPUtil.getEntityPositionsOn2D(entity));
            }
        }
    }

    @EventTarget
    public void onRenderNameTag(EventRenderNameTag event) {
        if (event.getTarget() instanceof EntityPlayer) {
            event.setCancelled(true);
        }
    }

    private void drawOpponentNameTag(EntityLivingBase entity, float x, float y, float width) {
        String name = entity.getDisplayName().getFormattedText();
        if (entity instanceof EntityPlayer) {
            EntityPlayer p = (EntityPlayer) entity;
            if (p.liveUser != null) {
                String live = dev.yalan.live.silencefix.LiveComponent.getLiveUserDisplayName(p.liveUser);
                name = live + EnumChatFormatting.RESET + name;
            }
        }
        name = getRank(entity) + EnumChatFormatting.WHITE + name;
        String sep = " | ";
        String hpStr = Math.round(Math.max(0f, entity.getHealth())) + "HP";
        dev.xinxin.utils.render.fontRender.RapeMasterFontManager font = FontManager.harmonybold16;
        float nameW = font.getStringWidth(name);
        float sepW  = font.getStringWidth(sep);
        float hpW   = font.getStringWidth(hpStr);
        float textW = nameW + sepW + hpW;
        float padX = 4f, padY = 2f;
        float tagW = textW + padX * 2f;
        float tagH = font.getHeight() + padY * 2f;
        float mid = x + (width - x) / 2.0f;
        float boxL = mid - tagW / 2.0f;
        float boxT = y - (tagH + 6.0f);
        float boxR = boxL + tagW;
        float boxB = boxT + tagH;
        int bg    = new java.awt.Color(20, 20, 20, 130).getRGB();
        int cName = 0xFFFFFFFF;
        int cSep  = new java.awt.Color(190, 190, 190).getRGB();
        int cHP   = new java.awt.Color(230, 70, 70).getRGB();
        net.minecraft.client.gui.Gui.drawRect((int)boxL,(int)boxT,(int)boxR,(int)boxB,bg);
        float tx = boxL + padX;
        float ty = boxT + (tagH - font.getHeight()) / 2f + 2.0f;
        font.drawString(name, tx, ty, cName); tx += nameW;
        font.drawString(sep,  tx, ty, cSep);  tx += sepW;
        font.drawString(hpStr,tx, ty, cHP);
    }



    private void drawItemTag(EntityItem item, float x, float y, float width) {
        ItemStack stack = item.getEntityItem();
        if (stack == null) return;

        String itemName = getItemDisplayName(stack);
        if (itemName == null || itemName.isEmpty()) return;

        String count = stack.stackSize > 1 ? " x" + stack.stackSize : "";
        String sep = " | ";
        String distStr = String.format("%.1fm", mc.thePlayer.getDistanceToEntity(item));

        RapeMasterFontManager font = FontManager.harmonybold16;

        boolean special = isTargetItem(stack);

        String text = special ? (itemName + count + sep + distStr) : (itemName + count);

        float nameW = font.getStringWidth(itemName + count);
        float sepW  = special ? font.getStringWidth(sep) : 0f;
        float dW    = special ? font.getStringWidth(distStr) : 0f;
        float textW = nameW + sepW + dW;

        float padX = 4f, padY = 2f;
        float tagW = textW + padX * 2f;
        float tagH = font.getHeight() + padY * 2f;

        float mid = x + (width - x) / 2.0f;
        float boxL = mid - tagW / 2.0f;
        float boxT = y - (tagH + 5.0f);
        float boxR = boxL + tagW;
        float boxB = boxT + tagH;

        int bg   = new Color(20,20,20,130).getRGB();
        int cSep = new Color(190,190,190).getRGB();
        int cRed = new Color(230,70,70).getRGB();
        int cName = special ? getItemColor(stack) : 0xFFFFFFFF;

        if (special) net.minecraft.client.gui.Gui.drawRect((int)boxL,(int)boxT,(int)boxR,(int)boxB,bg);

        float ty = boxT + (tagH - font.getHeight()) / 2f + 2.0f;
        float tx = special ? (boxL + padX) : (mid - textW / 2.0f);

        font.drawString(itemName + count, tx, ty, cName);
        if (special) {
            tx += nameW;
            font.drawString(sep, tx, ty, cSep);
            tx += sepW;
            font.drawString(distStr, tx, ty, cRed);
        }
    }







//    private void drawOwnNameTag(EntityLivingBase entity, float x, float y, float width, boolean blur) {
//        FontRenderer font = mc.fontRendererObj;
//        String rank = getRank(entity);
//
//        if (entity instanceof EntityPlayer) {
//            EntityPlayer player = (EntityPlayer) entity;
//            if (player.ircUser != null) rank += getIRCInfo(player);
//        }
//
//        String name = entity.getDisplayName().getFormattedText();
//        String health = distanceFormat.format(entity.getHealth());
//        String text = rank + EnumChatFormatting.WHITE + name + " " + health;
//
//        float textWidth = font.getStringWidth(text);
//        float tagWidth = textWidth + 20.0f;
//        float fontHeight = font.getHeight();
//
//        float middle = x + (width - x) / 2.0f;
//        float boxLeft = middle - tagWidth / 2.0f;
//
//        if (entity.getHeldItem() != null) {
//            mc.getRenderItem().renderItemIntoGUI(entity.getHeldItem(), (int) boxLeft + 20, (int) y - 30);
//        }
//        for (int slot = 0; slot <= 3; slot++){
//            if (entity.getCurrentArmor(slot) == null) continue;
//            mc.getRenderItem().renderItemIntoGUI(entity.getCurrentArmor(slot), (int) boxLeft + 20 - (slot * 15) + 65, (int) y - 30);
//        }
//
//        RoundedUtils.drawRound(boxLeft, y - (fontHeight + 7.0f), tagWidth, fontHeight, 0f, new Color(19, 19, 19, 140));
//        float textX = boxLeft + (tagWidth - textWidth) / 2.0f;
//        mc.fontRendererObj.drawString(text,(int) textX, (int)(y - (fontHeight + 6.5f) + 1.0f), -1);
//    }


    private String getItemDisplayName(ItemStack stack) {
        if (stack.getItem() == Items.golden_apple) {
            return stack.getMetadata() == 1 ?
                    EnumChatFormatting.GOLD + "附魔金苹果" :
                    EnumChatFormatting.YELLOW + "金苹果";
        } else if (stack.getItem() == Items.gold_ingot) {
            return EnumChatFormatting.GOLD + "金锭";
        } else if (stack.getItem() == Items.iron_ingot) {
            return EnumChatFormatting.GRAY + "铁锭";
        } else if (stack.getItem() == Items.diamond) {
            return EnumChatFormatting.AQUA + "钻石";
        }
        return stack.getDisplayName();
    }


    private int getItemColor(ItemStack stack) {
        if (stack.getItem() == Items.golden_apple) {
            return stack.getMetadata() == 1 ? new Color(255, 170, 0).getRGB() : new Color(255, 255, 85).getRGB();
        } else if (stack.getItem() == Items.gold_ingot) {
            return new Color(255, 255, 85).getRGB();
        } else if (stack.getItem() == Items.iron_ingot) {
            return new Color(200, 200, 200).getRGB();
        } else if (stack.getItem() == Items.diamond) {
            return new Color(85, 255, 255).getRGB();
        }
        return -1;
    }

    private String getRank(EntityLivingBase entity) {
        if (entity instanceof EntityPlayer) {
            final String name = entity.getName();
            final HackerPlayer.ClientId usingClient = Optional.ofNullable(LiveClient.INSTANCE.getLiveComponent().getHackerMap().get(name))
                    .map(HackerPlayer::getClient)
                    .orElse(null);
            if (usingClient != null) {
                switch (usingClient) {
                    case SOUTHSIDE -> {
                        return EnumChatFormatting.GOLD.toString()
                                + EnumChatFormatting.BOLD
                                + "[SouthSide] "
                                + EnumChatFormatting.RESET;
                    }
                    case GUARD_FIX -> {
                        return EnumChatFormatting.LIGHT_PURPLE.toString()
                                + EnumChatFormatting.ITALIC
                                + "[最强公益客户端] "
                                + EnumChatFormatting.RESET;
                    }
                    case OTHERS -> {
                        return EnumChatFormatting.DARK_RED.toString()
                                + EnumChatFormatting.BOLD
                                + "[外挂] "
                                + EnumChatFormatting.RESET;
                    }
                }
            }

            if (entity == mc.thePlayer) {
                return EnumChatFormatting.GREEN + "[You] ";
            }
            if (entity == KillAura.target) {
                return EnumChatFormatting.DARK_RED + "[杀戮目标] ";
            }
            if (Objects.requireNonNull(ModuleManager.getModule(Teams.class)).getState() && Teams.isSameTeam(entity)) {
                return EnumChatFormatting.GREEN + "[队伍] ";
            }
            if (HYTUtils.isStrength((EntityPlayer) entity) > 0 && entity != mc.thePlayer && !Teams.isSameTeam(entity)) {
                return EnumChatFormatting.DARK_RED + "[力量狗] ";
            }
            if (HYTUtils.isRegen((EntityPlayer) entity) > 0 && entity != mc.thePlayer && !Teams.isSameTeam(entity)) {
                return EnumChatFormatting.DARK_RED + "[生命恢复狗] ";
            }
            if (HYTUtils.isHoldingGodAxe((EntityPlayer) entity) && entity != mc.thePlayer && !Teams.isSameTeam(entity)) {
                return EnumChatFormatting.DARK_RED + "[秒人斧] ";
            }
            if (HYTUtils.isKBBall(entity.getHeldItem()) && entity != mc.thePlayer && !Teams.isSameTeam(entity)) {
                return EnumChatFormatting.DARK_RED + "[击退粘液球] ";
            }
            if (HYTUtils.hasEatenGoldenApple((EntityPlayer) entity) > 0 && entity != mc.thePlayer && !Teams.isSameTeam(entity)) {
                return EnumChatFormatting.DARK_RED + "[金苹果] ";
            }
        }
        return "";
    }

    @EventTarget
    public void onShaderEvent(EventShader e) {
        for (Entity entity : entityPosition.keySet()) {
            Vector4f pos = entityPosition.get(entity);
            if (!(entity instanceof EntityLivingBase)) continue;

            EntityLivingBase living = (EntityLivingBase) entity;
//            HUD hud = this.getModule(HUD.class);
//            switch ((HUD.HUDmode) hud.hudModeValue.getValue()) {
//                case Silence:
//                case XinXin:
//                    if (isSelfOrIRC(living)) {
//                        drawOwnNameTag(living, pos.getX(), pos.getY(), pos.getZ(), false);
//                    } else {
                        drawOpponentNameTag(living, pos.getX(), pos.getY(), pos.getZ());
//                    }
//                    break;
//            }
        }

//        for (EntityItem item : itemPosition.keySet()) {
//            Vector4f pos = itemPosition.get(item);
//            drawItemTag(item, pos.getX(), pos.getY(), pos.getZ());
//        }
    }

    @EventTarget
    public void onRender2DEvent(EventRender2D e) {
        for (EntityItem item : itemPosition.keySet()) {
            Vector4f pos = itemPosition.get(item);
            drawItemTag(item, pos.getX(), pos.getY() + 10, pos.getZ());
        }

        for (Entity entity : entityPosition.keySet()) {
            Vector4f pos = entityPosition.get(entity);
            if (!(entity instanceof EntityLivingBase living)) continue;

            drawOpponentNameTag(living, pos.getX(), pos.getY(), pos.getZ());

        }
    }

    private boolean isSelfOrIRC(EntityLivingBase entity) {
        if (entity == mc.thePlayer) return true;
        if (entity instanceof EntityPlayer player) {
            return player.liveUser != null;
        }
        return false;
    }

    private boolean shouldRender(Entity entity) {
        if (entity.isDead) {
            return false;
        }
        if (entity == mc.thePlayer && playersValue.getValue()) {
            return mc.gameSettings.thirdPersonView != 0;
        }

        if (entity instanceof EntityPlayer && playersValue.getValue()) {
            return true;
        }

        if (entity instanceof EntityAnimal && animalsValue.getValue()) {
            return true;
        }

        if (entity instanceof EntityMob && mobsValue.getValue()) {
            return true;
        }

        if (entity.isInvisible() && invisibleValue.getValue()) {
            return true;
        }

        return false;
    }

    private boolean shouldRenderItem(Entity entity) {
        if (!(entity instanceof EntityItem item)) return false;
        return !item.isDead;
    }


    private boolean isTargetItem(ItemStack stack) {
        return stack != null && (
                stack.getItem() == Items.golden_apple ||
                        stack.getItem() == Items.gold_ingot ||
                        stack.getItem() == Items.iron_ingot ||
                        stack.getItem() == Items.diamond
        );
    }
}