package net.minecraft.util;

import lombok.Getter;
import net.minecraft.client.Minecraft;

@Getter
public class Timer
{
    public float ticksPerSecond;
    private double lastHRTime;
    public int elapsedTicks;
    public float renderPartialTicks;
    public float timerSpeed = 1.0F;
    public float elapsedPartialTicks;
    public long lastSyncSysClock;
    private long lastSyncHRClock;

    private long counter;
    private double timeSyncAdjustment = 1.0D;

    public float tickLength;

    public Timer(float tps)
    {
        this.tickLength = 1000.0F / tps;
        this.ticksPerSecond = tps;
        this.lastSyncSysClock = Minecraft.getSystemTime();
        this.lastSyncHRClock = System.nanoTime() / 1000000L;
    }
    public void updateTimer(long i)
    {
        this.elapsedPartialTicks = (float)(i - this.lastSyncSysClock) / this.tickLength;
        this.lastSyncSysClock = i;
        this.renderPartialTicks += this.elapsedPartialTicks;
        this.elapsedTicks = (int)this.renderPartialTicks;
        this.renderPartialTicks -= (float)this.elapsedTicks;
    }
}
