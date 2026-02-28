package com.geotrack.api.service;

import com.geotrack.api.dto.AlertResponse;
import com.geotrack.api.mapper.AlertMapper;
import com.geotrack.api.model.AlertEntity;
import com.geotrack.api.repository.AlertRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AlertService {

    private final AlertRepository alertRepository;
    private final AlertMapper alertMapper;

    @Inject
    public AlertService(AlertRepository alertRepository, AlertMapper alertMapper) {
        this.alertRepository = alertRepository;
        this.alertMapper = alertMapper;
    }

    public List<AlertResponse> findAll() {
        return alertRepository.findRecent(100).stream()
                .map(alertMapper::toResponse)
                .toList();
    }

    public List<AlertResponse> findUnacknowledged() {
        return alertRepository.findUnacknowledged().stream()
                .map(alertMapper::toResponse)
                .toList();
    }

    public List<AlertResponse> findByAssetId(UUID assetId) {
        return alertRepository.findByAssetId(assetId).stream()
                .map(alertMapper::toResponse)
                .toList();
    }

    @Transactional
    public void acknowledge(UUID alertId, String username) {
        AlertEntity entity = alertRepository.findByIdOptional(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found: " + alertId));
        entity.acknowledged = true;
        entity.acknowledgedBy = username;
        entity.acknowledgedAt = Instant.now();
        alertRepository.persist(entity);
    }
}
