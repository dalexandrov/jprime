package org.bgjug.jprime.admin.security;

import java.util.List;
import java.util.Optional;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TrustedAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Re-creates the identity from the encrypted form-login cookie on every request (the cookie only carries the user
 * name, so the role is re-derived here - removing an e-mail from {@code jprime.admin.emails} revokes access).
 */
@ApplicationScoped
public class AdminTrustedIdentityProvider implements IdentityProvider<TrustedAuthenticationRequest> {

    @ConfigProperty(name = "jprime.admin.username", defaultValue = "admin")
    String adminUsername;

    @ConfigProperty(name = "jprime.admin.emails")
    Optional<List<String>> adminEmails;

    @Override
    public Class<TrustedAuthenticationRequest> getRequestType() {
        return TrustedAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(TrustedAuthenticationRequest request, AuthenticationRequestContext context) {
        String name = request.getPrincipal();
        boolean allowed = adminUsername.equals(name)
            || adminEmails.orElse(List.of()).stream().anyMatch(e -> e.trim().equalsIgnoreCase(name));
        if (!allowed) {
            return Uni.createFrom().failure(new AuthenticationFailedException("Not an admin: " + name));
        }
        return Uni.createFrom().item(QuarkusSecurityIdentity.builder()
            .setPrincipal(new QuarkusPrincipal(name))
            .addRole(AdminIdentityProvider.ROLE_ADMIN)
            .build());
    }
}
