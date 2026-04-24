package ru.qa.blogapi.tests.ui;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.support.ui.WebDriverWait;
import ru.qa.blogapi.base.BaseUiTest;
import ru.qa.blogapi.pages.LoginPage;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LoginUiTest extends BaseUiTest {

    @Test
    @Tag("e2e")
    @DisplayName("UI /login -> should login with existing user credentials")
    void shouldLoginWithExistingUserCredentials() {
        String email = randomEmail();
        String password = "SecurePass123!";

        registerUserViaApi(email, password);

        LoginPage loginPage = new LoginPage(driver);
        loginPage.open(uiBaseUrl);
        loginPage.fillEmail(email);
        loginPage.fillPassword(password);
        loginPage.clickLogin();

        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(driver -> !driver.getCurrentUrl().contains("/login"));

        assertFalse(driver.getCurrentUrl().contains("/login"));
    }

    private void registerUserViaApi(String email, String password) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);
        body.put("firstName", "Ronam");
        body.put("lastName", "Doe");
        body.put("nickname", "roman_" + suffix(5));
        body.put("birthDate", "1990-01-02");
        body.put("phone", randomPhone());

        given()
                .baseUri(apiBaseUrl)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(200);
    }

    private String randomEmail() {
        return "student_" + suffix(8) + "@example.com";
    }

    private String randomPhone() {
        return "+79" + UUID.randomUUID()
                .toString()
                .replaceAll("[^0-9]", "")
                .substring(0, 9);
    }

    private String suffix(int length) {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, length);
    }
}
