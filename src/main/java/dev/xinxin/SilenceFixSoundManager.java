package dev.xinxin;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SilenceFixSoundManager {
    private static final Logger logger = LogManager.getLogger("SilenceFixSoundManager");
    private final ExecutorService executor = Executors.newSingleThreadExecutor(
        new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("SilenceFixSoundPlayer")
            .build()
    );

    public void init() {
        SoundType.KILL.ordinal();
    }

    public void playSound(SoundType soundType, float volume) {
        final byte[] data = soundType.getData();

        if (data == null) {
            return;
        }

        executor.execute(() -> {
            try {
                final Clip clip = AudioSystem.getClip();
                final AudioInputStream ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(data));

                clip.open(ais);

                final FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(volume);

                clip.start();
            } catch (Exception e) {
                logger.error("Can't play audio " + soundType.getName(), e);
            }
        });
    }

    public enum SoundType {
        KILL("kill.wav"),
        ENABLE("enable.wav"),
        DISABLE("disable.wav");

        private final String soundName;
        private final byte[] data;

        SoundType(String soundName) {
            this.soundName = soundName;
            this.data = readData();
        }

        private byte[] readData() {
            final InputStream is = SilenceFixSoundManager.class.getResourceAsStream("/assets/minecraft/silencefix/sounds/" + this.soundName);

            if (is == null) {
                logger.error("Can't read sound data {} because the sound does not exists", this.soundName);
            } else {
                try {
                    return IOUtils.toByteArray(is);
                } catch (IOException e) {
                    logger.error("Can't read sound data " + this.soundName, e);
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }

            return null;
        }

        public byte[] getData() {
            return data;
        }

        public String getName() {
            return this.soundName;
        }
    }
}
