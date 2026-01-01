package dev.xinxin.command.commands;

import dev.xinxin.command.Command;
import dev.xinxin.utils.client.HelperUtil;
import dev.yalan.live.silencefix.LiveClient;
import dev.yalan.live.silencefix.LiveUser;
import dev.yalan.live.silencefix.netty.LiveProto;

import java.util.ArrayList;
import java.util.List;

public class IRCKickMCCommand extends Command {
    public IRCKickMCCommand() {
        super("ikick");
    }

    @Override
    public List<String> autoComplete(int var1, String[] var2) {
        return new ArrayList<>();
    }

    @Override
    public void run(String[] args) {
        if (args.length < 1) {
            HelperUtil.sendMessage(".ikick <target> <reason>");
            return;
        }

//        if (LiveClient.INSTANCE.liveUser.getLevel().isLower(LiveUser.Level.PAID)) {
//            HelperUtil.sendMessage("只有内部和管理员可使用此指令");
//            return;
//        }

        final StringBuilder reasonBuilder = new StringBuilder();
        if (args.length > 1) {
            for (int i = 1; i < args.length; i++) {
                reasonBuilder.append(args[i]);

                if (i != args.length - 1) {
                    reasonBuilder.append(' ');
                }
            }
        }

        String reason = reasonBuilder.toString();
        if (reason.isEmpty()) {
            reason = "无";
        }

        LiveClient.INSTANCE.sendPacket(LiveProto.createKickPlayer(args[0], reason));
    }
}
