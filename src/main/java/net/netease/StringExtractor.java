package net.netease;

public class StringExtractor {

    public static String extractBetween(String source, String start, String end) {
        if (source == null || start == null || end == null) {
            return null;
        }

        int startIndex = source.indexOf(start);
        if (startIndex == -1) {
            return null; // start not found
        }
        startIndex += start.length();

        int endIndex = source.indexOf(end, startIndex);
        if (endIndex == -1) {
            return null; // end not found
        }

        return source.substring(startIndex, endIndex);
    }
}
