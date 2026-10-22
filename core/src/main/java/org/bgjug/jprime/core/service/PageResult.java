package org.bgjug.jprime.core.service;

import java.util.List;
import java.util.function.Function;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;

/** A page of results, independent of the web layer. Page numbers are zero based. */
public record PageResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResult<T> of(PanacheQuery<T> query, Page page) {
        PanacheQuery<T> paged = query.page(page);
        return new PageResult<>(paged.list(), page.index, page.size, paged.count(), paged.pageCount());
    }

    public static <T> PageResult<T> ofList(List<T> all, int page, int size) {
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        int pages = size == 0 ? 0 : (int) Math.ceil(all.size() / (double) size);
        return new PageResult<>(all.subList(from, to), page, size, all.size(), pages);
    }

    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(content.stream().map(mapper).toList(), page, size, totalElements, totalPages);
    }

    public boolean hasPrevious() {
        return page > 0;
    }

    public boolean hasNext() {
        return page + 1 < totalPages;
    }

    public int previousPage() {
        return Math.max(0, page - 1);
    }

    public int nextPage() {
        return page + 1;
    }
}
