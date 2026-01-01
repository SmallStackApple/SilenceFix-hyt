package dev.xinxin.command.commands;

import dev.xinxin.command.Command;
import dev.xinxin.utils.client.HelperUtil;
import dev.yalan.live.silencefix.IRCModule;
import dev.yalan.live.silencefix.LiveClient;
import dev.yalan.live.silencefix.netty.LiveProto;

import java.util.ArrayList;
import java.util.List;

public class ChatCommand extends Command {
    public ChatCommand() {
        super("i");
    }

    @Override
    public List<String> autoComplete(int var1, String[] var2) {
        return new ArrayList<>();
    }

    @Override
    public void run(String[] args) {
        if (!IRCModule.Instance.state) {
            HelperUtil.sendMessage("需要打开IRC才可以进行IRC聊天！");
            return;
        }

        final StringBuilder sb = new StringBuilder();
        final int len = args.length;

        for (int i = 0; i < len; i++) {
            sb.append(args[i]);

            if (i != len - 1) {
                sb.append(' ');
            }
        }

        LiveClient.INSTANCE.sendPacket(LiveProto.createChat(sb.toString()));
    }
}
