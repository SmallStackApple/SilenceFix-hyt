package dev.xinxin.utils;

public class TimerUtil {
   private boolean run = true;
   public long lastMS = System.currentTimeMillis();
   private long time = System.currentTimeMillis();
   public long passed() {
      return this.getCurrentMS() - this.lastMS;
   }
   public long getCurrentMS() {
      return System.nanoTime() / 1000000L;
   }

   public TimerUtil(boolean run) {
      this.run = run;
   }

   public void start() {
      this.run = true;
   }

   public void stop() {
      this.run = false;
   }

   public long getTimePassed() {
      return System.currentTimeMillis() - this.lastMS;
   }
   public long getTimeElapsed() {
      return System.currentTimeMillis() - this.lastMS;
   }
   public boolean hasTimePassed(long time) {
      return System.currentTimeMillis() - this.lastMS >= time;
   }
   public void reset() {
      this.lastMS = System.currentTimeMillis();
      this.time = System.currentTimeMillis();
   }

   public long getElapsedTime() {
      return this.run ? System.currentTimeMillis() - this.time : 0L;
   }

   public boolean hasTimeElapsed(long milliseconds) {
      return this.run && this.getElapsedTime() >= milliseconds;
   }

   public boolean hasTimeElapsed(long time, boolean reset) {
      if (System.currentTimeMillis() - lastMS > time) {
         if (reset) reset();
         return true;
      }

      return false;
   }

   public boolean delay(float time) {
      return System.currentTimeMillis() - this.lastMS >= time;

   }

   public static long getCurrentTime() {
      return System.currentTimeMillis();
   }

   public boolean isOver(long milliseconds) {
      return System.currentTimeMillis() - this.time > milliseconds;
   }

   public long remainingTime(long milliseconds) {
      long elapsedTime = System.currentTimeMillis() - this.time;
      return elapsedTime < milliseconds ? milliseconds - elapsedTime : 0L;
   }

   public boolean isRun() {
      return this.run;
   }

   public long getTime() {
      return this.time;
   }

   public void setRun(boolean run) {
      this.run = run;
   }

   public void setTime(long time) {
      this.time = time;
   }

   public TimerUtil() {
   }
}
