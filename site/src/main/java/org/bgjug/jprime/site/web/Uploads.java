package org.bgjug.jprime.site.web;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

import org.jboss.resteasy.reactive.multipart.FileUpload;

final class Uploads {

    private Uploads() {
    }

    /** Bytes of the uploaded file, or null when nothing was uploaded. */
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
}
