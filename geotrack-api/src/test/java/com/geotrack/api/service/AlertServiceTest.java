package com.geotrack.api.service;

import com.geotrack.api.dto.AlertResponse;
import com.geotrack.api.model.AlertEntity;
import com.geotrack.api.repository.AlertRepository;
import com.geotrack.common.model.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlertService")
class AlertServiceTest {

    @Mock
    AlertRepository alertRepository;

    AlertService alertService;

    @BeforeEach
    void setUp() {
        alertService = new AlertService(alertRepository);
    }

    private AlertEntity makeAlert(UUID assetId, String type, Severity severity, boolean acked) {
        var entity = new AlertEntity();
        entity.id = UUID.randomUUID();
        entity.assetId = assetId;
        entity.alertType = type;
        entity.severity = severity;
        entity.message = "Test alert: " + type;
        entity.acknowledged = acked;
        entity.createdAt = Instant.now();
        return entity;
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("returns recent alerts as DTOs")
        void returnsRecent() {
            var assetId = UUID.randomUUID();
            var alerts = List.of(
                    makeAlert(assetId, "GEOFENCE_ENTRY", Severity.MEDIUM, false),
                    makeAlert(assetId, "SPEED_LIMIT", Severity.CRITICAL, true));
            when(alertRepository.findRecent(100)).thenReturn(alerts);

            List<AlertResponse> result = alertService.findAll();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).alertType()).isEqualTo("GEOFENCE_ENTRY");
            assertThat(result.get(1).severity()).isEqualTo(Severity.CRITICAL);
        }
    }

    @Nested
    @DisplayName("findUnacknowledged")
    class FindUnacknowledged {

        @Test
        @DisplayName("returns only unacknowledged alerts")
        void returnsUnacked() {
            var alert = makeAlert(UUID.randomUUID(), "GEOFENCE_EXIT", Severity.LOW, false);
            when(alertRepository.findUnacknowledged()).thenReturn(List.of(alert));

            List<AlertResponse> result = alertService.findUnacknowledged();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).acknowledged()).isFalse();
        }
    }

    @Nested
    @DisplayName("findByAssetId")
    class FindByAssetId {

        @Test
        @DisplayName("returns alerts for specific asset")
        void returnsByAsset() {
            var assetId = UUID.randomUUID();
            var other = UUID.randomUUID();
            var alert = makeAlert(assetId, "GEOFENCE_ENTRY", Severity.HIGH, false);
            when(alertRepository.findByAssetId(assetId)).thenReturn(List.of(alert));

            List<AlertResponse> result = alertService.findByAssetId(assetId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).assetId()).isEqualTo(assetId);
        }
    }

    @Nested
    @DisplayName("acknowledge")
    class Acknowledge {

        @Test
        @DisplayName("marks alert as acknowledged with username and timestamp")
        void acknowledgesAlert() {
            var alert = makeAlert(UUID.randomUUID(), "GEOFENCE_ENTRY", Severity.HIGH, false);
            when(alertRepository.findByIdOptional(alert.id)).thenReturn(Optional.of(alert));

            alertService.acknowledge(alert.id, "operator1");

            assertThat(alert.acknowledged).isTrue();
            assertThat(alert.acknowledgedBy).isEqualTo("operator1");
            assertThat(alert.acknowledgedAt).isNotNull();
            verify(alertRepository).persist(alert);
        }

        @Test
        @DisplayName("throws when alert not found")
        void throwsWhenNotFound() {
            var id = UUID.randomUUID();
            when(alertRepository.findByIdOptional(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> alertService.acknowledge(id, "user"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not found");
        }
    }
}
