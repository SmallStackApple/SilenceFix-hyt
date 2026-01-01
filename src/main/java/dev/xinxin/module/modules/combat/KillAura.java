package dev.xinxin.module.modules.combat;

import com.xinxin.client.viaversion.viamcp.fixes.AttackOrder;
import dev.xinxin.SilenceFix;
import dev.xinxin.event.EventManager;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.attack.EventAttack;
import dev.xinxin.event.rendering.EventRender2D;
import dev.xinxin.event.rendering.EventRender3D;
import dev.xinxin.event.world.*;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.ModuleManager;
import dev.xinxin.module.modules.misc.Teams;
import dev.xinxin.module.modules.player.Blink;
import dev.xinxin.module.modules.render.HUD;
import dev.xinxin.module.modules.world.Scaffold;
import dev.xinxin.module.modules.world.Stuck;
import dev.xinxin.module.values.BoolValue;
import dev.xinxin.module.values.ColorValue;
import dev.xinxin.module.values.ModeValue;
import dev.xinxin.module.values.NumberValue;
import dev.xinxin.utils.MovementFix;
import dev.xinxin.utils.RayCastUtil;
import dev.xinxin.utils.RotationComponent;
import dev.xinxin.utils.client.MathUtil;
import dev.xinxin.utils.client.TimeUtil;
import dev.xinxin.utils.player.PredictionUtil;
import dev.xinxin.utils.player.RotationNew;
import dev.xinxin.utils.player.RotationUtil;
import dev.xinxin.utils.render.ColorUtil;
import dev.xinxin.utils.render.RenderUtil;
//import dev.yalan.live.silencefix.LiveComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.monster.EntityGhast;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.potion.Potion;
import net.minecraft.util.*;
import net.minecraft.world.WorldSettings;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


public class KillAura extends Module {
    public static EntityLivingBase target;
    public static List<Entity> targets;
    private final TimeUtil lossTimer = new TimeUtil();
    public static float[] KaRotation;
    private float randomYaw;
    private float randomPitch;
    public static float[] lastRotation;
    private static double currentRange = 3.0;
    double originalCpsValue = cpsValue.getValue();


    public static BoolValue rayCastValue = new BoolValue("RayCast", false);
    public BoolValue autoBlock = new BoolValue("Auto block", true);
    private final BoolValue keepSprint = new BoolValue("Keep sprint", true);
    public BoolValue playersValue = new BoolValue("Players", true);
    public BoolValue animalsValue = new BoolValue("Animals", true);
    public BoolValue mobsValue = new BoolValue("Mobs", false);
    public BoolValue invisibleValue = new BoolValue("Invisible", false);
    public static BoolValue breakc = new BoolValue("Break Check", false);
    public BoolValue footCircle = new BoolValue("Circle", true);
    public ColorValue circleColor = new ColorValue("Circle Color", Color.YELLOW.getRGB());
    public NumberValue circleAlpha = new NumberValue("Circle Alpha", 200, 1, 255, 1);


    @EventTarget
    public void render3D(EventRender3D event) {
        if (!footCircle.getValue()) return;
        EntityPlayer player = KillAura.mc.thePlayer;
        double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * KillAura.mc.timer.renderPartialTicks - mc.getRenderManager().renderPosX;
        double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * KillAura.mc.timer.renderPartialTicks - mc.getRenderManager().renderPosY;
        double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * KillAura.mc.timer.renderPartialTicks - mc.getRenderManager().renderPosZ;
        float radius = maxRange.getValue().floatValue();
        int color = ColorUtil.withAlpha(circleColor.getColorC(), circleAlpha.getValue().intValue()).getRGB();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableDepth();
        GlStateManager.disableTexture2D();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.0f);
        GlStateManager.rotate(90F, 1F, 0F, 0F);
        RenderUtil.color(color);
        GlStateManager.glBegin(GL11.GL_LINE_STRIP);
        for (int i = 0; i <= 360; i += 2) {
            double rad = Math.toRadians(i);
            GL11.glVertex2f((float) (Math.cos(rad) * radius), (float) (Math.sin(rad) * radius));
        }
        GlStateManager.glEnd();
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GL11.glLineWidth(2);
        if (target == null) {
            return;
        }
        if (target.isDead || target.getHealth() <= 0 || KillAura.mc.thePlayer.isSpectator())
            return;
    }

    public static final NumberValue cpsValue;
    public static final NumberValue cpsTick;
    public static NumberValue minRange = new NumberValue("Min Range", 3.0, 3.0, 7.0, 0.1);
    public static NumberValue maxRange = new NumberValue("Max Range", 3.0, 3.0, 7.0, 0.1);
    public NumberValue findRange = new NumberValue("ESP Range", 4.0, 1.0, 6.0, 0.1);
    public NumberValue blockRange = new NumberValue("Block Range", 3.0, 1.0, 6.0, 0.01);
    public NumberValue Fov = new NumberValue("Fov", 360.0, 0.0, 360.0, 1.0);
    public NumberValue switchDelay = new NumberValue("SwitchDelay", 200.0, 1.0, 1000.0, 1.0);

    public static ModeValue<AuraModes> Mode;
    public static ModeValue<RotationModes> RotationMode;
    public static ModeValue<AttackingModes> AttackingMode;
    private final ModeValue<TargetGetModes> targetGetMode = new ModeValue<>("Target Select", TargetGetModes.values(), TargetGetModes.Silence);
    public ModeValue<MarkMode> mark = new ModeValue<>("Mark Styles", MarkMode.values(), MarkMode.Normal);
    private final ModeValue<MovementFix> moveType = new ModeValue<>("MoveFix Mode", MovementFix.values(), MovementFix.HeShuYou);

    public static boolean isBlocking;
    public static boolean renderBlocking;
    public static boolean strict;
    private final Comparator<Entity> angleComparator = Comparator.comparingDouble(e2 -> mc.thePlayer.getClosestDistanceToEntity(e2));
    private final Comparator<Entity> healthComparator = Comparator.comparingDouble(e2 -> ((EntityLivingBase) e2).getHealth());
    private final TimeUtil switchTimer = new TimeUtil();
    private int index;

    private float lastHp;
    private boolean locked = false;

    public KillAura() {
        super("KillAura", Category.Combat, "杀戮光环");
    }


    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isLocked() {
        return locked;
    }

    @Override
    public void onDisable() {
        this.lossTimer.reset();
        targets.clear();
        autoBlock(true);
        index = 0;
        tick = 0;
        this.resetRo();
        renderBlocking = false;
        isBlocking = false;
        target = null;
        KaRotation = null;
        lastRotation = new float[]{0f, 0f, 0f};
    }

    private int tick;
    private final TimeUtil timeUtil = new TimeUtil();

    @EventTarget
    public void onWorld(EventWorldLoad event) {
        this.setState(false);
        target = null;
        targets.clear();
        KaRotation = null;
        lastRotation = new float[]{0f, 0f, 0f};
        isBlocking = false;
        renderBlocking = false;
        this.resetRo();
        index = 0;
        tick = 0;
    }



    @Override
    public void onEnable() {
        if (KillAura.mc.thePlayer == null) {
            return;
        }
        this.resetRo();
        isBlocking = false;
        this.lossTimer.reset();
        index = 0;

        tick = 0;
        timeUtil.reset();
        target = null;
    }


    public float getClosestDistanceToEntity(Entity target, Entity entityIn) {
        return entityIn.getClosestDistanceToEntity(target);
    }



    @EventTarget
    public void onM(EventUpdate event) {
        if (ModuleManager.getModule(Blink.class).state) {
            target = null;
            return;
        }
        this.setSuffix(Mode.getValue());

        List<Entity> attackTargets = mc.theWorld.loadedEntityList.stream()
                .filter(entity -> entity instanceof EntityLivingBase)
                .map(EntityLivingBase.class::cast)
                .filter(it -> this.isValid(it, findRange.getValue()))
                .sorted((e1, e2) -> (int) (mc.thePlayer.getClosestDistanceToEntity(e1) - mc.thePlayer.getClosestDistanceToEntity(e2)))
                .collect(Collectors.toList());

        if (attackTargets.stream().anyMatch((e) -> !(e instanceof EntityPlayer) || ((EntityPlayer) e).liveUser == null)) {
            attackTargets = attackTargets.stream()
                    .filter((e) -> !(e instanceof EntityPlayer && ((EntityPlayer) e).liveUser != null))
                    .collect(Collectors.toList());
        }

        if (attackTargets.isEmpty()) {
            if (RotationMode.getValue() == RotationModes.XinXin) {
                this.randomiseTargetRotations();
            }
            target = null;
        }

        targets = attackTargets;

        EntityPlayerSP thePlayer = KillAura.mc.thePlayer;
        switch (this.targetGetMode.getValue()) {
            case Silence -> targets.sort(this.angleComparator);
            case RangeFix -> targets.sort((o1, o2) -> (int) (o1.getClosestDistanceToEntity(thePlayer) - o2.getClosestDistanceToEntity(thePlayer)));
            case XinXin -> targets.sort(this.healthComparator);
        }

        Entity tempTarget = targets.stream().findFirst().orElse(null);

        if (target != null && !isValid(target, findRange.getValue())) {
            target = null;
        }
        if (target == null || !targets.contains(target)) {
            index = 0;
            target = (EntityLivingBase) tempTarget;
        } else {
            index = targets.indexOf(target);
        }


        if (Mode.getValue() == AuraModes.Switch || Mode.getValue() == AuraModes.Multiple) {
            if (switchTimer.delay(switchDelay.getValue().longValue()) && !targets.isEmpty()) {
                index = (index + 1) % targets.size();
                target = (EntityLivingBase) targets.get(index);

                int trial = 0;
                while (trial < targets.size()) {
                    if (mc.thePlayer.getClosestDistanceToEntity(target) <= maxRange.getValue()) {
                        break;
                    }
                    index = (index + 1) % targets.size();
                    target = (EntityLivingBase) targets.get(index);
                    trial++;
                }

                switchTimer.reset();
            }
        } else {
            index = -1;
            target = null;
            for (int i = 0; i < targets.size(); i++) {
                if (mc.thePlayer.getClosestDistanceToEntity(targets.get(i)) <= maxRange.getValue()) {
                    index = i;
                    target = (EntityLivingBase) targets.get(i);
                    break;
                }
            }
        }

        if (targets.size() > 1 && Mode.getValue() == AuraModes.HeShuYou) {
            if (mc.thePlayer.getClosestDistanceToEntity(target) > maxRange.getValue() || target.isDead) {
                target = (EntityLivingBase) tempTarget;
            }
        }

        if (!targets.isEmpty() && AttackingMode.is("Update")) {
            this.attack();
        } else {
            this.resetRo();
        }

        if (ModuleManager.getModule(Scaffold.class).state){
            if (isBlocking){
                autoBlock(true);
            }
            if (target != null && ModuleManager.getModule(Scaffold.class).blockPos == null && mc.thePlayer.ticksExisted % 2 == 0) {
                this.doRotation(target);
            }
            return;
        }

        if (target != null) {
            this.doRotation(target);
            autoBlock(false);
        } else {
            autoBlock(true);
        }
    }

    public static boolean isSpectator() {
        return mc.playerController != null &&
                mc.playerController.getCurrentGameType() != null &&
                mc.playerController.getCurrentGameType() == WorldSettings.GameType.SPECTATOR;
    }


    public static boolean breakCheck = true;

    @EventTarget
    public void onPS(EventPacketSend packet){
        if (!breakc.getValue()) return;
        breakCheck = !(packet.getPacket() instanceof C07PacketPlayerDigging && mc.gameSettings.keyBindAttack.isKeyDown());
    }

    private AuraModes previousMode = AuraModes.Switch;


    @EventTarget
    private void onMotion(EventMotion event) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.thePlayer.isDead) {
            target = null;
            targets.clear();
            this.setState(false);
            return;
        }
        if (isSpectator()) {
            this.setState(false);
            target = null;
            targets.clear();
            return;
        }
        if (!breakCheck) return;

        if (ModuleManager.getModule(Blink.class).state) return;
        if (ModuleManager.getModule(Stuck.class).state)
            if (event.isPre()) return;


    }

    private final TimeUtil hurtCooldownTimer = new TimeUtil();
    private boolean isHurtLimited = false;
    int lastHurtTime = 0;

    @EventTarget
    public void doAttackEntity(EventMotion event) {
        if (mc.thePlayer == null || mc.thePlayer.isDead) return;
        if (isSpectator()) {
            this.setState(false);
            target = null;
            targets.clear();
            return;
        }
        if (!breakCheck) return;

        Module blink = ModuleManager.getModule(Blink.class);
        if (blink != null && blink.getState()) return;

        if (event.isPost() && (AttackingMode.is("Fix") || AttackingMode.is("Post"))) {
            Module bw = SilenceFix.instance.moduleManager.getModule("-----BW-----");
            Module bwbest = SilenceFix.instance.moduleManager.getModule("-----BWBEST-----");
            boolean bwModeEnabled = (bw != null && bw.getState()) || (bwbest != null && bwbest.getState());

            boolean limitEnabled = bwModeEnabled && cpsTick.getValue().intValue() > 0;
            int currentHurtTime = mc.thePlayer.hurtTime;

            if (limitEnabled) {
                if (currentHurtTime > 0 && lastHurtTime == 0) {
                    hurtCooldownTimer.reset();
                    if (!isHurtLimited) {
                        isHurtLimited = true;
                        previousMode = Mode.getValue();
                        Mode.setValue(AuraModes.HeShuYou);
                        this.originalCpsValue = cpsValue.getValue();
                        cpsValue.setValue(16.0);
                    }
                }

                if (isHurtLimited) {
                    tick++;
                    if (Mode.getValue() != AuraModes.HeShuYou) {
                        Mode.setValue(AuraModes.HeShuYou);
                    }
                    if (tick >= cpsTick.getValue()) {
                        tick = 0;
                        if (hurtCooldownTimer.hasReached(2000)) {
                            isHurtLimited = false;
                            Mode.setValue(previousMode);
                            cpsValue.setValue(this.originalCpsValue);
                            Mode.setValue(AuraModes.Switch);
                        }
                    } else {
                        return;
                    }
                }
            } else {
                if (isHurtLimited) {
                    isHurtLimited = false;
                    Mode.setValue(previousMode);
                    tick = 0;
                    cpsValue.setValue(this.originalCpsValue);
                }
            }

            lastHurtTime = currentHurtTime;
            if (target != null && shouldAttack()) {
                attack();
            }
        }
    }


    private void autoBlock(boolean stop) {
        if (!autoBlock.getValue())return;
        if (mc.thePlayer == null || mc.thePlayer.isDead) {
            isBlocking = false;
            renderBlocking = false;
            return;
        }
        if (stop) {
            if (isBlocking && mc.thePlayer.getHeldItem() != null &&
                    mc.thePlayer.getHeldItem().getItem() instanceof ItemSword) {
                mc.thePlayer.sendQueue.addToSendQueueUnregisteredNoEvent(
                        new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                                BlockPos.ORIGIN, EnumFacing.DOWN));
                mc.thePlayer.setItemInUse(null, 0);
            }
            isBlocking = false;
            renderBlocking = false;
        } else {
            if (target != null && mc.thePlayer.getClosestDistanceToEntity(target) <= (blockRange.getValue() + 0.5)) {
                if (mc.thePlayer.getHeldItem() != null &&
                        mc.thePlayer.getHeldItem().getItem() instanceof ItemSword) {
                    isBlocking = true;
                    renderBlocking = true;
                    mc.thePlayer.sendQueue.addToSendQueueUnregisteredNoEvent(
                            new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                    mc.thePlayer.setItemInUse(mc.thePlayer.getHeldItem(), 72000);
                }
            }
        }
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (targets == null) return;

        switch (mark.value) {
            case Normal -> {
                for (Entity entity : targets) {
                    if (!(entity instanceof EntityLivingBase target)) continue;

                    AxisAlignedBB bb = PredictionUtil.PredictedTarget(target, 2);
                    if (bb == null) continue;

                    if (Double.isNaN(bb.minX) || Double.isNaN(bb.minY) || Double.isNaN(bb.minZ)
                            || Double.isNaN(bb.maxX) || Double.isNaN(bb.maxY) || Double.isNaN(bb.maxZ)) continue;
                    if (bb.minX > bb.maxX || bb.minY > bb.maxY || bb.minZ > bb.maxZ) continue;

                    Color color = new Color(0, 255, 0, 100);
                    if (target == KillAura.target) {
                        int alpha = target.hurtTime * 5;
                        color = new Color(255, 0, 0, Math.min(255, 100 + alpha));
                    }

                    RenderUtil.boundingESPBoxFilled(bb, color);
                }

            }
            case Modern -> {
                if (target == null) return;
                float dst = mc.thePlayer.getDistanceToEntity(target);
                javax.vecmath.Vector2f vector2f = RenderUtil.targetESPSPos(target);
                if (vector2f != null) RenderUtil.drawTargetESP2D(vector2f.x, vector2f.y, HUD.color(1),HUD.color(8),HUD.color(3),HUD.color(10), (1.0F - MathHelper.clamp_float(Math.abs(dst - 6.0F) / 60.0F, 0.0F, 0.75F)), 1);
            }
        }
    }

    @EventTarget
    void onSlowDownEvent(EventSlowDown event) {
//        if (SilenceFix.instance.moduleManager.getModule(Scaffold.class).state && Scaffold.canTellyPlace) return;
        if (ModuleManager.getModule(Scaffold.class).getState()) return;

        if (isSpectator()) {
            this.setState(false);
            target = null;
            targets.clear();
            return;
        }
        if (!breakCheck) {
            return;
        }

        if (event.getType() == EventSlowDown.Type.Sprinting && this.keepSprint.getValue().booleanValue()) {
            event.setCancelled(true);
        }
    }


    private void randomiseTargetRotations() {
        this.randomYaw += (float) (Math.random() - 0.5);
        this.randomPitch += (float) (Math.random() - 0.5) * 2.0f;
    }

    private void doRotation(EntityLivingBase target) {
        if (mc.thePlayer == null || mc.thePlayer.isDead) return;
        float[] rot = RotationUtil.getHVHRotation(target, currentRange);
        currentRange = MathUtil.getRandomInRange(minRange.getValue(), maxRange.getValue());
        if (mc.thePlayer.getClosestDistanceToEntity(target) <= blockRange.getValue()) {
            switch (RotationMode.getValue()) {
                case Normal: {
                    Vector2f vec = RotationUtil.calculate(target, true, currentRange, currentRange, true, true);
                    rot = new float[]{vec.x, vec.y};
                    break;
                }
                case Silence: {
                    final RotationNew rotation = RotationUtil.calculate(target, blockRange.getValue(), blockRange.getValue(), 1F, 1F);
                    if (rotation != null) {
                        KaRotation = new float[]{rotation.getYaw(), rotation.getPitch()};
                    }
                    break;
                }
            }
            if (KaRotation != null) {
                lastRotation[0] = KaRotation[0];
                lastRotation[2] = Math.min(90.0f, KaRotation[1]);
                RotationComponent.setRotation(new Vector2f(lastRotation[0], lastRotation[2]), 20.0f, true, moveType.getValue().equals(MovementFix.Strict));
            }
        }
    }

    private void resetRo() {
        if (mc.thePlayer == null) return;
        lastRotation = new float[]{
                KillAura.mc.thePlayer.rotationYaw,
                KillAura.mc.thePlayer.renderYawOffset,
                KillAura.mc.thePlayer.rotationPitch
        };
    }


    private void attackEntity(Entity target) {
        currentRange = MathUtil.getRandomInRange(minRange.getValue(), maxRange.getValue());

        double distance = mc.thePlayer.getClosestDistanceToEntity(target);
        if (distance <= currentRange) {
            AttackOrder.sendFixedAttack(mc.thePlayer, target);
        }
    }

    private void attack() {

        if (mc.thePlayer == null || mc.thePlayer.isDead || target == null || target.isDead) return;
        if (shouldAttack()) {

            if (target != null) {
                {
                    EventManager.call(new EventAttack(target, true));
                    this.attackEntity(target);
                    EventManager.call(new EventAttack(target, false));
                    if (this.keepSprint.getValue().booleanValue()) {
                        if (!(!(KillAura.mc.thePlayer.fallDistance > 0.0f) || KillAura.mc.thePlayer.onGround || KillAura.mc.thePlayer.isOnLadder() || KillAura.mc.thePlayer.isInWater() || KillAura.mc.thePlayer.isPotionActive(Potion.blindness) || KillAura.mc.thePlayer.ridingEntity != null)) {
                            KillAura.mc.thePlayer.onCriticalHit(target);
                        }
                        if (EnchantmentHelper.getModifierForCreature(KillAura.mc.thePlayer.getHeldItem(), target.getCreatureAttribute()) > 0.0f) {
                            KillAura.mc.thePlayer.onEnchantmentCritical(target);

                        }
                    }
                }
            }
        }

    }

    public List<Entity> getTargets() {
        return Minecraft.getMinecraft().theWorld.loadedEntityList.stream()
                .filter(e -> isValid(e, maxRange.getValue()))
                .sorted(Comparator.comparingDouble(e -> mc.thePlayer.getClosestDistanceToEntity(e)))
                .collect(Collectors.toList());
    }



    public boolean isValid(Entity entity, double range) {
        double distance = mc.thePlayer.getClosestDistanceToEntity(entity);
        if (distance > range)
            return false;
        if (entity.isInvisible() && !invisibleValue.getValue())
            return false;
        if (mc.thePlayer.getClosestDistanceToEntity(entity) > range)
            return false;
        if (entity.isInvisible() && !invisibleValue.getValue())
            return false;
        if (!entity.isEntityAlive())
            return false;
        if (entity instanceof EntityPlayer p) {
            Module mcf = SilenceFix.instance.moduleManager.getModule("MCF");
            if (mcf != null && mcf.getState() && SilenceFix.instance.getFriendManager().isFriend(p.getName())) {
                return false;
            }

//            if (LiveComponent.isNotSelectable(p.liveUser)) {
//                return false;
//            }

            if (SilenceCrit.isTrustedPlayer(p)) {
                return false;
            }
        }
        if (entity == Minecraft.getMinecraft().thePlayer || entity.isDead || Minecraft.getMinecraft().thePlayer.getHealth() == 0F)
            return false;
        if ((entity instanceof EntityMob || entity instanceof EntityGhast || entity instanceof EntityGolem
                || entity instanceof EntityDragon || entity instanceof EntitySlime) && mobsValue.getValue())
            return true;
        if ((entity instanceof EntitySquid || entity instanceof EntityBat || entity instanceof EntityVillager) && animalsValue.getValue())
            return true;
        if (entity instanceof EntityAnimal && animalsValue.getValue())
            return true;
        if (AntiBot.isServerBot(entity)) {
            return false;
        }
        if (entity.getEntityId() == -8 || entity.getEntityId() == -1337) {
            return false;
        }
        if (Teams.isSameTeam(entity))
            return false;

        return entity instanceof EntityPlayer && playersValue.getValue();
    }






    public static boolean shouldAttack() {
        if (target == null) {
            return false;
        }

        currentRange = MathUtil.getRandomInRange(minRange.getValue(), maxRange.getValue());
        double actualDistance = mc.thePlayer.getClosestDistanceToEntity(target);

        System.out.printf("[KillAura] 距离检查: %.3f ≤ %.3f? %b%n",
                actualDistance,
                currentRange,
                actualDistance <= currentRange);

        if (actualDistance > currentRange) {
            System.out.println("[KillAura] 超出攻击距离，禁止攻击");
            return false;
        }

        boolean isMultipleTargets = KillAura.targets.size() > 1;

        if (rayCastValue.getValue()) {
            Vector2f rotation = RotationComponent.lastRotation != null
                    ? RotationComponent.lastRotation
                    : new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);

            MovingObjectPosition result = RayCastUtil.rayCast(
                    rotation,
                    currentRange,
                    0.0f,
                    mc.thePlayer,
                    true,
                    0F,
                    mc.timer.renderPartialTicks
            );

            if (result != null) {
                Entity hit = result.entityHit;

//                if (hit instanceof EntityPlayer p) {
////                    if (p.liveUser != null && LiveComponent.isSelectable(p.liveUser)) {
////                        System.out.println("[KillAura] 射线命中 IRC 队友，忽略，视为命中通过");
////                        return true;
////                    }
//                }

                if (hit == target) {
                    System.out.println("[KillAura] 射线检测: 命中目标");
                    return true;
                } else {
                    if (result.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
                        System.out.println("[KillAura] 射线被前方实体遮挡，忽略遮挡，允许攻击后方目标");
                        return true;
                    }
                    if (isMultipleTargets) {
                        System.out.println("[KillAura] 多目标模式下，忽略射线未命中");
                        return true;
                    }
                    System.out.println("[KillAura] 射线检测: 被方块遮挡，禁止攻击");
                    return false;
                }
            }

            System.out.println("[KillAura] 射线检测: 无命中结果");
            return false;
        }

        System.out.println("[KillAura] 射线检测关闭: 距离内允许攻击");
        return true;
    }




    private static float[] getRotationFloat(EntityLivingBase target, double xDiff, double yDiff) {
        double zDiff = target.posZ - KillAura.mc.thePlayer.posZ;
        double dist = MathHelper.sqrt_double(xDiff * xDiff + zDiff * zDiff);
        float yaw = (float) (Math.atan2(zDiff, xDiff) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float) (-Math.atan2(yDiff, dist) * 180.0 / Math.PI);
        float[] array = new float[2];
        int n = 0;
        float rotationYaw = lastRotation[0];
        array[n] = rotationYaw + MathHelper.wrapAngleTo180_float(yaw - lastRotation[0]);
        int n3 = 1;
        float rotationPitch = KillAura.mc.thePlayer.rotationPitch;
        array[n3] = rotationPitch + MathHelper.wrapAngleTo180_float(pitch - KillAura.mc.thePlayer.rotationPitch);
        return array;
    }

    public static float[] getRotation(EntityLivingBase target) {
        double xDiff = target.posX - KillAura.mc.thePlayer.posX;
        double yDiff = target.posY + (double) (target.getEyeHeight() / 5.0f * 3.0f) - (KillAura.mc.thePlayer.posY + (double) KillAura.mc.thePlayer.getEyeHeight());
        return KillAura.getRotationFloat(target, xDiff, yDiff);
    }

    public static EntityLivingBase getTarget() {
        return target;
    }

    static {
        targets = new ArrayList<>(0);
        Mode = new ModeValue<>("Aura Mode", AuraModes.values(), AuraModes.Switch);
        RotationMode = new ModeValue<>("Rotation Mode", RotationModes.values(), RotationModes.Silence);
        AttackingMode = new ModeValue<>("Attacking Mode", AttackingModes.values(), AttackingModes.Fix);
        cpsValue = new NumberValue("CPS", 20.0, 1.0, 20.0, 1.0);
        cpsTick = new NumberValue("HurtTime", 1.0, 0.0, 10.0, 1.0);

        strict = false;
    }

    public enum TargetGetModes {
        Silence,
        XinXin,
        RangeFix,
    }



    public enum AttackingModes {
        Fix,
        Post,
        All,
        Update

    }

    public enum RotationModes {
        XinXin,
        Normal,
        Silence
    }

    public enum AuraModes {
        Switch,
        HeShuYou,
        Multiple
    }

    public enum MarkMode {
        Normal,
        Modern
    }

}