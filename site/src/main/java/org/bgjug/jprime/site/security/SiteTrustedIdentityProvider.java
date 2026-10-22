package org.bgjug.jprime.site.security;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TrustedAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import org.bgjug.jprime.core.repository.UserRepository;

/** Re-creates the identity of a logged in site user from the encrypted form-login cookie. */
@ApplicationScoped
public class SiteTrustedIdentityProvider implements IdentityProvider<TrustedAuthenticationRequest> {

    @Inject
    UserRepository userRepository;

    @Override
    public Class<TrustedAuthenticationRequest> getRequestType() {
        return TrustedAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(TrustedAuthenticationRequest request, AuthenticationRequestContext context) {
        String email = request.getPrincipal();
        return context.runBlocking(() -> lookup(email));
    }

    @ActivateRequestContext
    SecurityIdentity lookup(String email) {
        if (userRepository.findByEmail(email).isEmpty()) {
            throw new AuthenticationFailedException("Unknown user " + email);
        }
        return QuarkusSecurityIdentity.builder()
            .setPrincipal(new QuarkusPrincipal(email))
            .addRole(SiteIdentityProvider.ROLE_USER)
            .build();
    }
}
