package dev.yalan.live.silencefix;

import net.minecraft.client.Minecraft;

public class LiveReconnectionThread extends Thread {

    @Override
    public void run() {
        final Minecraft mc = Minecraft.getMinecraft();

        while (mc.running) {
            if (!LiveClient.INSTANCE.isOpen()) {
                LiveClient.INSTANCE.connect();
            }

            try {
                Thread.sleep(10000L);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
