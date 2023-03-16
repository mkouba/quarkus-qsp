package io.quarkiverse.qsp.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class QspTest {

    @Test
    public void testTemplates() {
        given()
                .when().get("/qsp/hello")
                .then()
                .statusCode(200)
                .body(containsString("Hello world!"));

        // CDI bean
        given()
                .when().get("/qsp/names.html")
                .then()
                .statusCode(200)
                .body(containsString("<li>Violet</li>"));

        // Globals
        given()
                .when().get("/qsp/integers?name=foo")
                .then()
                .statusCode(200)
                .body(containsString("1:11:42"));

        // Static method
        given()
                .when().get("/qsp/colors")
                .then()
                .statusCode(200)
                .body(containsString("red, green, blue"));

        // Enum
        given()
                .when().get("/qsp/nested/enum")
                .then()
                .statusCode(200)
                .body(Matchers.is("OK"));

        // Not found
        given()
                .when().get("/qsp/fooooooo")
                .then()
                .statusCode(404);
    }
}
