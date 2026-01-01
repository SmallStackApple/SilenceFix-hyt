package com.xinxin.client.utils.misc.fixer;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.xinxin.client.viaversion.vialoadingbase.ViaLoadingBase;
import lombok.experimental.UtilityClass;
import net.minecraft.entity.player.EntityPlayer;

@UtilityClass
public class MovementFixer {
    private static final Double MINIMUM_MOTION_POST_1_8 = 0.003D;
    private static final Double MINIMUM_MOTION_PRE_1_8 = 0.005D;
    private static final Float EXHAUSTION_SPRINTING_POST_1_8 = 0.2F;
    private static final Float EXHAUSTION_SPRINTING_PRE_1_8 = 0.8F;
    private static final Float EXHAUSTION_WALKING_POST_1_8 = 0.05F;
    private static final Float EXHAUSTION_WALKING_PRE_1_8 = 0.2F;

    private boolean isVersionConditionMet_Exhaustion() {
        return ViaLoadingBase.getInstance().getTargetVersion().isNewerThan(ProtocolVersion.v1_8);
    }

    private boolean isVersionConditionMet_MinimumMotion() {
        return ViaLoadingBase.getInstance().getTargetVersion().isNewerThan(ProtocolVersion.v1_8);
    }

    public static Double fixMinimumMotion() {
        return isVersionConditionMet_MinimumMotion() ? MINIMUM_MOTION_POST_1_8 : MINIMUM_MOTION_PRE_1_8;
    }

    public void fixJumpExhaustion(EntityPlayer player) {
        float exhaustionSprinting = isVersionConditionMet_Exhaustion() ? EXHAUSTION_SPRINTING_POST_1_8 : EXHAUSTION_SPRINTING_PRE_1_8;
        float exhaustionWalking = isVersionConditionMet_Exhaustion() ? EXHAUSTION_WALKING_POST_1_8 : EXHAUSTION_WALKING_PRE_1_8;

        float exhaustion = player.isSprinting() ? exhaustionSprinting : exhaustionWalking;
        player.addExhaustion(exhaustion);
    }

    public float fixInWaterExhaustion() {
        return isVersionConditionMet_Exhaustion() ? 0.01F : 0.015F;
    }

    public float fixSprintExhaustion() {
        return isVersionConditionMet_Exhaustion() ? 0.1F : 0.099999994F;
    }

    public void fixOtherMotionExhaustion(EntityPlayer player, float f) {
        if (isVersionConditionMet_Exhaustion()) {
            player.addExhaustion(0.0F * f * 0.01F);
        }
    }
}
