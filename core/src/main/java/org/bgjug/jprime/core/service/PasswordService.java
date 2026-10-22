package org.bgjug.jprime.core.service;

import java.nio.charset.StandardCharsets;

import at.favre.lib.crypto.bcrypt.BCrypt;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.lang3.StringUtils;

/** BCrypt hashing, compatible with the hashes produced by the legacy Spring Security encoder. */
@ApplicationScoped
public class PasswordService {

    private static final int COST = 10;

    public String hash(String rawPassword) {
        return BCrypt.withDefaults().hashToString(COST, rawPassword.toCharArray());
    }

    public boolean matches(String rawPassword, String hash) {
        if (StringUtils.isEmpty(rawPassword) || StringUtils.isEmpty(hash)) {
            return false;
        }
        return BCrypt.verifyer().verify(rawPassword.getBytes(StandardCharsets.UTF_8),
            hash.getBytes(StandardCharsets.UTF_8)).verified;
    }
}
