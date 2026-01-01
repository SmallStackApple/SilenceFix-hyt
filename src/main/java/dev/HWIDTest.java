package dev;

import oshi.SystemInfo;
import oshi.hardware.Processor;

public class HWIDTest {
    public static void main(String[] args) {
        System.out.println(generateHardwareId());
    }

    private static String generateHardwareId() {
        final SystemInfo systemInfo = new SystemInfo();
        final Processor[] processors = systemInfo.getHardware().getProcessors();

        return "x" + processors.length + "(" + processors[0].getName() + ":" + processors[0].getIdentifier() + ")";
    }
}
