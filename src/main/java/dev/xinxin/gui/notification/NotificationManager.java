
package dev.xinxin.gui.notification;

import dev.xinxin.module.modules.render.HUD;

import java.util.concurrent.CopyOnWriteArrayList;


public class NotificationManager
{
    private static float toggleTime;
    private static final CopyOnWriteArrayList<Notification> notifications;

    public static void post(final NotificationType type, final String title, final String description) {
        post(new Notification(type, title, description));
    }

    public static void post(final NotificationType type, final String title, final String description, final float time) {
        post(new Notification(type, title, description, time));
    }

    public static void post(final Notification notification) {
        if (HUD.notifications.getValue()) {
            NotificationManager.notifications.add(notification);
        }
    }

    public static float getToggleTime() {
        return NotificationManager.toggleTime;
    }

    public static void setToggleTime(final float toggleTime) {
        NotificationManager.toggleTime = toggleTime;
    }

    public static CopyOnWriteArrayList<Notification> getNotifications() {
        return NotificationManager.notifications;
    }

    static {
        NotificationManager.toggleTime = 2.0f;
        notifications = new CopyOnWriteArrayList<Notification>();
    }
}
