package ru.qa.blogapi.tests.api;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.qa.blogapi.auth.AuthApiClient;
import ru.qa.blogapi.auth.AuthSession;
import ru.qa.blogapi.base.BaseAuthorizedApiTest;
import ru.qa.blogapi.models.PostCreateRequest;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class BlogApiHomeworkTest extends BaseAuthorizedApiTest {

    @Test
    @DisplayName("POST /api/auth/register -> should register user with valid required fields")
    void shouldRegisterUserWithValidRequiredFields() {
        String email = randomEmail();
        String password = "SecurePass123!";

        Map<String, Object> body = registrationBody(email, password);

        given()
                .spec(requestSpec)
                .body(body)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(200)
                .body("status", equalTo("success"))
                .body("message", equalTo("User registered successfully"))
                .body("user.id", notNullValue())
                .body("user.email", equalTo(email))
                .body("user.firstName", equalTo(body.get("firstName")))
                .body("user.lastName", equalTo(body.get("lastName")))
                .body("user.nickname", equalTo(body.get("nickname")))
                .body("user.birthDate", equalTo(body.get("birthDate")))
                .body("user.phone", equalTo(body.get("phone")));
    }

    @Test
    @DisplayName("POST /api/auth/register -> should return validation error for invalid email")
    void shouldReturnValidationErrorForInvalidEmailOnRegistration() {
        Map<String, Object> body = registrationBody("invalid-email", "SecurePass123!");

        given()
                .spec(requestSpec)
                .body(body)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(400)
                .body("error", notNullValue())
                .body("error.code", equalTo(400))
                .body("error.message", notNullValue());
    }

    @Test
    @DisplayName("POST /api/login -> should login with valid credentials")
    void shouldLoginWithValidCredentials() {
        String email = randomEmail();
        String password = "SecurePass123!";

        registerUser(email, password);

        given()
                .spec(requestSpec)
                .body(loginBody(email, password))
                .when()
                .post("/api/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("refresh_token", notNullValue());
    }

    @Test
    @DisplayName("POST /api/login -> should return unauthorized for wrong password")
    void shouldReturnUnauthorizedForWrongPassword() {
        String email = randomEmail();
        String password = "SecurePass123!";

        registerUser(email, password);

        given()
                .spec(requestSpec)
                .body(loginBody(email, "WrongPassword999!"))
                .when()
                .post("/api/login")
                .then()
                .statusCode(401)
                .body("error", nullValue())
                .body("code", equalTo(401))
                .body("message", equalTo("Invalid credentials."));
    }

    @Test
    @DisplayName("POST /api/token/refresh -> should refresh access token by refresh token")
    void shouldRefreshAccessToken() {
        String email = randomEmail();
        String password = "SecurePass123!";

        registerUser(email, password);

        String refreshToken = given()
                .spec(requestSpec)
                .body(loginBody(email, password))
                .when()
                .post("/api/login")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("refresh_token");

        Map<String, Object> body = new HashMap<>();
        body.put("refresh_token", refreshToken);

        given()
                .spec(requestSpec)
                .body(body)
                .when()
                .post("/api/token/refresh")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("refresh_token", notNullValue());
    }

    @Test
    @DisplayName("GET /api/profile -> should return current user profile for authorized user")
    void shouldReturnCurrentUserProfile() {
        given()
                .spec(authorizedRequestSpec)
                .when()
                .get("/api/profile")
                .then()
                .statusCode(200)
                .body("user", notNullValue())
                .body("user.id", notNullValue());
    }

    @Test
    @DisplayName("PUT /api/profile -> should update current user profile")
    void shouldUpdateCurrentUserProfile() {
        String newFirstName = "Updated_" + suffix(5);
        String newLastName = "Name_" + suffix(5);

        Map<String, Object> body = new HashMap<>();
        body.put("firstName", newFirstName);
        body.put("lastName", newLastName);

        given()
                .spec(authorizedRequestSpec)
                .body(body)
                .when()
                .put("/api/profile")
                .then()
                .statusCode(200)
                .body("status", equalTo("success"))
                .body("user.firstName", equalTo(newFirstName))
                .body("user.lastName", equalTo(newLastName));
    }

    @Test
    @DisplayName("GET /api/posts -> should return paginated list of posts")
    void shouldReturnPaginatedPostsList() {
        given()
                .spec(authorizedRequestSpec)
                .queryParam("page", 1)
                .queryParam("limit", 10)
                .when()
                .get("/api/posts")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .body("totalItems", notNullValue())
                .body("itemsPerPage", equalTo(10))
                .body("page", equalTo(1))
                .body("pages", notNullValue());
    }

    @Test
    @DisplayName("GET /api/posts -> should filter posts by category")
    void shouldFilterPostsByCategory() {
        PostCreateRequest request = new PostCreateRequest(
                "Tech Post " + suffix(6), "Body content", "Short desc", "technology", false
        );
        given()
                .spec(authorizedRequestSpec)
                .body(request)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(201);

        given()
                .spec(authorizedRequestSpec)
                .queryParam("category", "technology")
                .when()
                .get("/api/posts")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .body("items.category", everyItem(equalTo("technology")));
    }

    @Test
    @DisplayName("POST /api/posts -> should create published post")
    void shouldCreatePublishedPost() {
        String title = "Published " + suffix(6);
        PostCreateRequest request = new PostCreateRequest(
                title, "Full body content for the post", "Short description", "technology", false
        );

        given()
                .spec(authorizedRequestSpec)
                .body(request)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(201)
                .body("status", equalTo("success"))
                .body("post.id", notNullValue())
                .body("post.title", equalTo(title))
                .body("post.isDraft", equalTo(false))
                .body("post.author.email", equalTo(authSession.getEmail()));
    }

    @Test
    @DisplayName("POST /api/posts -> should create draft post")
    void shouldCreateDraftPost() {
        String title = "Draft " + suffix(6);
        PostCreateRequest request = new PostCreateRequest(
                title, "Draft body content", "Draft description", "travel", true
        );

        given()
                .spec(authorizedRequestSpec)
                .body(request)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(201)
                .body("post.id", notNullValue())
                .body("post.isDraft", equalTo(true));
    }

    @Test
    @DisplayName("GET /api/posts/my -> should return only current user posts")
    void shouldReturnOnlyCurrentUserPosts() {
        PostCreateRequest request = new PostCreateRequest(
                "My Post " + suffix(6), "Body content", "Short desc", "technology", false
        );
        given()
                .spec(authorizedRequestSpec)
                .body(request)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(201);

        given()
                .spec(authorizedRequestSpec)
                .when()
                .get("/api/posts/my")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .body("items.author.email", everyItem(equalTo(authSession.getEmail())));
    }

    @Test
    @DisplayName("GET /api/posts/feed -> should return posts from other users")
    void shouldReturnFeedPosts() {
        AuthApiClient secondClient = new AuthApiClient(requestSpec);
        AuthSession secondSession = secondClient.createAuthorizedSession();

        RequestSpecification secondUserSpec = new RequestSpecBuilder()
                .addRequestSpecification(requestSpec)
                .addHeader("Authorization", "Bearer " + secondSession.getAccessToken())
                .build();

        PostCreateRequest request = new PostCreateRequest(
                "Feed Post " + suffix(6), "Body from second user", "Short desc", "travel", false
        );
        given()
                .spec(secondUserSpec)
                .body(request)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(201);

        given()
                .spec(authorizedRequestSpec)
                .when()
                .get("/api/posts/feed")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .body("items.author.email", not(hasItem(authSession.getEmail())));
    }

    @Test
    @DisplayName("GET /api/posts/{id} -> should return single post by id")
    void shouldReturnSinglePostById() {
        String title = "Single Post " + suffix(6);
        PostCreateRequest request = new PostCreateRequest(
                title, "Body content", "Short desc", "technology", false
        );
        int postId = given()
                .spec(authorizedRequestSpec)
                .body(request)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getInt("post.id");

        given()
                .spec(authorizedRequestSpec)
                .pathParam("id", postId)
                .when()
                .get("/api/posts/{id}")
                .then()
                .statusCode(200)
                .body("post.id", equalTo(postId))
                .body("post.title", equalTo(title))
                .body("statistics", notNullValue());
    }

    @Test
    @DisplayName("PUT /api/posts/{id} -> should update existing post")
    void shouldUpdateExistingPost() {
        PostCreateRequest createRequest = new PostCreateRequest(
                "Original " + suffix(6), "Original body", "Original desc", "technology", false
        );
        int postId = given()
                .spec(authorizedRequestSpec)
                .body(createRequest)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getInt("post.id");

        String newTitle = "Updated " + suffix(6);
        String newDescription = "Updated desc " + suffix(4);

        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("title", newTitle);
        updateBody.put("description", newDescription);

        given()
                .spec(authorizedRequestSpec)
                .pathParam("id", postId)
                .body(updateBody)
                .when()
                .put("/api/posts/{id}")
                .then()
                .statusCode(200)
                .body("status", equalTo("success"))
                .body("post.id", equalTo(postId))
                .body("post.title", equalTo(newTitle))
                .body("post.description", equalTo(newDescription));
    }

    @Test
    @DisplayName("DELETE /api/posts/{id} -> should delete post")
    void shouldDeletePost() {
        PostCreateRequest request = new PostCreateRequest(
                "To Delete " + suffix(6), "Body content", "Short desc", "technology", false
        );
        int postId = given()
                .spec(authorizedRequestSpec)
                .body(request)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getInt("post.id");

        given()
                .spec(authorizedRequestSpec)
                .pathParam("id", postId)
                .when()
                .delete("/api/posts/{id}")
                .then()
                .statusCode(200)
                .body("status", equalTo("success"))
                .body("message", notNullValue());

        given()
                .spec(authorizedRequestSpec)
                .pathParam("id", postId)
                .when()
                .get("/api/posts/{id}")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("POST /api/posts/{id}/favorite -> should add post to favorites")
    void shouldAddPostToFavorites() {
        PostCreateRequest request = new PostCreateRequest(
                "Fav Post " + suffix(6), "Body content", "Short desc", "technology", false
        );
        int postId = given()
                .spec(authorizedRequestSpec)
                .body(request)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getInt("post.id");

        Map<String, Object> favoriteBody = new HashMap<>();
        favoriteBody.put("isFavorite", true);

        given()
                .spec(authorizedRequestSpec)
                .pathParam("id", postId)
                .body(favoriteBody)
                .when()
                .post("/api/posts/{id}/favorite")
                .then()
                .statusCode(200)
                .body("status", equalTo("success"))
                .body("isFavorite", equalTo(true));
    }

    @Test
    @DisplayName("GET /api/posts/favorites -> should return favorite posts")
    void shouldReturnFavoritePosts() {
        PostCreateRequest request = new PostCreateRequest(
                "Fav List " + suffix(6), "Body content", "Short desc", "technology", false
        );
        int postId = given()
                .spec(authorizedRequestSpec)
                .body(request)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getInt("post.id");

        Map<String, Object> favoriteBody = new HashMap<>();
        favoriteBody.put("isFavorite", true);

        given()
                .spec(authorizedRequestSpec)
                .pathParam("id", postId)
                .body(favoriteBody)
                .when()
                .post("/api/posts/{id}/favorite")
                .then()
                .statusCode(200);

        given()
                .spec(authorizedRequestSpec)
                .when()
                .get("/api/posts/favorites")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .body("items.id", hasItem(postId));
    }

    @Test
    @DisplayName("POST /api/files/upload -> should upload image file for post")
    void shouldUploadImageFileForPost() {
        byte[] pngBytes = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
        );

        given()
                .spec(authorizedRequestSpec)
                .contentType("multipart/form-data")
                .multiPart("file", "test.png", pngBytes, "image/png")
                .multiPart("type", "post-image")
                .when()
                .post("/api/files/upload")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("url", notNullValue())
                .body("mimeType", notNullValue())
                .body("filename", notNullValue());
    }

    @Test
    @DisplayName("GET /api/files/{id} -> should return uploaded file metadata")
    void shouldReturnUploadedFileMetadata() {
        byte[] pngBytes = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
        );

        int fileId = given()
                .spec(authorizedRequestSpec)
                .contentType("multipart/form-data")
                .multiPart("file", "test.png", pngBytes, "image/png")
                .multiPart("type", "post-image")
                .when()
                .post("/api/files/upload")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getInt("id");

        given()
                .spec(authorizedRequestSpec)
                .pathParam("id", fileId)
                .when()
                .get("/api/files/{id}")
                .then()
                .statusCode(200)
                .body("id", equalTo(fileId))
                .body("url", notNullValue())
                .body("filename", notNullValue())
                .body("size", notNullValue())
                .body("mimeType", notNullValue());
    }

    @Test
    @DisplayName("POST /api/profile/report/{id} -> should create report for user")
    void shouldCreateUserReport() {
        String targetEmail = randomEmail();
        Response regResponse = registerUser(targetEmail, "SecurePass123!");
        int targetUserId = regResponse.jsonPath().getInt("user.id");

        Map<String, Object> body = new HashMap<>();
        body.put("descriptionReport", "Test report reason");

        given()
                .spec(authorizedRequestSpec)
                .pathParam("id", targetUserId)
                .body(body)
                .when()
                .post("/api/profile/report/{id}")
                .then()
                .statusCode(200)
                .body("status", equalTo("success"))
                .body("message", notNullValue());
    }

    private Response registerUser(String email, String password) {
        return given()
                .spec(requestSpec)
                .body(registrationBody(email, password))
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(200)
                .extract()
                .response();
    }

    private Map<String, Object> registrationBody(String email, String password) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);
        body.put("firstName", "Ronam");
        body.put("lastName", "Doe");
        body.put("nickname", "roman_" + suffix(5));
        body.put("birthDate", "1990-01-02");
        body.put("phone", randomPhone());
        return body;
    }

    private Map<String, Object> loginBody(String email, String password) {
        Map<String, Object> body = new HashMap<>();
        body.put("username", email);
        body.put("password", password);
        return body;
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
