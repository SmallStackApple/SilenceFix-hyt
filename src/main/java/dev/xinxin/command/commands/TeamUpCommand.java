package dev.xinxin.command.commands;

import dev.xinxin.SilenceFix;
import dev.xinxin.command.Command;
import dev.xinxin.utils.client.HelperUtil;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
public class TeamUpCommand extends Command {
    public TeamUpCommand() {
        super("teamup");
    }

    @Override
    public List<String> autoComplete(int var1, String[] var2) {
        return new ArrayList<>();
    }

    @Override
    public void run(String[] args) {
        if (args.length < 2) {
            HelperUtil.sendMessage(".teamup <ircName> <mcName>");
            return;  // 如果参数不足，提前返回
        }

        final String ircName = args[0];
        final String mcName = args[1];
        Minecraft.getMinecraft().thePlayer.sendChatMessage("/hub");

        SilenceFix.executor.schedule(() -> {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                if (Minecraft.getMinecraft().thePlayer != null) {
                    Minecraft.getMinecraft().thePlayer.sendChatMessage("/组队 " + mcName);
                }
            });
        }, 1, TimeUnit.SECONDS);

        SilenceFix.executor.schedule(() -> {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                if (Minecraft.getMinecraft().thePlayer != null) {
                    Minecraft.getMinecraft().thePlayer.sendChatMessage(".i " + ircName + " 我已向你发送组队申请,请你按H之后点击我的IRC");
                }
            });
        }, 3, TimeUnit.SECONDS);

        SilenceFix.executor.schedule(() -> {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                if (Minecraft.getMinecraft().thePlayer != null) {
                    Minecraft.getMinecraft().thePlayer.sendChatMessage(".i 如果想退出队伍或者解散请你输入/组队 解散");
                }
            });
        }, 5, TimeUnit.SECONDS);
    }
}
