package com.geotrack.api.service;

import com.geotrack.api.dto.AssetResponse;
import com.geotrack.api.dto.CreateAssetRequest;
import com.geotrack.api.model.AssetEntity;
import com.geotrack.api.repository.AssetRepository;
import com.geotrack.common.model.AssetStatus;
import com.geotrack.common.model.AssetType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Business logic for asset management.
 */
@ApplicationScoped
public class AssetService {

    private final AssetRepository assetRepository;

    @Inject
    public AssetService(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    public List<AssetResponse> findAll(AssetType type, AssetStatus status, int page, int size, String sort) {
        return assetRepository.findFiltered(type, status, page, size, sort)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public long countAll(AssetType type, AssetStatus status) {
        return assetRepository.countFiltered(type, status);
    }

    public AssetResponse findById(UUID id) {
        return assetRepository.findByIdOptional(id)
                .map(this::toResponse)
                .orElseThrow(() -> new AssetNotFoundException(id));
    }

    @Transactional
    public AssetResponse create(CreateAssetRequest request) {
        AssetEntity entity = new AssetEntity();
        entity.name = request.name();
        entity.assetType = request.type();
        entity.status = AssetStatus.ACTIVE;
        entity.metadata = request.metadata();

        assetRepository.persist(entity);
        return toResponse(entity);
    }

    @Transactional
    public void delete(UUID id) {
        AssetEntity entity = assetRepository.findByIdOptional(id)
                .orElseThrow(() -> new AssetNotFoundException(id));
        entity.status = AssetStatus.DECOMMISSIONED;
        assetRepository.persist(entity);
    }

    private AssetResponse toResponse(AssetEntity entity) {
        return new AssetResponse(
                entity.id,
                entity.name,
                entity.assetType,
                entity.status,
                entity.createdAt,
                entity.updatedAt
        );
    }

    /**
     * Domain exception for asset not found.
     */
    public static class AssetNotFoundException extends RuntimeException {
        private final UUID assetId;

        public AssetNotFoundException(UUID assetId) {
            super("Asset not found: " + assetId);
            this.assetId = assetId;
        }

        public UUID getAssetId() {
            return assetId;
        }
    }
}
