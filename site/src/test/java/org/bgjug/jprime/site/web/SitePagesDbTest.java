package org.bgjug.jprime.site.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Smoke test of the public pages against a real MySQL (Dev Services, needs Docker) migrated with the original Flyway
 * scripts - so it also validates the entity mapping against the schema. Run with {@code mvn verify -Pdb-tests}.
 */
@QuarkusTest
@Tag("db")
class SitePagesDbTest {

    @Test
    void pagesRender() {
        for (String path : new String[] {"/", "/team", "/venue", "/speakers", "/agenda", "/nav", "/cfp", "/tickets",
            "/privacy-policy", "/privacy-policy?bg", "/qr", "/qr/tuk", "/account/login", "/account/signup",
            "/account/reset-password"}) {
            given().accept("text/html").when().get(path).then().statusCode(200).body(containsString("jPrime"));
        }
    }

    @Test
    void unknownSpeakerIs404() {
        given().accept("text/html").when().get("/speaker/999999").then().statusCode(404);
    }

    @Test
    void mobileAppApi() {
        given().when().get("/pwa/findSessionsByHall?hallName=Hall A").then().statusCode(200);
    }

    @Test
    void captchaImage() {
        given().when().get("/captcha-image").then().statusCode(200).contentType("image/png")
            .cookie(CaptchaService.COOKIE);
    }
}
