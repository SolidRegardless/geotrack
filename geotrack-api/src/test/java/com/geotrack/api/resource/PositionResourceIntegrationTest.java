package com.geotrack.api.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for Position API endpoints.
 * Tests the full ingestion pipeline: REST → Service → PostGIS storage.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PositionResourceIntegrationTest {

    private static String assetId;

    @BeforeAll
    static void createTestAsset() {
        // Create an asset to attach positions to
        assetId = given()
                .contentType(ContentType.JSON)
                .body("""
                    {"name": "Position Test Vehicle", "type": "VEHICLE"}
                    """)
            .when()
                .post("/api/v1/assets")
            .then()
                .statusCode(201)
            .extract()
                .path("id");
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/v1/positions — should submit a position")
    void shouldSubmitPosition() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "assetId": "%s",
                        "latitude": 54.9783,
                        "longitude": -1.6178,
                        "altitude": 50.0,
                        "speed": 45.0,
                        "heading": 180.0,
                        "timestamp": "%s"
                    }
                    """.formatted(assetId, Instant.now().toString()))
            .when()
                .post("/api/v1/positions")
            .then()
                .statusCode(201)
                .body("assetId", equalTo(assetId))
                .body("latitude", closeTo(54.9783, 0.001))
                .body("longitude", closeTo(-1.6178, 0.001))
                .body("speed", closeTo(45.0, 0.1));
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/v1/positions — should submit a second position")
    void shouldSubmitSecondPosition() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "assetId": "%s",
                        "latitude": 54.9800,
                        "longitude": -1.6100,
                        "altitude": 55.0,
                        "speed": 50.0,
                        "heading": 90.0,
                        "timestamp": "%s"
                    }
                    """.formatted(assetId, Instant.now().toString()))
            .when()
                .post("/api/v1/positions")
            .then()
                .statusCode(201);
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/v1/positions/latest — should return latest positions")
    void shouldGetLatestPositions() {
        given()
            .when()
                .get("/api/v1/positions/latest")
            .then()
                .statusCode(200)
                .body("$.size()", greaterThanOrEqualTo(1));
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/v1/assets/{id}/positions — should return position history")
    void shouldGetPositionHistory() {
        given()
            .when()
                .get("/api/v1/assets/{id}/positions", assetId)
            .then()
                .statusCode(200)
                .body("$.size()", greaterThanOrEqualTo(2));
    }

    @Test
    @Order(5)
    @DisplayName("POST /api/v1/positions — should reject invalid latitude")
    void shouldRejectInvalidLatitude() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "assetId": "%s",
                        "latitude": 91.0,
                        "longitude": -1.6178,
                        "timestamp": "%s"
                    }
                    """.formatted(assetId, Instant.now().toString()))
            .when()
                .post("/api/v1/positions")
            .then()
                .statusCode(400);
    }

    @Test
    @Order(6)
    @DisplayName("POST /api/v1/positions — should reject Null Island (0,0)")
    void shouldRejectNullIsland() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "assetId": "%s",
                        "latitude": 0.0,
                        "longitude": 0.0,
                        "timestamp": "%s"
                    }
                    """.formatted(assetId, Instant.now().toString()))
            .when()
                .post("/api/v1/positions")
            .then()
                .statusCode(400);
    }
}
