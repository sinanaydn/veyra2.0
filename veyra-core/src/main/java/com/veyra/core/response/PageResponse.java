package com.veyra.core.response;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Sayfalanmış liste endpoint'leri için yanıt sarmalayıcısı.
 * Spring Data'nın Page<T> nesnesinden kolayca oluşturulur.
 * Frontend her liste endpoint'inden bu şekli bekler.
 */
@Getter
public class PageResponse<T> {

    private final List<T> content;
    private final int pageNumber;
    private final int pageSize;
    private final long totalElements;
    private final int totalPages;
    private final boolean first;
    private final boolean last;

    // Spring Data Page<T>'den direkt oluşturma — DRY
    public PageResponse(Page<T> page) {
        this.content = page.getContent();
        this.pageNumber = page.getNumber();
        this.pageSize = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.first = page.isFirst();
        this.last = page.isLast();
    }
}
