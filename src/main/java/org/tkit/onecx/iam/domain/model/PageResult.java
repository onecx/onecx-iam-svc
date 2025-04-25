package org.tkit.onecx.iam.domain.model;

import java.util.List;

import lombok.Getter;

@Getter
public class PageResult<T> {

    private final long totalElements;

    private final long number;

    private final long size;

    private final long totalPages;

    private final List<T> stream;

    public PageResult(long totalElements, List<T> stream, Page page) {
        this.totalElements = totalElements;
        this.stream = stream;
        this.number = page.number();
        this.size = page.size();
        this.totalPages = (totalElements + size - 1) / size;
    }

    @Override
    public String toString() {
        return "PageResult{" +
                "c=" + totalElements +
                ",n=" + number +
                ",s=" + size +
                '}';
    }
}
