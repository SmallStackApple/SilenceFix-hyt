package dev.xinxin.utils.render;

import dev.xinxin.utils.TimerUtil;
import lombok.Getter;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.nio.ByteBuffer;

@Getter
public class WallpaperEngine implements AutoCloseable {
    private static final int MILLIS_PER_SECOND = 1000;
    private static final int GL_RGBA = 32992; // OpenGL RGBA format constant
    private final TimerUtil timer = new TimerUtil();
    private int framerate;
    private int grabbedFrames;
    private int lastFrameTexID;
    private FFmpegFrameGrabber grabber;
    private Frame currentFrame;
    private boolean initialized;
    public void setup(File videoFile, int framerate) {
        validateInput(videoFile, framerate);
        this.framerate = framerate;
        initGrabber(videoFile);
    }

    private void validateInput(File videoFile, int framerate) {
        if (framerate <= 0) throw new IllegalArgumentException("Framerate must be positive");
        if (videoFile == null || !videoFile.exists()) {
            throw new IllegalArgumentException("Video file does not exist");
        }
    }

    private void initGrabber(File videoFile) {
        try {
            avutil.av_log_set_level(avutil.AV_LOG_ERROR);
            this.grabber = FFmpegFrameGrabber.createDefault(videoFile);
            grabber.start();
            initialized = true;
        } catch (Exception e) {
            cleanupResources();
            throw new RuntimeException("Failed to initialize video grabber", e);
        }
    }

    public void render(int width, int height) {
        if (!isReady()) return;
        if (lastFrameTexID == 0) {
            updateFrame(); // 强制重新加载一帧来触发纹理创建
        }

        if (shouldUpdateFrame()) {
            updateFrame();
        }

        renderCurrentFrame(width, height);
    }


    private boolean isReady() {
        return initialized && grabber != null;
    }

    private boolean shouldUpdateFrame() {
        return timer.hasTimeElapsed(MILLIS_PER_SECOND / framerate, true);
    }

    private void renderCurrentFrame(int width, int height) {
        if (currentFrame != null && lastFrameTexID != 0) {
            GlStateManager.bindTexture(lastFrameTexID);
            Gui.drawModalRectWithCustomSizedTexture(0, 0, 0, 0, width, height, width, height);
        }
    }
    private void updateFrame() {
        try {
            handleFrameLooping();
            currentFrame = grabber.grabImage();
            if (currentFrame != null) {
                updateTexture();
                grabbedFrames++;
            }
        } catch (Exception e) {
            handleFrameGrabError(e);
        }
    }

    private void handleFrameLooping() throws FrameGrabber.Exception {
        if (grabbedFrames >= grabber.getLengthInFrames()) {
            grabber.setFrameNumber(0);
            grabbedFrames = 0;
        }
    }

    private void handleFrameGrabError(Exception e) {
        System.err.println("Error grabbing frame: " + e.getMessage());
        cleanupResources();
    }
    private void updateTexture() {
        try {
            ensureTextureInitialized();
            uploadFrameToTexture();
        } catch (Exception e) {
            handleTextureError(e);
        }
    }

    private void ensureTextureInitialized() {
        if (lastFrameTexID == 0) {
            lastFrameTexID = GL11.glGenTextures();
            if (lastFrameTexID == 0) {
                throw new RuntimeException("Failed to generate OpenGL texture");
            }
            configureTextureParameters();
        }
    }

    private void configureTextureParameters() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, lastFrameTexID);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
    }

    private void uploadFrameToTexture() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, lastFrameTexID);
        GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL11.GL_RGBA,
                currentFrame.imageWidth,
                currentFrame.imageHeight,
                0,
                GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                (ByteBuffer) currentFrame.image[0]
        );
    }

    private void handleTextureError(Exception e) {
        System.err.println("Error updating texture: " + e.getMessage());
        releaseTexture();
    }
    @Override
    public void close() {
        cleanupResources();
    }

    private void cleanupResources() {
        releaseGrabber();
        releaseTexture();
        resetState();
    }

    private void releaseGrabber() {
        if (grabber != null) {
            try {
                grabber.stop();
                grabber.close();
            } catch (Exception e) {
                System.err.println("Error closing grabber: " + e.getMessage());
            } finally {
                grabber = null;
            }
        }
    }

    private void releaseTexture() {
        if (lastFrameTexID != 0) {
            GL11.glDeleteTextures(lastFrameTexID);
            lastFrameTexID = 0;
        }
    }

    private void resetState() {
        currentFrame = null;
        initialized = false;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            cleanupResources();
        } finally {
            super.finalize();
        }
    }
}