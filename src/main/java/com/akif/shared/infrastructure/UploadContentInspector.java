package com.akif.shared.infrastructure;

import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class UploadContentInspector {

    public record InspectedUpload(String contentType, String extension) {
    }

    static final long MAX_PIXELS = 50_000_000L;
    private static final long DECODE_PIXEL_BUDGET = 4_000_000L;
    private static final Set<String> HEIF_BRANDS = Set.of("heic", "heix", "hevc", "hevx", "heim", "heis", "mif1", "msf1");
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};

    private UploadContentInspector() {
    }

    public static boolean matchesAllowedTypes(MultipartFile file, List<String> allowedTypes) {
        String declared = file == null ? null : file.getContentType();
        if (declared == null || !allowedTypes.contains(declared)) {
            return false;
        }
        return inspect(file)
                .filter(inspected -> inspected.contentType().equals(declared)
                        || ("image/jpg".equals(declared) && "image/jpeg".equals(inspected.contentType())))
                .isPresent();
    }

    public static Optional<InspectedUpload> inspect(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Optional.empty();
        }
        try {
            return inspect(file.getBytes());
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    static Optional<InspectedUpload> inspect(byte[] content) {
        if (isJpeg(content)) {
            return decodable(content) ? Optional.of(new InspectedUpload("image/jpeg", ".jpg")) : Optional.empty();
        }
        if (startsWith(content, PNG_SIGNATURE)) {
            return decodable(content) ? Optional.of(new InspectedUpload("image/png", ".png")) : Optional.empty();
        }
        if (isGif(content)) {
            return decodable(content) ? Optional.of(new InspectedUpload("image/gif", ".gif")) : Optional.empty();
        }
        if (isWebp(content)) {
            return Optional.of(new InspectedUpload("image/webp", ".webp"));
        }
        if (isHeif(content)) {
            return Optional.of(new InspectedUpload("image/heic", ".heic"));
        }
        return Optional.empty();
    }

    private static boolean isJpeg(byte[] content) {
        return content.length > 3 && (content[0] & 0xff) == 0xff && (content[1] & 0xff) == 0xd8
                && (content[2] & 0xff) == 0xff;
    }

    private static boolean isGif(byte[] content) {
        return startsWith(content, "GIF87a".getBytes(StandardCharsets.US_ASCII))
                || startsWith(content, "GIF89a".getBytes(StandardCharsets.US_ASCII));
    }

    private static boolean isWebp(byte[] content) {
        return content.length > 12
                && new String(content, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                && new String(content, 8, 4, StandardCharsets.US_ASCII).equals("WEBP");
    }

    private static boolean isHeif(byte[] content) {
        return content.length > 12
                && new String(content, 4, 4, StandardCharsets.US_ASCII).equals("ftyp")
                && HEIF_BRANDS.contains(new String(content, 8, 4, StandardCharsets.US_ASCII));
    }

    private static boolean startsWith(byte[] content, byte[] prefix) {
        if (content.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (content[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean decodable(byte[] content) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            if (input == null) {
                return false;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                return false;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                long width = reader.getWidth(0);
                long height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width * height > MAX_PIXELS) {
                    return false;
                }
                ImageReadParam param = reader.getDefaultReadParam();
                int step = (int) Math.max(1, Math.ceil(Math.sqrt((double) (width * height) / DECODE_PIXEL_BUDGET)));
                param.setSourceSubsampling(step, step, 0, 0);
                return reader.read(0, param) != null;
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }
}
