package com.geotrack.api.model;

import com.geotrack.common.model.Severity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for tracking alerts (geofence breaches, speed violations, etc.)
 */
@Entity
@Table(name = "alerts")
public class AlertEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    @Column(name = "asset_id", nullable = false)
    public UUID assetId;

    @Column(name = "geofence_id")
    public UUID geofenceId;

    @Column(name = "alert_type", nullable = false)
    public String alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Severity severity;

    @Column(name = "position", columnDefinition = "geometry(Point, 4326)")
    public Point position;

    public String message;

    public boolean acknowledged = false;

    @Column(name = "acknowledged_by")
    public String acknowledgedBy;

    @Column(name = "acknowledged_at")
    public Instant acknowledgedAt;

    @Column(name = "created_at")
    public Instant createdAt = Instant.now();
}
