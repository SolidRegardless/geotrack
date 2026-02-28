package com.geotrack.api.service;

import com.geotrack.api.dto.CreateGeofenceRequest;
import com.geotrack.api.dto.GeofenceResponse;
import com.geotrack.api.mapper.GeofenceMapper;
import com.geotrack.api.model.GeofenceEntity;
import com.geotrack.api.repository.GeofenceRepository;
import com.geotrack.common.model.FenceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GeofenceService")
class GeofenceServiceTest {

    @Mock
    GeofenceRepository geofenceRepository;

    GeofenceService geofenceService;

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    private final GeofenceMapper geofenceMapper = GeofenceMapper.INSTANCE;

    @BeforeEach
    void setUp() {
        geofenceService = new GeofenceService(geofenceRepository, geofenceMapper);
    }

    private GeofenceEntity makeEntity(String name, FenceType type, boolean active) {
        var entity = new GeofenceEntity();
        entity.id = UUID.randomUUID();
        entity.name = name;
        entity.description = "Test geofence";
        entity.fenceType = type;
        entity.active = active;
        entity.alertOnEnter = true;
        entity.alertOnExit = true;
        entity.createdAt = Instant.now();
        return entity;
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("returns all geofences mapped to DTOs")
        void returnsAll() {
            var entities = List.of(
                    makeEntity("Zone A", FenceType.INCLUSION, true),
                    makeEntity("Zone B", FenceType.EXCLUSION, false));
            when(geofenceRepository.findAllGeofences()).thenReturn(entities);

            List<GeofenceResponse> result = geofenceService.findAll();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("Zone A");
            assertThat(result.get(1).name()).isEqualTo("Zone B");
        }

        @Test
        @DisplayName("returns empty list when no geofences exist")
        void returnsEmpty() {
            when(geofenceRepository.findAllGeofences()).thenReturn(List.of());
            assertThat(geofenceService.findAll()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findActive")
    class FindActive {

        @Test
        @DisplayName("returns only active geofences")
        void returnsActive() {
            var active = makeEntity("Active", FenceType.INCLUSION, true);
            when(geofenceRepository.findActive()).thenReturn(List.of(active));

            List<GeofenceResponse> result = geofenceService.findActive();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).active()).isTrue();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("returns geofence when found")
        void returnsWhenFound() {
            var entity = makeEntity("Found", FenceType.INCLUSION, true);
            when(geofenceRepository.findByIdOptional(entity.id)).thenReturn(Optional.of(entity));

            GeofenceResponse result = geofenceService.findById(entity.id);

            assertThat(result.name()).isEqualTo("Found");
            assertThat(result.id()).isEqualTo(entity.id);
        }

        @Test
        @DisplayName("throws when not found")
        void throwsWhenNotFound() {
            var id = UUID.randomUUID();
            when(geofenceRepository.findByIdOptional(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> geofenceService.findById(id))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("creates geofence from request with polygon coordinates")
        void createsFromRequest() {
            // Newcastle city centre triangle
            var coords = List.of(
                    new double[]{-1.615, 54.975},
                    new double[]{-1.610, 54.970},
                    new double[]{-1.605, 54.975},
                    new double[]{-1.615, 54.975});

            var request = new CreateGeofenceRequest(
                    "Newcastle Centre", "City centre zone",
                    FenceType.INCLUSION, coords, true, true);

            doAnswer(inv -> {
                GeofenceEntity e = inv.getArgument(0);
                e.id = UUID.randomUUID();
                return null;
            }).when(geofenceRepository).persist(any(GeofenceEntity.class));

            GeofenceResponse result = geofenceService.create(request);

            assertThat(result.name()).isEqualTo("Newcastle Centre");
            assertThat(result.fenceType()).isEqualTo(FenceType.INCLUSION);

            ArgumentCaptor<GeofenceEntity> captor = ArgumentCaptor.forClass(GeofenceEntity.class);
            verify(geofenceRepository).persist(captor.capture());
            assertThat(captor.getValue().geometry).isNotNull();
            assertThat(captor.getValue().geometry).isInstanceOf(Polygon.class);
        }
    }

    @Nested
    @DisplayName("delete (soft)")
    class Delete {

        @Test
        @DisplayName("deactivates geofence instead of hard delete")
        void softDeletes() {
            var entity = makeEntity("Doomed", FenceType.EXCLUSION, true);
            when(geofenceRepository.findByIdOptional(entity.id)).thenReturn(Optional.of(entity));

            geofenceService.delete(entity.id);

            assertThat(entity.active).isFalse();
            verify(geofenceRepository).persist(entity);
        }

        @Test
        @DisplayName("throws when geofence not found")
        void throwsWhenNotFound() {
            var id = UUID.randomUUID();
            when(geofenceRepository.findByIdOptional(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> geofenceService.delete(id))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}
