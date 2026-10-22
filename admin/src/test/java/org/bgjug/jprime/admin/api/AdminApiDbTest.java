package org.bgjug.jprime.admin.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.filter.cookie.CookieFilter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Needs Docker (Dev Services MySQL with the original Flyway scripts). Run with {@code mvn verify -Pdb-tests}.
 */
@QuarkusTest
@Tag("db")
class AdminApiDbTest {

    @Test
    void apiRequiresLogin() {
        given().when().get("/api/admin/branches").then().statusCode(401);
        given().when().get("/api/auth/me").then().statusCode(401);
    }

    @Test
    void formLoginAndBranches() {
        CookieFilter cookies = new CookieFilter();
        given().filter(cookies)
            .contentType("application/x-www-form-urlencoded")
            .formParam("j_username", "admin")
            .formParam("j_password", "password")
            .when().post("/j_security_check")
            .then().statusCode(200);

        given().filter(cookies).when().get("/api/auth/me").then().statusCode(200).body("username", equalTo("admin"));
        // the initial data migration creates the branches 2015-2025
        given().filter(cookies).when().get("/api/admin/branches").then().statusCode(200).body("year", hasItem(2025));
        given().filter(cookies).when().get("/api/admin/lookups").then().statusCode(200);
        given().filter(cookies).when().get("/api/admin/submissions").then().statusCode(200);
        given().filter(cookies).when().get("/api/admin/visitors").then().statusCode(200);
    }

    @Test
    void wrongPasswordIsRejected() {
        given().contentType("application/x-www-form-urlencoded")
            .formParam("j_username", "admin")
            .formParam("j_password", "wrong")
            .when().post("/j_security_check")
            .then().statusCode(401);
    }

    @Test
    void legacyCheckInApiUsesBasicAuth() {
        given().auth().preemptive().basic("admin", "password").when().get("/api/speaker").then().statusCode(200);
        given().when().get("/api/visitor/2025").then().statusCode(401);
    }
}
