package com.geotrack.api.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for storing position data with PostGIS spatial column.
 * <p>
 * The {@code location} field is a JTS Point mapped to a PostGIS geometry column
 * via Hibernate Spatial. PostGIS GIST index enables fast spatial queries.
 */
@Entity
@Table(name = "positions", indexes = {
        @Index(name = "idx_positions_asset_time", columnList = "asset_id, timestamp DESC")
})
public class PositionEntity extends PanacheEntityBase {

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "asset_id", nullable = false)
    public UUID assetId;

    /**
     * PostGIS geometry column — spatial queries use this via GIST index.
     * JTS Point is mapped by Hibernate Spatial.
     */
    @Column(name = "location", columnDefinition = "geometry(Point, 4326)", nullable = false)
    public Point location;

    public Double altitude;
    public Double speed;
    public Double heading;
    public Double accuracy;

    @Column(name = "source")
    public String source;

    @Column(name = "timestamp", nullable = false)
    public Instant timestamp;

    @Column(name = "received_at")
    public Instant receivedAt = Instant.now();

    @Column(columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    public String metadata;

    /**
     * Factory method — creates entity from raw coordinates.
     */
    public static PositionEntity fromCoordinates(UUID assetId, double longitude, double latitude, Instant timestamp) {
        PositionEntity entity = new PositionEntity();
        entity.assetId = assetId;
        entity.location = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
        entity.timestamp = timestamp;
        return entity;
    }

    /**
     * Convenience accessors for latitude/longitude from the JTS Point.
     */
    public double getLatitude() {
        return location != null ? location.getY() : 0;
    }

    public double getLongitude() {
        return location != null ? location.getX() : 0;
    }
}
