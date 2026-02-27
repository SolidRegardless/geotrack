package com.geotrack.api.service;

import com.geotrack.api.dto.AssetResponse;
import com.geotrack.api.dto.CreateAssetRequest;
import com.geotrack.api.model.AssetEntity;
import com.geotrack.api.repository.AssetRepository;
import com.geotrack.common.model.AssetStatus;
import com.geotrack.common.model.AssetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AssetService.
 * Uses Mockito to isolate business logic from persistence.
 */
@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    AssetRepository assetRepository;

    @InjectMocks
    AssetService assetService;

    private AssetEntity sampleAsset;

    @BeforeEach
    void setUp() {
        sampleAsset = new AssetEntity();
        sampleAsset.id = UUID.randomUUID();
        sampleAsset.name = "Test Vehicle Alpha";
        sampleAsset.assetType = AssetType.VEHICLE;
        sampleAsset.status = AssetStatus.ACTIVE;
        sampleAsset.createdAt = Instant.now();
        sampleAsset.updatedAt = Instant.now();
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("Should return asset when found")
        void shouldReturnAssetWhenFound() {
            when(assetRepository.findByIdOptional(sampleAsset.id))
                    .thenReturn(Optional.of(sampleAsset));

            AssetResponse response = assetService.findById(sampleAsset.id);

            assertNotNull(response);
            assertEquals(sampleAsset.id, response.id());
            assertEquals("Test Vehicle Alpha", response.name());
            assertEquals(AssetType.VEHICLE, response.type());
            assertEquals(AssetStatus.ACTIVE, response.status());
        }

        @Test
        @DisplayName("Should throw when asset not found")
        void shouldThrowWhenNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(assetRepository.findByIdOptional(unknownId))
                    .thenReturn(Optional.empty());

            assertThrows(AssetService.AssetNotFoundException.class,
                    () -> assetService.findById(unknownId));
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("Should create and return new asset")
        void shouldCreateAsset() {
            var request = new CreateAssetRequest("New Drone", AssetType.DRONE, null);

            doAnswer(invocation -> {
                AssetEntity entity = invocation.getArgument(0);
                entity.id = UUID.randomUUID();
                return null;
            }).when(assetRepository).persist(any(AssetEntity.class));

            AssetResponse response = assetService.create(request);

            assertNotNull(response);
            assertEquals("New Drone", response.name());
            assertEquals(AssetType.DRONE, response.type());
            assertEquals(AssetStatus.ACTIVE, response.status());
            verify(assetRepository).persist(any(AssetEntity.class));
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("Should return filtered asset list")
        void shouldReturnFilteredList() {
            when(assetRepository.findFiltered(AssetType.VEHICLE, null, 0, 20, "name"))
                    .thenReturn(List.of(sampleAsset));

            List<AssetResponse> results = assetService.findAll(
                    AssetType.VEHICLE, null, 0, 20, "name");

            assertEquals(1, results.size());
            assertEquals("Test Vehicle Alpha", results.getFirst().name());
        }

        @Test
        @DisplayName("Should return empty list when no matches")
        void shouldReturnEmptyWhenNoMatches() {
            when(assetRepository.findFiltered(AssetType.AIRCRAFT, null, 0, 20, "name"))
                    .thenReturn(List.of());

            List<AssetResponse> results = assetService.findAll(
                    AssetType.AIRCRAFT, null, 0, 20, "name");

            assertTrue(results.isEmpty());
        }
    }

    @Nested
    @DisplayName("delete (soft delete)")
    class Delete {

        @Test
        @DisplayName("Should set status to DECOMMISSIONED")
        void shouldSoftDelete() {
            when(assetRepository.findByIdOptional(sampleAsset.id))
                    .thenReturn(Optional.of(sampleAsset));

            assetService.delete(sampleAsset.id);

            assertEquals(AssetStatus.DECOMMISSIONED, sampleAsset.status);
            verify(assetRepository).persist(sampleAsset);
        }

        @Test
        @DisplayName("Should throw when asset to delete not found")
        void shouldThrowWhenNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(assetRepository.findByIdOptional(unknownId))
                    .thenReturn(Optional.empty());

            assertThrows(AssetService.AssetNotFoundException.class,
                    () -> assetService.delete(unknownId));
        }
    }
}
