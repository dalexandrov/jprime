package org.bgjug.jprime.site.security;

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
import org.bgjug.jprime.core.service.AccountService;

/**
 * Form login of the public site: users and speakers stored in the {@code User} table (BCrypt passwords).
 */
@ApplicationScoped
public class SiteIdentityProvider implements IdentityProvider<UsernamePasswordAuthenticationRequest> {

    public static final String ROLE_USER = "USER";

    @Inject
    AccountService accountService;

    @Override
    public Class<UsernamePasswordAuthenticationRequest> getRequestType() {
        return UsernamePasswordAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(UsernamePasswordAuthenticationRequest request,
        AuthenticationRequestContext context) {
        String email = request.getUsername();
        String password = new String(request.getPassword().getPassword());
        return context.runBlocking(() -> check(email, password));
    }

    @ActivateRequestContext
    SecurityIdentity check(String email, String password) {
        return accountService.authenticate(email, password)
            .map(user -> (SecurityIdentity) QuarkusSecurityIdentity.builder()
                .setPrincipal(new QuarkusPrincipal(user.getEmail()))
                .addRole(ROLE_USER)
                .build())
            .orElseThrow(() -> new AuthenticationFailedException("Username/Password does not match for " + email));
    }
}
