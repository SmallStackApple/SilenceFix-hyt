package dev.xinxin.module.modules.combat;

import dev.xinxin.SilenceFix;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventMotion;
import dev.xinxin.event.world.EventUpdate;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.modules.misc.AutoGapple;
import dev.xinxin.module.modules.player.Blink;
import dev.xinxin.module.modules.player.ChestStealer;
import dev.xinxin.module.modules.world.ChestAura;
import dev.xinxin.module.values.BoolValue;
import dev.xinxin.module.values.NumberValue;
import dev.xinxin.utils.InventoryUtil;
import dev.xinxin.utils.RotationComponent;
import dev.xinxin.utils.TimerUtils;
import dev.xinxin.utils.client.HelperUtil;
import dev.xinxin.utils.player.RotationNew;
import dev.xinxin.utils.player.RotationUtil;
import dev.yalan.live.silencefix.LiveComponent;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.util.vector.Vector2f;

import java.util.Comparator;
import java.util.Optional;

import dev.xinxin.module.modules.render.Projectile;

public class AutoProjectile extends Module {
    public NumberValue minRangeValue = new NumberValue("Min Range", 3, 2, 10, 0.1);
    public NumberValue maxRangeValue = new NumberValue("Max Range", 8, 2, 16, 0.1);
    public NumberValue delayValue = new NumberValue("Delay", 200, 0, 1000, 50);
    public NumberValue predictValue = new NumberValue("Predict", 1.5, 0, 2, 0.1);
    public BoolValue autoSwitchValue = new BoolValue("AutoSwitch", true);
    public BoolValue wallCheckValue = new BoolValue("WallCheck", true);
    private static boolean pausedForPearl = false;
    public static boolean isThrowing = false;

    TimerUtils delay = new TimerUtils();
    public EntityLivingBase target;
    boolean she;
    private int targetSlot;

    public AutoProjectile() {
        super("AutoSnowBall", Category.Combat,"自动雪球鸡蛋");
    }

    public static void setPausedForPearl(boolean paused) {
        pausedForPearl = paused;
    }

    public boolean manualClose = false;
    private RotationNew rotationNew;

    @Override
    public void onEnable() {
        if (mc.thePlayer == null) return;
        this.she = false;
        delay.reset();
        checkAndToggleBasedOnWeapons();
        targetSlot = -1;
        this.manualClose = false;
        rotationNew = null;
        pausedForPearl = false;
        isThrowing = false;
        Projectile.disableOverride();
    }

    @Override
    public void onDisable() {
        targetSlot = -1;
        this.she = false;
        rotationNew = null;
        this.manualClose = true;
        pausedForPearl = false;
        isThrowing = false;
        Projectile.disableOverride();
    }

    public static Vector2f targetRotation = null;

    private boolean isKillAuraActive() {
        Module killAura = SilenceFix.instance.moduleManager.getModule(KillAura.class);
        return killAura != null && killAura.getState() && KillAura.target != null;
    }

    private boolean isFriend(EntityPlayer player) {
        if (player == null) return false;
        return SilenceFix.instance.getFriendManager().isFriend(player.getName());
    }

    @EventTarget
    public void onUpdate(EventUpdate e) {
        if (getThrowSlot() == -1) {
            this.she = false;
            Projectile.disableOverride();
            return;
        }

        boolean shouldDisable = shouldDisableBasedOnWeapons();
        if (shouldDisable) {
            if (this.getState()) {
                HelperUtil.sendMessage("§f破甲有效，自动关闭雪球");
                this.setState(false);
            }
        } else {
            if (!this.getState()) {
                HelperUtil.sendMessage("§f破甲失效，自动开启雪球");
                SilenceFix.instance.moduleManager.getModule(AutoProjectile.class).setState(true);
            }
        }

        if (shouldSkip()) {
            stop();
            return;
        }

        if (AutoGapple.eating) {
            stop();
            return;
        }

        if (pausedForPearl) {
            return;
        }

        boolean hasProjectiles = getThrowSlot() != -1;
        if (!hasProjectiles && autoSwitchValue.getValue()) {
            this.she = false;
            Projectile.disableOverride();
        }

        Optional<EntityOtherPlayerMP> target = getTarget(
                minRangeValue.getValue().floatValue(),
                maxRangeValue.getValue().floatValue());
        this.target = null;
        if (target.isEmpty()) {
            this.she = false;
            Projectile.disableOverride();
            return;
        }

        EntityLivingBase player = target.get();
        if (isFriend((EntityPlayer) player)) {
            this.she = false;
            Projectile.disableOverride();
            return;
        }

        this.target = player;
        double distance = mc.thePlayer.getDistanceToEntity(player);

        Vec3 motion = new Vec3(
                player.posX - player.lastTickPosX,
                0,
                player.posZ - player.lastTickPosZ
        );

        float predictFactor = (float) (distance * predictValue.getValue() * 0.5);
        double heightFactor = Math.max(0, (player.posY - mc.thePlayer.posY)) * 0.05;

        Vec3 predictedPos = new Vec3(
                player.posX + motion.xCoord * predictFactor,
                player.posY + player.getEyeHeight() - (distance * 0.013 + heightFactor),
                player.posZ + motion.zCoord * predictFactor
        );

        if (distance > 6) {
            predictedPos = new Vec3(
                    predictedPos.xCoord + motion.xCoord * predictFactor * 0.3,
                    predictedPos.yCoord,
                    predictedPos.zCoord + motion.zCoord * predictFactor * 0.3
            );
        }

        RotationNew rotationNew = RotationUtil.toRotation(predictedPos, 0F);
        targetRotation = new Vector2f(
                MathHelper.wrapAngleTo180_float(rotationNew.getYaw()),
                MathHelper.clamp_float(rotationNew.getPitch(), -90, 90)
        );

        if (RotationComponent.lastRotation == null) {
            RotationComponent.lastRotation = new Vector2f(
                    mc.thePlayer.rotationYaw,
                    mc.thePlayer.rotationPitch
            );
        }

        float rotationSpeed = (float) Math.min(25, 10 + distance * 1.5);

        RotationComponent.setRotation(targetRotation, rotationSpeed, true, false);
        isThrowing = true;

        int slot = getThrowSlot();
        if (slot != -1) {
            ItemStack s = mc.thePlayer.inventory.getStackInSlot(slot);
            if (s != null && s.getItem() != null) {
                Projectile.enableOverride(s.getItem(), targetRotation.x, targetRotation.y, 1.0f);
            } else {
                Projectile.disableOverride();
            }
        } else {
            Projectile.disableOverride();
        }

        this.she = hasProjectiles;
    }

    private boolean shouldDisableBasedOnWeapons() {
        if (mc.thePlayer == null || mc.thePlayer.inventoryContainer == null) {
            return false;
        }
        boolean hasSharpXAxe = false;
        boolean hasSharp2GoldSword = false;
        for (int i = 0; i < InventoryUtil.END; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && stack.getItem() != null) {
                if (stack.getItem() instanceof ItemAxe) {
                    int sharpness = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack);
                    if (sharpness >= 10) {
                        hasSharpXAxe = true;
                    }
                }
                else if (stack.getItem() == Items.golden_sword) {
                    int sharpness = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack);
                    if (sharpness >= 2) {
                        hasSharp2GoldSword = true;
                    }
                }
            }
        }
        return hasSharpXAxe || hasSharp2GoldSword;
    }

    private void checkAndToggleBasedOnWeapons() {
        boolean shouldDisable = shouldDisableBasedOnWeapons();
        if (shouldDisable && this.getState()) {
            HelperUtil.sendMessage("§f准备破甲,禁止开启");
            this.setState(false);
        } else if (shouldDisable && !this.getState()) {
            HelperUtil.sendMessage("§f准备破甲 禁止关闭");
        } else if (!shouldDisable && !this.getState()) {
            this.setState(true);
        }
    }

    private boolean shouldSkip() {
        return AutoGapple.eating ||
                SilenceFix.instance.moduleManager.getModule(Blink.class).getState() ||
                isHoldingAxe() ||
                isPullingBow() ||
                isMining() ||
                ChestStealer.working ||
                ChestAura.confirmedOpenedContainers.contains(ChestStealer.currentContainerPos) ||
                mc.currentScreen instanceof GuiChest;
    }

    private boolean isPullingBow() {
        return mc.thePlayer.isUsingItem() &&
                mc.thePlayer.getHeldItem() != null &&
                mc.thePlayer.getHeldItem().getItem() == Items.bow;
    }

    private boolean isMining() {
        return mc.gameSettings.keyBindAttack.isKeyDown() &&
                mc.objectMouseOver != null &&
                mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;
    }

    private boolean isHoldingAxe() {
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        return heldItem != null && heldItem.getItem() instanceof ItemAxe;
    }

    @EventTarget
    public void onMotion(EventMotion event) {
        if (pausedForPearl || AutoGapple.eating || event.isPre()) {
            if (targetSlot >= 0) {
                mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                targetSlot = -1;
            }
            return;
        }

        if (target == null) {
            if (targetSlot >= 0) {
                mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                targetSlot = -1;
            }
            Projectile.disableOverride();
            return;
        }

        ItemStack held = mc.thePlayer.inventory.getCurrentItem();

        if (RotationComponent.lastRotation != null && !isKillAuraActive() && (held == null || held.getItem() == null || held.getItem() != Items.bow)) {
            int slot = getThrowSlot();
            if (slot != -1) {
                targetSlot = slot;
            }
        }

        if (target != null && targetSlot >= 0 &&
                RotationComponent.lastRotation != null &&
                delay.hasReached(delayValue.getValue().longValue())) {

            mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(targetSlot));
            mc.thePlayer.sendQueue.addToSendQueueUnregisteredNoEvent(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
            mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
            targetSlot = -1;
            isThrowing = false;
            delay.reset();
            Projectile.disableOverride();
        }
    }

    private int getThrowSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && (stack.getItem() == Items.snowball || stack.getItem() == Items.egg)) {
                return i;
            }
        }
        return -1;
    }

    public static Optional<EntityOtherPlayerMP> getTarget(float min, float max) {
        final double minRange = min * min;
        final double maxRange = max * max;
        return mc.theWorld.loadedEntityList.stream()
                .filter(entity -> entity instanceof EntityOtherPlayerMP)
                .filter(Entity::isEntityAlive)
                .map(entity -> (EntityOtherPlayerMP) entity)
                .filter(entity -> !SilenceFix.instance.getFriendManager().isFriend(entity.getName()))
                .filter(entityOtherPlayerMP -> LiveComponent.isAttackable(entityOtherPlayerMP.liveUser))
                .filter(entityLivingBase -> mc.thePlayer.getDistanceSq(entityLivingBase) <= maxRange &&
                        mc.thePlayer.getDistanceSq(entityLivingBase) >= minRange)
                .filter(entityLivingBase -> !SilenceFix.instance.moduleManager.getModule(AutoProjectile.class).wallCheckValue.getValue() ||
                        !isTargetBehindWall(entityLivingBase))
                .min(Comparator.comparingDouble(entity -> mc.thePlayer.getDistanceSq(entity)));
    }

    private static boolean isTargetBehindWall(EntityLivingBase target) {
        Vec3 playerEyes = new Vec3(
                mc.thePlayer.posX,
                mc.thePlayer.posY + mc.thePlayer.getEyeHeight(),
                mc.thePlayer.posZ
        );

        Vec3 targetEyes = new Vec3(
                target.posX,
                target.posY + target.getEyeHeight(),
                target.posZ
        );

        MovingObjectPosition result = mc.theWorld.rayTraceBlocks(
                playerEyes,
                targetEyes,
                false,
                true,
                false
        );

        return result != null && result.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;
    }

    private void stop() {
        target = null;
        targetSlot = -1;
        pausedForPearl = false;
        isThrowing = false;
        she = false;
        Projectile.disableOverride();
    }
}
