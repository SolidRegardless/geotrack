package com.geotrack.api.service;

import com.geotrack.api.dto.AssetResponse;
import com.geotrack.api.dto.CreateAssetRequest;
import com.geotrack.api.mapper.AssetMapper;
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
    private final AssetMapper assetMapper;

    @Inject
    public AssetService(AssetRepository assetRepository, AssetMapper assetMapper) {
        this.assetRepository = assetRepository;
        this.assetMapper = assetMapper;
    }

    public List<AssetResponse> findAll(AssetType type, AssetStatus status, int page, int size, String sort) {
        return assetRepository.findFiltered(type, status, page, size, sort)
                .stream()
                .map(assetMapper::toResponse)
                .toList();
    }

    public long countAll(AssetType type, AssetStatus status) {
        return assetRepository.countFiltered(type, status);
    }

    public AssetResponse findById(UUID id) {
        return assetRepository.findByIdOptional(id)
                .map(assetMapper::toResponse)
                .orElseThrow(() -> new AssetNotFoundException(id));
    }

    @Transactional
    public AssetResponse create(CreateAssetRequest request) {
        AssetEntity entity = assetMapper.toEntity(request);

        assetRepository.persist(entity);
        return assetMapper.toResponse(entity);
    }

    @Transactional
    public void delete(UUID id) {
        AssetEntity entity = assetRepository.findByIdOptional(id)
                .orElseThrow(() -> new AssetNotFoundException(id));
        entity.status = AssetStatus.DECOMMISSIONED;
        assetRepository.persist(entity);
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
