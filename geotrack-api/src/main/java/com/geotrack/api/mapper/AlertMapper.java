package com.geotrack.api.mapper;

import com.geotrack.api.dto.AlertResponse;
import com.geotrack.api.model.AlertEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * MapStruct mapper for Alert entity → DTO.
 */
@Mapper(componentModel = "cdi")
public interface AlertMapper {

    AlertMapper INSTANCE = Mappers.getMapper(AlertMapper.class);

    AlertResponse toResponse(AlertEntity entity);
}
