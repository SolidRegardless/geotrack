package com.geotrack.api.model;

import com.geotrack.common.model.FenceType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Polygon;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for geofence zones stored as PostGIS polygons.
 */
@Entity
@Table(name = "geofences")
public class GeofenceEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(nullable = false)
    public String name;

    public String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "fence_type", nullable = false)
    public FenceType fenceType;

    @Column(name = "geometry", columnDefinition = "geometry(Polygon, 4326)", nullable = false)
    public Polygon geometry;

    @Column(name = "buffer_metres")
    public Double bufferMetres = 0.0;

    public boolean active = true;

    @Column(name = "alert_on_enter")
    public boolean alertOnEnter = true;

    @Column(name = "alert_on_exit")
    public boolean alertOnExit = true;

    @Column(columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    public String metadata;

    @Column(name = "created_at")
    public Instant createdAt = Instant.now();
}
