package net.netease;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

public class GZipUtils {

    public static byte[] gzipEncode(String data) {
        if (data == null || data.isEmpty()) {
            return new byte[0];
        }

        byte[] inputBytes = data.getBytes(StandardCharsets.UTF_8);

        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {

            gzipStream.write(inputBytes);
            gzipStream.finish(); // important to ensure all data is written

            return byteStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to compress string", e);
        }
    }
}