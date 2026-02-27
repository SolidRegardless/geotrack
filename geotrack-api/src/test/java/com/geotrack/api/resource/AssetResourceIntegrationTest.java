package com.geotrack.api.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for the Asset REST API.
 * <p>
 * Uses {@code @QuarkusTest} which starts the full Quarkus application with
 * Dev Services — automatically spins up PostgreSQL/PostGIS, Kafka, and Redis
 * containers via Testcontainers. No manual Docker Compose needed.
 * <p>
 * These tests exercise the full stack: HTTP → JAX-RS → Service → Repository → PostGIS.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AssetResourceIntegrationTest {

    private static String createdAssetId;

    @Test
    @Order(1)
    @DisplayName("POST /api/v1/assets — should create a new asset")
    void shouldCreateAsset() {
        createdAssetId = given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "Test Vehicle Alpha",
                        "type": "VEHICLE"
                    }
                    """)
            .when()
                .post("/api/v1/assets")
            .then()
                .statusCode(201)
                .header("Location", containsString("/api/v1/assets/"))
                .body("name", equalTo("Test Vehicle Alpha"))
                .body("type", equalTo("VEHICLE"))
                .body("status", equalTo("ACTIVE"))
                .body("id", notNullValue())
            .extract()
                .path("id");
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/v1/assets — should list assets")
    void shouldListAssets() {
        given()
            .when()
                .get("/api/v1/assets")
            .then()
                .statusCode(200)
                .body("$.size()", greaterThanOrEqualTo(1))
                .body("[0].name", notNullValue());
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/v1/assets/{id} — should get asset by ID")
    void shouldGetAssetById() {
        given()
            .when()
                .get("/api/v1/assets/{id}", createdAssetId)
            .then()
                .statusCode(200)
                .body("id", equalTo(createdAssetId))
                .body("name", equalTo("Test Vehicle Alpha"));
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/v1/assets?type=VEHICLE — should filter by type")
    void shouldFilterByType() {
        given()
                .queryParam("type", "VEHICLE")
            .when()
                .get("/api/v1/assets")
            .then()
                .statusCode(200)
                .body("$.size()", greaterThanOrEqualTo(1))
                .body("[0].type", equalTo("VEHICLE"));
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/v1/assets?type=AIRCRAFT — should return empty for unmatched type")
    void shouldReturnEmptyForUnmatchedType() {
        given()
                .queryParam("type", "AIRCRAFT")
            .when()
                .get("/api/v1/assets")
            .then()
                .statusCode(200)
                .body("$.size()", equalTo(0));
    }

    @Test
    @Order(6)
    @DisplayName("POST /api/v1/assets — should reject invalid request")
    void shouldRejectInvalidRequest() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "",
                        "type": "VEHICLE"
                    }
                    """)
            .when()
                .post("/api/v1/assets")
            .then()
                .statusCode(400);
    }

    @Test
    @Order(7)
    @DisplayName("DELETE /api/v1/assets/{id} — should soft-delete asset")
    void shouldSoftDeleteAsset() {
        given()
            .when()
                .delete("/api/v1/assets/{id}", createdAssetId)
            .then()
                .statusCode(204);

        // Verify it's decommissioned, not actually deleted
        given()
            .when()
                .get("/api/v1/assets/{id}", createdAssetId)
            .then()
                .statusCode(200)
                .body("status", equalTo("DECOMMISSIONED"));
    }

    @Test
    @Order(8)
    @DisplayName("GET /api/v1/assets/{random-uuid} — should return 404")
    void shouldReturn404ForUnknownAsset() {
        given()
            .when()
                .get("/api/v1/assets/{id}", "00000000-0000-0000-0000-000000000000")
            .then()
                .statusCode(404);
    }
}
