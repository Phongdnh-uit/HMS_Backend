package com.hms.common.mappers;

import org.mapstruct.MappingTarget;

public interface GenericMapper<E, I, O> {
    E requestToEntity(I request);

    O entityToResponse(E entity);

    void partialUpdate(I request, @MappingTarget E entity);
}
