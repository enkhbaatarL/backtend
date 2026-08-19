package com.sk.domain.admin.validator;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class NoticeImageUploadValidator {

    static final long MAX_FILE_SIZE_BYTES = 20L * 1024 * 1024;

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };
    private static final byte[] JPEG_SIGNATURE = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF
    };
    private static final Map<String, ValidatedNoticeImageUpload> ALLOWED_TYPES = Map.of(
            "png", new ValidatedNoticeImageUpload("png", "image/png"),
            "jpg", new ValidatedNoticeImageUpload("jpg", "image/jpeg"));

    public ValidatedNoticeImageUpload validate(MultipartFile file) {
        if (file == null) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER);
        }

        if (file.isEmpty() || file.getSize() <= 0 || file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw AppErrorException.of(ResultCode.Error.FILE_SIZE_ERROR);
        }

        String extension = extractExtension(file.getOriginalFilename());
        ValidatedNoticeImageUpload validatedType = ALLOWED_TYPES.get(extension);
        if (validatedType == null) {
            throw AppErrorException.of(ResultCode.Error.FILE_FORMAT_ERROR);
        }

        String normalizedContentType = normalizeContentType(file.getContentType());
        if (!validatedType.contentType().equals(normalizedContentType)) {
            throw AppErrorException.of(ResultCode.Error.FILE_FORMAT_ERROR);
        }

        byte[] signature = readSignature(file);
        if (!matchesSignature(validatedType, signature)) {
            throw AppErrorException.of(ResultCode.Error.FILE_FORMAT_ERROR);
        }

        return validatedType;
    }

    private String extractExtension(String fileName) {
        if (fileName == null) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID);
        }

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID);
        }

        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
    }

    private byte[] readSignature(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return inputStream.readNBytes(PNG_SIGNATURE.length);
        } catch (IOException exception) {
            throw AppErrorException.of(ResultCode.Error.FILE_INVALID);
        }
    }

    private boolean matchesSignature(ValidatedNoticeImageUpload validatedType, byte[] signature) {
        if ("png".equals(validatedType.extension())) {
            return Arrays.equals(PNG_SIGNATURE, signature);
        }

        return signature.length >= JPEG_SIGNATURE.length
                && signature[0] == JPEG_SIGNATURE[0]
                && signature[1] == JPEG_SIGNATURE[1]
                && signature[2] == JPEG_SIGNATURE[2];
    }
}
