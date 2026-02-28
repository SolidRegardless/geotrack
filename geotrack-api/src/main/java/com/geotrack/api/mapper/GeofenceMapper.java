package com.geotrack.api.mapper;

import com.geotrack.api.dto.GeofenceResponse;
import com.geotrack.api.model.GeofenceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * MapStruct mapper for Geofence entity → DTO.
 */
@Mapper(componentModel = "default")
public interface GeofenceMapper {

    GeofenceMapper INSTANCE = Mappers.getMapper(GeofenceMapper.class);

    GeofenceResponse toResponse(GeofenceEntity entity);
}
