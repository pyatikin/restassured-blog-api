package ru.qa.blogapi.auth;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

public class AuthApiClient {

    private final RequestSpecification requestSpec;

    public AuthApiClient(RequestSpecification requestSpec) {
        this.requestSpec = requestSpec;
    }

    public AuthSession createAuthorizedSession() {
        String email = "student_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String password = "SecurePass123!";

        Response registerResponse = register(email, password);
        Integer userId = registerResponse.jsonPath().getInt("user.id");

        Response loginResponse = login(email, password);
        String refreshToken = loginResponse.jsonPath().getString("refresh_token");

        Response refreshResponse = refresh(refreshToken);
        String accessToken = refreshResponse.jsonPath().getString("token");
        String newRefreshToken = refreshResponse.jsonPath().getString("refresh_token");

        AuthSession session = new AuthSession();
        session.setUserId(userId);
        session.setEmail(email);
        session.setPassword(password);
        session.setAccessToken(accessToken);
        session.setRefreshToken(newRefreshToken);

        return session;
    }

    private Response register(String email, String password) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);
        body.put("firstName", "Ronam");
        body.put("lastName", "Doe");
        body.put("nickname", "roman_" + UUID.randomUUID().toString().substring(0, 5));
        body.put("birthDate", "1990-01-02");
        body.put("phone", randomPhone());

        return given()
                .spec(requestSpec)
                .body(body)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(200)
                .extract()
                .response();
    }

    private Response login(String email, String password) {
        Map<String, Object> body = new HashMap<>();
        body.put("username", email);
        body.put("password", password);

        return given()
                .spec(requestSpec)
                .body(body)
                .when()
                .post("/api/login")
                .then()
                .statusCode(200)
                .extract()
                .response();
    }

    private Response refresh(String refreshToken) {
        Map<String, Object> body = new HashMap<>();
        body.put("refresh_token", refreshToken);

        return given()
                .spec(requestSpec)
                .body(body)
                .when()
                .post("/api/token/refresh")
                .then()
                .statusCode(200)
                .extract()
                .response();
    }

    private String randomPhone() {
        return "+79" + UUID.randomUUID().toString().replaceAll("[^0-9]", "").substring(0, 9);
    }
}