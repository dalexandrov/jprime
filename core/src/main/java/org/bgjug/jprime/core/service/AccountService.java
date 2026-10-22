package org.bgjug.jprime.core.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.bgjug.jprime.core.model.JPrimeException;
import org.bgjug.jprime.core.model.ResetPasswordToken;
import org.bgjug.jprime.core.model.User;
import org.bgjug.jprime.core.repository.ResetPasswordTokenRepository;
import org.bgjug.jprime.core.repository.UserRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Public user accounts: sign-up, login check and password reset.
 */
@ApplicationScoped
public class AccountService {

    private static final Logger LOG = Logger.getLogger(AccountService.class);
    private static final char[] TOKEN_CHARS = "abbbcdefghhiiijklmmnopqrstuuvwxyzz1234567899990".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    @Inject
    UserRepository userRepository;

    @Inject
    ResetPasswordTokenRepository tokenRepository;

    @Inject
    PasswordService passwordService;

    @Inject
    MailService mailService;

    @Inject
    MailTemplates mailTemplates;

    @ConfigProperty(name = "jprime.reset-password.url", defaultValue = "https://jprime.io/account/new-password?tokenId=")
    String resetPasswordUrl;

    @ConfigProperty(name = "jprime.reset-password.token-duration-hours", defaultValue = "2")
    int tokenDurationHours;

    /** User (or speaker) with a password matching the given one. */
    public Optional<User> authenticate(String email, String password) {
        return userRepository.findByEmail(email)
            .filter(u -> StringUtils.isNotEmpty(u.getPassword()))
            .filter(u -> passwordService.matches(password, u.getPassword()));
    }

    @Transactional
    public User signUp(String firstName, String lastName, String email, String password, String confirmPassword) {
        if (StringUtils.isBlank(email) || !email.contains("@")) {
            throw ValidationFailedException.of("email", "Please enter a valid e-mail");
        }
        if (StringUtils.isEmpty(password) || !password.equals(confirmPassword)) {
            throw ValidationFailedException.of("cpassword", "Passwords dont match!");
        }
        if (userRepository.findByEmail(email.trim()).isPresent()) {
            throw ValidationFailedException.of("email", "This email already exists, please use forgot password");
        }
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email.trim());
        user.setPassword(passwordService.hash(password));
        userRepository.persist(user);
        mailService.sendQuietly(user.getEmail(), "Welcome to JPrime!",
            mailTemplates.render(MailTemplates.WELCOME, Map.of("firstName", StringUtils.defaultString(firstName))));
        return user;
    }

    /** Creates a reset token and mails the link. Silently does nothing for unknown e-mails. */
    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = newToken();
            tokenRepository.persist(new ResetPasswordToken(user, sha512(token)));
            mailService.sendQuietly(email, "Reset your JPrime password",
                mailTemplates.render(MailTemplates.RESET_PASSWORD, Map.of("firstName",
                    StringUtils.defaultString(user.getFirstName()), "url", resetPasswordUrl + token)));
        });
    }

    /** The owner of a valid (existing, unused, not expired) token. */
    @Transactional
    public Optional<User> checkToken(String token) {
        if (StringUtils.isBlank(token)) {
            return Optional.empty();
        }
        Optional<ResetPasswordToken> stored = tokenRepository.findByTokenId(sha512(token));
        if (stored.isEmpty()) {
            LOG.debug("Reset password token not found - could be brute forcing");
            return Optional.empty();
        }
        ResetPasswordToken resetToken = stored.get();
        if (resetToken.isUsed()) {
            return Optional.empty();
        }
        LocalDateTime created = resetToken.getCreatedDate();
        if (created == null || created.plusHours(tokenDurationHours).isBefore(LocalDateTime.now())) {
            return Optional.empty();
        }
        return Optional.of(resetToken.getOwner());
    }

    @Transactional
    public User resetPassword(String token, String password, String confirmPassword) {
        User owner = checkToken(token).orElseThrow(
            () -> ValidationFailedException.of("tokenId", "Your token is invalid. Request a new one"));
        if (StringUtils.isEmpty(password) || !password.equals(confirmPassword)) {
            throw ValidationFailedException.of("cpassword", "Passwords did not match");
        }
        owner.setPassword(passwordService.hash(password));
        tokenRepository.findAllByOwner(owner).forEach(t -> t.setUsed(true));
        return owner;
    }

    private static String newToken() {
        StringBuilder sb = new StringBuilder(128);
        for (int i = 0; i < 128; i++) {
            sb.append(TOKEN_CHARS[RANDOM.nextInt(TOKEN_CHARS.length)]);
        }
        return sb.toString();
    }

    static String sha512(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new JPrimeException(e);
        }
    }
}
