package com.geotrack.api.repository;

import com.geotrack.api.model.AlertEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

/**
 * Repository for alert queries.
 */
@ApplicationScoped
public class AlertRepository implements PanacheRepositoryBase<AlertEntity, UUID> {

    public List<AlertEntity> findUnacknowledged() {
        return find("acknowledged = false ORDER BY createdAt DESC").list();
    }

    public List<AlertEntity> findByAssetId(UUID assetId) {
        return find("assetId = ?1 ORDER BY createdAt DESC", assetId).list();
    }

    public List<AlertEntity> findRecent(int limit) {
        return find("ORDER BY createdAt DESC").page(0, limit).list();
    }
}
