package dev.xinxin.module.modules.misc;

import dev.xinxin.SilenceFix;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.values.BoolValue;
import dev.xinxin.utils.player.PlayerUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import java.util.Objects;

public class Teams
extends Module {
    private static final BoolValue armorValue = new BoolValue("ArmorColor", true);
    private static final BoolValue colorValue = new BoolValue("Color", true);
    private static final BoolValue scoreboardValue = new BoolValue("ScoreboardTeam", true);

    public Teams() {
        super("Teams", Category.Misc,"队伍");
    }

    public static boolean isSameTeam(Entity entity) {
        if (entity instanceof EntityPlayer) {
            EntityPlayer entityPlayer = (EntityPlayer)entity;
            if (Objects.requireNonNull(SilenceFix.instance.moduleManager.getModule("Teams")).getState()) {
                return armorValue.getValue() != false && PlayerUtil.armorTeam(entityPlayer) || (Boolean)colorValue.getValue() != false && PlayerUtil.colorTeam(entityPlayer) || (Boolean)scoreboardValue.getValue() != false && PlayerUtil.scoreTeam(entityPlayer);
            }
            return false;
        }
        return false;
    }
}

