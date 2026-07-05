package com.society.app.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Cloudflare R2-backed file storage (S3-compatible API).
 *
 * <p>Two upload paths:
 * <ul>
 *   <li>{@link #store(MultipartFile, String)} — validates MIME/size then streams to R2.</li>
 *   <li>{@link #store(byte[], String, String, String)} — uploads raw bytes (server-generated PDFs).</li>
 * </ul>
 * Both return the object key ({@code subfolder/filename}) for DB persistence.
 * Use {@link #getPublicUrl(String)} to build the full URL at read time.
 * </p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FileStorageService {

    private static final List<String> ALLOWED_MIME = List.of(
            "image/jpeg", "image/png", "application/pdf");
    private static final long MAX_SIZE_BYTES = 5L * 1024L * 1024L;

    private final S3Client s3;

    @Value("${app.r2.bucket}")
    private String bucket;

    @Value("${app.r2.public-url}")
    private String publicUrl;

    /**
     * Validates and uploads a user-uploaded file to R2.
     *
     * @throws IllegalArgumentException when MIME / size is invalid
     */
    public String store(MultipartFile file, String subfolder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        String mime = file.getContentType();
        if (mime == null || !ALLOWED_MIME.contains(mime.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file type: only JPEG, PNG, PDF allowed");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("File exceeds 5 MB limit");
        }

        String ext = getExtension(file.getOriginalFilename(), mime);
        String filename = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
        String key = subfolder + "/" + filename;

        try {
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(mime)
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to R2: " + e.getMessage(), e);
        }
        log.info("Uploaded {} → R2 key={}", file.getOriginalFilename(), key);
        return key;
    }

    /**
     * Uploads raw bytes to R2 — used for server-generated PDFs.
     */
    public String store(byte[] bytes, String contentType, String subfolder, String filename) {
        String key = subfolder + "/" + filename;
        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(bytes));
        log.info("Uploaded {} bytes → R2 key={}", bytes.length, key);
        return key;
    }

    /**
     * Builds the public URL for a stored object key.
     * Requires public access enabled on the R2 bucket in Cloudflare dashboard.
     */
    public String getPublicUrl(String key) {
        if (key == null || key.isBlank()) return null;
        String base = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
        return base + "/" + key;
    }

    private static String getExtension(String original, String mime) {
        if (original != null && original.contains(".")) {
            String ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase();
            if (ext.matches("[a-z0-9]{1,6}")) return ext;
        }
        return switch (mime.toLowerCase()) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "application/pdf" -> "pdf";
            default -> "";
        };
    }
}
