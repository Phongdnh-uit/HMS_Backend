package com.hms.common.services;

import com.hms.common.dtos.PageResponse;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.common.hooks.GenericHook;
import com.hms.common.mappers.GenericMapper;
import com.hms.common.repositories.SimpleRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.HashMap;
import java.util.Map;

public interface CrudService<E, ID, I, O> {

    PageResponse<O> findAll(Pageable pageable, Specification<E> specification);

    O findById(ID id);

    O create(I input);

    O update(ID id, I input);

    void delete(ID id);

    void deleteAll(Iterable<ID> ids);

    default PageResponse<O> defaultFindAll(
            Pageable pageable,
            Specification<E> specification,
            GenericMapper<E, I, O> mapper,
            SimpleRepository<E, ID> repository,
            GenericHook<E, ID, I, O> hook) {
        PageResponse<O> response =
                PageResponse.fromPage(
                        repository.findAll(specification, pageable).map(mapper::entityToResponse));
        hook.enrichFindAll(response);
        return response;
    }

    default O defaultFindById(
            ID id,
            GenericMapper<E, I, O> mapper,
            SimpleRepository<E, ID> repository,
            GenericHook<E, ID, I, O> hook) {
        E entity =
                repository.findById(id).orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        O response = mapper.entityToResponse(entity);
        hook.enrichFindById(response);
        return response;
    }

    default O defaultCreate(
            I input,
            GenericMapper<E, I, O> mapper,
            SimpleRepository<E, ID> repository,
            GenericHook<E, ID, I, O> hook) {
        Map<String, Object> context = new HashMap<>();
        hook.validateCreate(input, context);
        E entity = mapper.requestToEntity(input);
        hook.enrichCreate(input, entity, context);
        E savedEntity = repository.save(entity);
        O response = mapper.entityToResponse(savedEntity);
        hook.afterCreate(entity, response, context);
        return response;
    }

    default O defaultCreate(
            I input, GenericMapper<E, I, O> mapper, SimpleRepository<E, ID> repository) {
        E entity = mapper.requestToEntity(input);
        E savedEntity = repository.save(entity);
        O response = mapper.entityToResponse(savedEntity);
        return response;
    }

    default O defaultUpdate(
            ID id,
            I input,
            GenericMapper<E, I, O> mapper,
            SimpleRepository<E, ID> repository,
            GenericHook<E, ID, I, O> hook) {
        E entity =
                repository.findById(id).orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        Map<String, Object> context = new HashMap<>();
        hook.validateUpdate(id, input, entity, context);
        mapper.partialUpdate(input, entity);
        hook.enrichUpdate(input, entity, context);
        entity = repository.save(entity);
        O response = mapper.entityToResponse(entity);
        hook.afterUpdate(entity, response, context);
        return response;
    }

    default O defaultUpdate(
            ID id, I input, GenericMapper<E, I, O> mapper, SimpleRepository<E, ID> repository) {
        E entity =
                repository.findById(id).orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        mapper.partialUpdate(input, entity);
        entity = repository.save(entity);
        O response = mapper.entityToResponse(entity);
        return response;
    }

    default void defaultDelete(
            ID id, SimpleRepository<E, ID> repository, GenericHook<E, ID, I, O> hook) {
        hook.validateDelete(id);
        repository.deleteById(id);
        hook.afterDelete(id);
    }

    default void defaultDelete(ID id, SimpleRepository<E, ID> repository) {
        repository.deleteById(id);
    }

    default void defaultDeleteAll(
            Iterable<ID> ids, SimpleRepository<E, ID> repository, GenericHook<E, ID, I, O> policy) {
        policy.validateBulkDelete(ids);
        repository.deleteAllByIdInBatch(ids);
        policy.afterBulkDelete(ids);
    }

    default void defaultDeleteAll(Iterable<ID> ids, SimpleRepository<E, ID> repository) {
        repository.deleteAllByIdInBatch(ids);
    }
}