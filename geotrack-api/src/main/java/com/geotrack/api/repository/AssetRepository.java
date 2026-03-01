package com.geotrack.api.repository;

import com.geotrack.api.model.AssetEntity;
import com.geotrack.common.model.AssetStatus;
import com.geotrack.common.model.AssetType;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for asset CRUD operations using Panache.
 */
@ApplicationScoped
public class AssetRepository implements PanacheRepositoryBase<AssetEntity, UUID> {

    public List<AssetEntity> findFiltered(AssetType type, AssetStatus status, int page, int size, String sortField) {
        StringBuilder query = new StringBuilder("1=1");
        java.util.Map<String, Object> params = new java.util.HashMap<>();

        if (type != null) {
            query.append(" AND assetType = :type");
            params.put("type", type);
        }
        if (status != null) {
            query.append(" AND status = :status");
            params.put("status", status);
        }

        return find(query.toString(), Sort.ascending(sortField), params)
                .page(Page.of(page, size))
                .list();
    }

    public long countFiltered(AssetType type, AssetStatus status) {
        StringBuilder query = new StringBuilder("1=1");
        java.util.Map<String, Object> params = new java.util.HashMap<>();

        if (type != null) {
            query.append(" AND assetType = :type");
            params.put("type", type);
        }
        if (status != null) {
            query.append(" AND status = :status");
            params.put("status", status);
        }

        return count(query.toString(), params);
    }

    @Override
    public Optional<AssetEntity> findByIdOptional(UUID id) {
        return find("id", id).firstResultOptional();
    }
}
