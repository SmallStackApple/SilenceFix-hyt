package dev.xinxin.module.modules.player;

import dev.xinxin.SilenceFix;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventTick;
import dev.xinxin.gui.notification.NotificationManager;
import dev.xinxin.gui.notification.NotificationType;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.utils.TimerUtil;
import dev.xinxin.utils.wings.FriendManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StringUtils;
import org.lwjgl.input.Mouse;

public class MCF extends Module {
    private TimerUtil timer = new TimerUtil();

    public MCF() {
        super("MCF", Category.Player, "中键添加好友");
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.inGameHasFocus && Mouse.isButtonDown(2)) { // 检测中键
            if (timer.delay(200)) { // 200ms防连点
                if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer) mc.objectMouseOver.entityHit;
                    String name = StringUtils.stripControlCodes(player.getName());
                    FriendManager friendManager = SilenceFix.instance.getFriendManager();

                    if (friendManager.isFriend(name)) {
                        friendManager.remove(name);
                        mc.thePlayer.addChatMessage(new ChatComponentText("§f删除好友，杀戮可以攻击 : §a" + name));
                        mc.thePlayer.addChatMessage(new ChatComponentText("§f删除好友，鸡蛋雪球可砸 : §a" + name));
                    } else {
                        friendManager.add(name);
                        mc.thePlayer.addChatMessage(new ChatComponentText("§e添加好友，杀戮无法攻击 : §a" + name));
                        mc.thePlayer.addChatMessage(new ChatComponentText("§e添加好友，雪球鸡蛋不砸 : §a" + name));
                    }
                } else {
                    mc.thePlayer.addChatMessage(new ChatComponentText("§c没有对准玩家，添加失败"));
                    NotificationManager.post(NotificationType.WARNING, "MCF", "添加失败 "+ "原因：没对准玩家W " , 5);

                }
                timer.reset(); // 重置计时器
            }
        }
    }
}