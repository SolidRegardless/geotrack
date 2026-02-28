package com.geotrack.api.mapper;

import com.geotrack.api.dto.PositionResponse;
import com.geotrack.api.model.PositionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/**
 * MapStruct mapper for Position entity → DTO.
 * Handles JTS Point → lat/lon extraction via custom methods.
 */
@Mapper(componentModel = "cdi")
public interface PositionMapper {

    PositionMapper INSTANCE = Mappers.getMapper(PositionMapper.class);

    @Mapping(target = "assetId", source = "entity", qualifiedByName = "extractAssetId")
    @Mapping(target = "latitude", expression = "java(entity.getLatitude())")
    @Mapping(target = "longitude", expression = "java(entity.getLongitude())")
    @Mapping(target = "altitude", expression = "java(entity.altitude != null ? entity.altitude : 0)")
    @Mapping(target = "speed", expression = "java(entity.speed != null ? entity.speed : 0)")
    @Mapping(target = "heading", expression = "java(entity.heading != null ? entity.heading : 0)")
    PositionResponse toResponse(PositionEntity entity);

    /**
     * Use source identifier if available, fall back to UUID.
     */
    @Named("extractAssetId")
    default String extractAssetId(PositionEntity entity) {
        return entity.source != null && !entity.source.isBlank()
                ? entity.source : entity.assetId.toString();
    }
}
