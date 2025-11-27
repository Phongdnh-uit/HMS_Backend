package com.hms.common.services;

import com.hms.common.dtos.PageResponse;
import com.hms.common.hooks.GenericHook;
import com.hms.common.mappers.GenericMapper;
import com.hms.common.repositories.SimpleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/**
 * @param <E> entity type
 * @param <ID> entity id type
 * @param <I>  input dto type
 * @param <O>  output dto type
 */
@RequiredArgsConstructor
@Scope("prototype")
public class GenericService<E, ID, I, O> implements CrudService<E, ID, I, O> {

    private final SimpleRepository<E, ID> repository;
    private final GenericMapper<E, I, O> mapper;
    private final GenericHook<E, ID, I, O> hook;

    @Override
    public PageResponse<O> findAll(Pageable pageable, Specification<E> specification) {
        return defaultFindAll(pageable, specification, mapper, repository, hook);
    }

    @Override
    public O findById(ID id) {
        return defaultFindById(id, mapper, repository, hook);
    }

    @Override
    public O create(I input) {
        return defaultCreate(input, mapper, repository, hook);
    }

    @Override
    public O update(ID id, I input) {
        return defaultUpdate(id, input, mapper, repository, hook);
    }

    @Override
    public void delete(ID id) {
        defaultDelete(id, repository, hook);
    }

    @Override
    public void deleteAll(Iterable<ID> ids) {
        defaultDeleteAll(ids, repository, hook);
    }
}
