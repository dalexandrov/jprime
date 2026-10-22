package org.bgjug.jprime.admin.api;

import java.util.Set;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Login state for the React UI. Logging in itself is {@code POST /j_security_check} (Quarkus form authentication).
 */
@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    public record Me(String username, Set<String> roles) {
    }

    @Inject
    SecurityIdentity identity;

    @ConfigProperty(name = "quarkus.http.auth.form.cookie-name", defaultValue = "quarkus-credential")
    String cookieName;

    @GET
    @Path("me")
    public Response me() {
        if (identity.isAnonymous()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.ok(new Me(identity.getPrincipal().getName(), identity.getRoles())).build();
    }

    @POST
    @Path("logout")
    public Response logout() {
        NewCookie expired = new NewCookie.Builder(cookieName).value("").path("/").maxAge(0).build();
        return Response.noContent().cookie(expired).build();
    }
}
