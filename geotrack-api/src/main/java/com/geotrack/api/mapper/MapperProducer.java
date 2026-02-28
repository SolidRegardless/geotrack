package com.geotrack.api.mapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.mapstruct.factory.Mappers;

/**
 * CDI producer for MapStruct-generated mapper instances.
 * <p>
 * Quarkus ArC performs bean discovery at build time via Jandex indexing.
 * Generated MapStruct implementation classes may not be included in the
 * application index, so we explicitly produce them as CDI beans.
 * </p>
 */
@ApplicationScoped
public class MapperProducer {

    @Produces
    @ApplicationScoped
    public AssetMapper assetMapper() {
        return Mappers.getMapper(AssetMapper.class);
    }

    @Produces
    @ApplicationScoped
    public PositionMapper positionMapper() {
        return Mappers.getMapper(PositionMapper.class);
    }

    @Produces
    @ApplicationScoped
    public AlertMapper alertMapper() {
        return Mappers.getMapper(AlertMapper.class);
    }

    @Produces
    @ApplicationScoped
    public GeofenceMapper geofenceMapper() {
        return Mappers.getMapper(GeofenceMapper.class);
    }
}
