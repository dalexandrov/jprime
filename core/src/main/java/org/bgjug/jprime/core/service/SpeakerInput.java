package org.bgjug.jprime.core.service;

import org.apache.commons.lang3.StringUtils;

/** Speaker data as entered in the CFP form (or the admin submission form). */
public record SpeakerInput(String firstName, String lastName, String email, String headline, String twitter,
                           String bsky, String bio, byte[] picture) {

    public boolean isEmpty() {
        return StringUtils.isAllBlank(firstName, lastName, email, bio);
    }

    public boolean hasPicture() {
        return picture != null && picture.length > 0;
    }
}
