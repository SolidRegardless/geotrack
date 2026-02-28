package com.geotrack.api.mapper;

import com.geotrack.api.dto.AssetResponse;
import com.geotrack.api.dto.CreateAssetRequest;
import com.geotrack.api.model.AssetEntity;
import com.geotrack.common.model.AssetStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * MapStruct mapper for Asset entity ↔ DTO conversions.
 * Compile-time code generation — no reflection overhead.
 */
@Mapper(componentModel = "cdi")
public interface AssetMapper {

    AssetMapper INSTANCE = Mappers.getMapper(AssetMapper.class);

    @Mapping(source = "assetType", target = "type")
    AssetResponse toResponse(AssetEntity entity);

    @Mapping(source = "type", target = "assetType")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    AssetEntity toEntity(CreateAssetRequest request);
}
