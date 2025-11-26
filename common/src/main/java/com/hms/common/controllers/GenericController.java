package com.hms.common.controllers;

import com.hms.common.dtos.Action;
import com.hms.common.dtos.ApiResponse;
import com.hms.common.dtos.PageResponse;
import com.hms.common.services.CrudService;
import io.github.perplexhub.rsql.RSQLJPASupport;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
public abstract class GenericController<E, ID, I, O> {

    protected final CrudService<E, ID, I, O> service;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<PageResponse<O>>> findAll(
            Pageable pageable,
            @RequestParam(value = "filter", required = false) @Nullable String filter,
            @RequestParam(value = "all", defaultValue = "false") boolean all) {
        Specification<E> specification = RSQLJPASupport.toSpecification(filter);
        if (all) {
            pageable = Pageable.unpaged(pageable.getSort());
        }
        return ResponseEntity.ok(ApiResponse.ok(service.findAll(pageable, specification)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<O>> findById(@PathVariable("id") ID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.findById(id)));
    }

    @PostMapping()
    public ResponseEntity<ApiResponse<O>> create(
            @Validated({Default.class, Action.Create.class}) @RequestBody I input) {
        return ResponseEntity.ok(ApiResponse.ok(service.create(input)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<O>> update(
            @PathVariable("id") ID id, @Validated({Default.class, Action.Update.class}) @RequestBody I input) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, input)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") ID id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<ApiResponse<Void>> deleteAll(@RequestParam("ids") List<ID> ids) {
        service.deleteAll(ids);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
