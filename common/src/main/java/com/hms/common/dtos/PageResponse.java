package com.hms.common.dtos;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
public class PageResponse<T> {
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;
    private Integer numberOfElements;
    private List<T> content;

    public static <T> PageResponse<T> fromPage(Page<T> page) {
        PageResponse<T> response = new PageResponse<>();
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setNumberOfElements(page.getNumberOfElements());
        response.setContent(page.getContent());
        return response;
    }

    /**
     * Create an empty PageResponse.
     */
    public static <T> PageResponse<T> empty() {
        PageResponse<T> response = new PageResponse<>();
        response.setPage(0);
        response.setSize(0);
        response.setTotalElements(0L);
        response.setTotalPages(0);
        response.setNumberOfElements(0);
        response.setContent(List.of());
        return response;
    }
}
