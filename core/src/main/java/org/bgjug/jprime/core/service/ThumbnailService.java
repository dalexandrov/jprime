package org.bgjug.jprime.core.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import jakarta.enterprise.context.ApplicationScoped;
import org.imgscalr.Scalr;
import org.jboss.logging.Logger;

/** Resizes uploaded pictures and logos (same sizes as the legacy site). */
@ApplicationScoped
public class ThumbnailService {

    private static final Logger LOG = Logger.getLogger(ThumbnailService.class);

    public static final int SPEAKER_WIDTH = 280;
    public static final int SPEAKER_HEIGHT = 326;
    public static final int LOGO_WIDTH = 180;
    public static final int LOGO_HEIGHT = 64;

    public enum ResizeType {
        FIT_TO_HEIGHT, FIT_TO_RATIO
    }

    public byte[] speakerPicture(byte[] original) {
        return thumbImage(original, SPEAKER_WIDTH, SPEAKER_HEIGHT, ResizeType.FIT_TO_RATIO);
    }

    public byte[] logo(byte[] original) {
        return thumbImage(original, LOGO_WIDTH, LOGO_HEIGHT, ResizeType.FIT_TO_HEIGHT);
    }

    public byte[] thumbImage(byte[] original, int width, int height, ResizeType resizeType) {
        try {
            BufferedImage in = ImageIO.read(new ByteArrayInputStream(original));
            if (in == null) {
                LOG.warn("Uploaded file is not a supported image - stored as is");
                return original;
            }
            BufferedImage out = rescale(in, width, height, resizeType);
            if (out.getColorModel().hasAlpha()) {
                out = pad(out, out.getWidth(), out.getHeight());
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(out, "jpg", bytes);
            return bytes.toByteArray();
        } catch (IOException e) {
            LOG.error("An error occurred while generating image thumbnail", e);
            return original;
        }
    }

    BufferedImage rescale(BufferedImage image, int width, int height, ResizeType resizeType) {
        if (resizeType == ResizeType.FIT_TO_HEIGHT) {
            if (image.getHeight() < height) {
                return image;
            }
            return Scalr.resize(image, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.FIT_TO_HEIGHT, width, height);
        }
        BufferedImage rescaled = image;
        if (rescaled.getWidth() > width) {
            rescaled = Scalr.resize(rescaled, Scalr.Method.AUTOMATIC, Scalr.Mode.FIT_TO_WIDTH, width, height);
        }
        if (rescaled.getHeight() > height) {
            rescaled = Scalr.resize(rescaled, Scalr.Method.AUTOMATIC, Scalr.Mode.FIT_TO_HEIGHT, width, height);
        }
        return pad(rescaled, width, height);
    }

    /** Centers the image on a white canvas. */
    BufferedImage pad(BufferedImage image, int width, int height) {
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.drawImage(image, (width - image.getWidth()) / 2, (height - image.getHeight()) / 2, null);
        g.dispose();
        return canvas;
    }
}
