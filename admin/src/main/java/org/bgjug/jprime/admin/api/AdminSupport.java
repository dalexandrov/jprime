package org.bgjug.jprime.admin.api;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Optional;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.multipart.FileUpload;

final class AdminSupport {

    static final String ADMIN = "ADMIN";

    private AdminSupport() {
    }

    static <T> T found(Optional<T> value, String what) {
        return value.orElseThrow(() -> new NotFoundException(what + " not found"));
    }

    static byte[] bytes(FileUpload upload) {
        if (upload == null || upload.size() == 0 || upload.uploadedFile() == null) {
            return null;
        }
        try {
            return Files.readAllBytes(upload.uploadedFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Response image(byte[] data) {
        if (data == null || data.length == 0) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        String type = "image/jpeg";
        if (data.length > 3 && (data[0] & 0xFF) == 0x89 && data[1] == 'P') {
            type = "image/png";
        } else if (data.length > 2 && data[0] == 'G' && data[1] == 'I') {
            type = "image/gif";
        } else if (data[0] == '<') {
            type = "image/svg+xml";
        }
        return Response.ok(data).type(type).build();
    }

    static Response download(byte[] data, String fileName, String contentType) {
        return Response.ok(data)
            .type(contentType)
            .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
            .build();
    }
}
