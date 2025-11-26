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
}
