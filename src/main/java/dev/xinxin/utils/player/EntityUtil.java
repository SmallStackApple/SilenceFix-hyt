package dev.xinxin.utils.player;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.WorldSettings;

import java.util.UUID;

import static dev.xinxin.utils.player.RotationUtil.mc;

public class EntityUtil {
   public static EntityPlayer getCopiedPlayer(EntityPlayer player) {
      final int count = player.getItemInUseCount();
      EntityPlayer copied = new EntityPlayer(mc.theWorld, new GameProfile(UUID.randomUUID(), player.getName())) {
         public boolean isSpectator() {
            return false;
         }

         @Override
         public WorldSettings.GameType getGameType() {
            return WorldSettings.GameType.NOT_SET;
         }

         public boolean isCreative() {
            return false;
         }

         public int getItemInUseCount() {
            return count;
         }
      };
      copied.setSneaking(player.isSneaking());
      copied.swingProgress = player.swingProgress;
      copied.limbSwing = player.limbSwing;
      copied.limbSwingAmount = player.prevLimbSwingAmount;
      copied.inventory.copyInventory(player.inventory);
      copied.ticksExisted = player.ticksExisted;
      copied.setEntityId(player.getEntityId());
      copied.copyLocationAndAnglesFrom(player);
      return copied;
   }
}
