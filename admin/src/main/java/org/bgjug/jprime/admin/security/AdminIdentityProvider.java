package org.bgjug.jprime.admin.security;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.bgjug.jprime.core.service.AccountService;
import org.bgjug.jprime.core.service.PasswordService;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Who may use the admin application:
 * <ul>
 *     <li>the built-in {@code admin} account with the password from {@code jprime.admin.password} (plain text or a
 *     BCrypt hash) - the same "admin hack" the legacy site had;</li>
 *     <li>registered users (User table) whose e-mail is listed in {@code jprime.admin.emails}.</li>
 * </ul>
 */
@ApplicationScoped
public class AdminIdentityProvider implements IdentityProvider<UsernamePasswordAuthenticationRequest> {

    public static final String ROLE_ADMIN = "ADMIN";

    @ConfigProperty(name = "jprime.admin.username", defaultValue = "admin")
    String adminUsername;

    @ConfigProperty(name = "jprime.admin.password")
    Optional<String> adminPassword;

    @ConfigProperty(name = "jprime.admin.emails")
    Optional<List<String>> adminEmails;

    @Inject
    AccountService accountService;

    @Inject
    PasswordService passwordService;

    @Override
    public Class<UsernamePasswordAuthenticationRequest> getRequestType() {
        return UsernamePasswordAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(UsernamePasswordAuthenticationRequest request,
        AuthenticationRequestContext context) {
        String username = request.getUsername();
        String password = new String(request.getPassword().getPassword());
        return context.runBlocking(() -> check(username, password));
    }

    @ActivateRequestContext
    SecurityIdentity check(String username, String password) {
        if (adminUsername.equals(username) && adminPassword.filter(StringUtils::isNotBlank).isPresent()
            && matchesConfigured(password, adminPassword.get())) {
            return identity(username);
        }
        boolean listed = adminEmails.orElse(List.of()).stream().anyMatch(e -> e.trim().equalsIgnoreCase(username));
        if (listed && accountService.authenticate(username, password).isPresent()) {
            return identity(username);
        }
        throw new AuthenticationFailedException("Invalid credentials for " + username);
    }

    private boolean matchesConfigured(String given, String configured) {
        if (configured.startsWith("$2")) {
            return passwordService.matches(given, configured);
        }
        return MessageDigest.isEqual(given.getBytes(StandardCharsets.UTF_8), configured.getBytes(StandardCharsets.UTF_8));
    }

    private static SecurityIdentity identity(String name) {
        return QuarkusSecurityIdentity.builder()
            .setPrincipal(new QuarkusPrincipal(name))
            .addRole(ROLE_ADMIN)
            .build();
    }
}
