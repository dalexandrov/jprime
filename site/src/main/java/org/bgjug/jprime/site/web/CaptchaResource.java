package org.bgjug.jprime.site.web;

import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

@Path("/captcha-image")
public class CaptchaResource {

    @Inject
    CaptchaService captchaService;

    @GET
    public Response image(@QueryParam("width") @DefaultValue("150") int width,
        @QueryParam("height") @DefaultValue("30") int height) {
        CaptchaService.Captcha captcha = captchaService.generate(Math.min(Math.max(width, 100), 400),
            Math.min(Math.max(height, 20), 100));
        NewCookie cookie = new NewCookie.Builder(CaptchaService.COOKIE)
            .value(captcha.cookieValue())
            .path("/")
            .httpOnly(true)
            .sameSite(NewCookie.SameSite.LAX)
            .build();
        CacheControl noCache = new CacheControl();
        noCache.setNoStore(true);
        noCache.setNoCache(true);
        return Response.ok(captcha.png()).type("image/png").cookie(cookie).cacheControl(noCache).build();
    }
}
