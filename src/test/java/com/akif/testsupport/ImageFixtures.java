package com.akif.testsupport;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

public final class ImageFixtures {

    private static final byte[] JPEG = encode("jpg", 4, 4);
    private static final byte[] PNG = encode("png", 4, 4);
    private static final byte[] GIF = encode("gif", 4, 4);

    private ImageFixtures() {
    }

    public static byte[] jpeg() {
        return JPEG.clone();
    }

    public static byte[] png() {
        return PNG.clone();
    }

    public static byte[] gif() {
        return GIF.clone();
    }

    public static byte[] png(int width, int height) {
        return encode("png", width, height);
    }

    private static byte[] encode(String format, int width, int height) {
        int type = width * height > 1_000_000 ? BufferedImage.TYPE_BYTE_BINARY : BufferedImage.TYPE_INT_RGB;
        BufferedImage image = new BufferedImage(width, height, type);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, format, out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }
}
