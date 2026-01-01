package dev.xinxin.utils;

import org.apache.commons.lang3.RandomUtils;

public class TimerUtils {
    private long lastMS = System.currentTimeMillis(); // 非静态，使用 currentTimeMillis

    public long getDifference() {
        return getCurrentMS() - lastMS;
    }

    public long getCurrentMS() {
        return System.currentTimeMillis(); // 统一时间源
    }

    public void reset() {
        this.lastMS = getCurrentMS();
    }

    public boolean hasReached(double milliseconds) {
        return getDifference() >= milliseconds;
    }

    public static int randomDelay(int minDelay, int maxDelay) {
        return RandomUtils.nextInt(minDelay, maxDelay);
    }

    public boolean hasTimeElapsed(long time, boolean reset) {
        boolean elapsed = getDifference() >= time;
        if (elapsed && reset) {
            reset();
        }
        return elapsed;
    }

    public boolean hasTimeElapsed(long time) {
        return getDifference() >= time;
    }

    public boolean delay(float time) {
        return (getCurrentMS() - lastMS) >= time;
    }

    public long getTime() {
        return getDifference();
    }

    public void setTime(long time) {
        this.lastMS = time;
    }
}