package dev.xinxin.utils.wings;


import dev.xinxin.SilenceFix;
import dev.xinxin.gui.notification.NotificationManager;
import dev.xinxin.gui.notification.NotificationType;
import lombok.Getter;

import java.util.ArrayList;

@Getter
public class FriendManager {

    private final ArrayList<String> friends;

    public FriendManager() {
        friends = new ArrayList<>();
    }

    public void add(String name) {
        if (!friends.contains(name)) {
            friends.add(name);
            NotificationManager.post(NotificationType.SUCCESS, "Friend Manager", "杀戮雪球鸡蛋不能攻击这个何树友了: " + name);
            SilenceFix.instance.getConfigManager().saveConfig("friends.json");
        } else {
            NotificationManager.post(NotificationType.DISABLE, "Friend Manager", name + "他已经是何树友了!");
        }
    }

    public void remove(String name) {
        if (friends.contains(name)) {
            friends.remove(name);
            NotificationManager.post(NotificationType.WARNING, "Friend Manager", "杀戮雪球鸡蛋可以攻击这个何树友了: " + name);
            SilenceFix.instance.getConfigManager().saveConfig("friends.json");

        } else {
            NotificationManager.post(NotificationType.DISABLE, "Friend Manager", "他不在是何树友了！ " + name);

        }
    }

    public boolean isFriend(String name) {
        return friends.contains(name);
    }

}
