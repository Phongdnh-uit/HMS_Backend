package com.hms.common.hooks;

import com.hms.common.dtos.PageResponse;

import java.util.Map;

/**
 * GenericPolicy
 *
 * @param <E> Entity type
 * @param <ID> Identifier type
 * @param <I> Input type
 * @param <O> Output type
 * 
 * All methods have default empty implementations.
 * Override only the methods you need for your specific business logic.
 */
public interface GenericHook<E, ID, I, O> {
    // ============================ VIEW ============================
    default void enrichFindAll(PageResponse<O> response) {}

    default void enrichFindById(O response) {}

    // ============================ CREATE ============================
    default void validateCreate(I input, Map<String, Object> context) {}

    default void enrichCreate(I input, E entity, Map<String, Object> context) {}

    default void afterCreate(E entity, O response, Map<String, Object> context) {}

    // ============================ UPDATE ============================

    default void validateUpdate(ID id, I input, E existingEntity, Map<String, Object> context) {}

    default void enrichUpdate(I input, E entity, Map<String, Object> context) {}

    default void afterUpdate(E entity, O response, Map<String, Object> context) {}

    // ============================ DELETE ============================
    default void validateDelete(ID id) {}

    default void afterDelete(ID id) {}

    default void validateBulkDelete(Iterable<ID> ids) {}

    default void afterBulkDelete(Iterable<ID> ids) {}
}
