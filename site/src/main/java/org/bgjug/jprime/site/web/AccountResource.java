package org.bgjug.jprime.site.web;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.bgjug.jprime.core.model.User;
import org.bgjug.jprime.core.service.AccountService;
import org.bgjug.jprime.core.service.ValidationFailedException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Sign-up, login form, logout and password reset of the public site. The actual login POST
 * ({@code /j_security_check}).
 * URLs: /account/login, /account/logout, /account/signup, /account/reset-password, /account/new-password is handled by Quarkus form authentication.
 */
@Path("/account")
@Produces(MediaType.TEXT_HTML)
public class AccountResource {

    @Inject
    @Location("account/login.html")
    Template login;

    @Inject
    @Location("account/signup.html")
    Template signup;

    @Inject
    @Location("account/reset-password.html")
    Template resetPassword;

    @Inject
    @Location("account/new-password.html")
    Template newPassword;

    @Inject
    @Location("account/message.html")
    Template message;

    @Inject
    AccountService accountService;

    @ConfigProperty(name = "quarkus.http.auth.form.cookie-name", defaultValue = "quarkus-credential")
    String credentialCookie;

    @GET
    @Path("login")
    public String login(@QueryParam("error") String error) {
        return login.data("error", error != null ? "Invalid e-mail or password." : null).render();
    }

    @GET
    @Path("logout")
    public Response logout() {
        NewCookie expired = new NewCookie.Builder(credentialCookie).path("/").maxAge(0).value("").build();
        return Response.seeOther(URI.create("/")).cookie(expired).build();
    }

    @GET
    @Path("signup")
    public String signupForm() {
        return signup.data("form", Map.of()).data("errors", Map.of()).render();
    }

    @POST
    @Path("signup")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response signup(@FormParam("firstName") String firstName, @FormParam("lastName") String lastName,
        @FormParam("email") String email, @FormParam("password") String password,
        @FormParam("cpassword") String cpassword) {
        try {
            accountService.signUp(firstName, lastName, email, password, cpassword);
        } catch (ValidationFailedException e) {
            Map<String, String> form = Map.of("firstName", nn(firstName), "lastName", nn(lastName), "email", nn(email));
            return Response.ok(signup.data("form", form).data("errors", e.getErrors()).render()).build();
        }
        return Response.ok(message.data("title", "Welcome to jPrime!")
            .data("text", "Your account was created. You can now log in.")
            .data("link", "/account/login")
            .render()).build();
    }

    @GET
    @Path("reset-password")
    public String resetPasswordForm(@QueryParam("sent") String sentTo) {
        return resetPassword.data("sentTo", sentTo).data("error", null).render();
    }

    @POST
    @Path("reset-password")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response resetPassword(@FormParam("email") String email) {
        if (email == null || !email.contains("@")) {
            return Response.ok(resetPassword.data("sentTo", null).data("error", "Please, enter a valid mail address")
                .render()).build();
        }
        accountService.requestPasswordReset(email.trim());
        return Response.seeOther(URI.create("/account/reset-password?sent=" + java.net.URLEncoder.encode(email.trim(),
            java.nio.charset.StandardCharsets.UTF_8))).build();
    }

    @GET
    @Path("new-password")
    public Response newPasswordForm(@QueryParam("tokenId") String tokenId) {
        Optional<User> owner = accountService.checkToken(tokenId);
        if (owner.isEmpty()) {
            return Response.seeOther(URI.create("/")).build();
        }
        return Response.ok(newPassword.data("tokenId", tokenId).data("error", null).render()).build();
    }

    @POST
    @Path("new-password")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String createNewPassword(@FormParam("tokenId") String tokenId, @FormParam("password") String password,
        @FormParam("cpassword") String cpassword) {
        try {
            User user = accountService.resetPassword(tokenId, password, cpassword);
            return message.data("title", "Password changed")
                .data("text", "Successfully changed password for user: " + user.getName() + " (" + user.getEmail() + ")")
                .data("link", "/account/login")
                .render();
        } catch (ValidationFailedException e) {
            return newPassword.data("tokenId", tokenId).data("error", e.getErrors().values().iterator().next())
                .render();
        }
    }

    private static String nn(String value) {
        return value == null ? "" : value;
    }
}
