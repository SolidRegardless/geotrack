package com.geotrack.api.model;

import com.geotrack.common.model.AssetStatus;
import com.geotrack.common.model.AssetType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a tracked asset.
 */
@Entity
@Table(name = "assets")
public class AssetEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(nullable = false)
    public String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false)
    public AssetType assetType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public AssetStatus status = AssetStatus.ACTIVE;

    @Column(columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    public String metadata;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
