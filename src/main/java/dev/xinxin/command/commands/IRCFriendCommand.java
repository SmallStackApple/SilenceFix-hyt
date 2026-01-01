package dev.xinxin.command.commands;

import dev.xinxin.SilenceFix;
import dev.xinxin.command.Command;
import dev.xinxin.utils.client.HelperUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class IRCFriendCommand extends Command {
    public IRCFriendCommand() {
        super("ircFriend");
    }

    @Override
    public List<String> autoComplete(int var1, String[] var2) {
        return new ArrayList<>();
    }


    @Override
    public void run(String[] args) {
        if (args.length < 2) {
            HelperUtil.sendMessage(".ircFriend <add or rmv> <name>");
            return;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "add": {
                SilenceFix.instance.ircFriends.add(args[1]);
                HelperUtil.sendMessage("成功添加为IRC好友: " + args[1]);
                break;
            }
            case "rmv": {
                SilenceFix.instance.ircFriends.remove(args[1]);
                HelperUtil.sendMessage("成功删除掉IRC好友: " + args[1]);
                break;
            }
            default: {
                HelperUtil.sendMessage(".ircFriend <add or rmv> <name>");
                break;
            }
        }
    }
}
