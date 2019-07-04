package ru.inovus.ms.rdm.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Objects;

@ApiModel("Страница после фильтрации содержания")
@JsonIgnoreProperties(ignoreUnknown = true)
public class FilteredContent<T> {

    @ApiModelProperty("Страница с отфильтрованным содержимым")
    private Page<T> page;

    @ApiModelProperty("Размер содержимого исходной страницы")
    private long originalSize;

    @SuppressWarnings("unused")
    public FilteredContent() {
    }

    @SuppressWarnings("WeakerAccess")
    public FilteredContent(Page<T> page, long originalSize) {
        this.page = page;
        this.originalSize = originalSize;
    }

    public FilteredContent(List<T> content, Pageable pageable, long filteredTotal, long originalSize) {
        this(new PageImpl<>(content, pageable, filteredTotal), originalSize);
    }

    public Page<T> getPage() {
        return page;
    }

    public void setPage(Page<T> page) {
        this.page = page;
    }

    public long getOriginalSize() {
        return originalSize;
    }

    public void setOriginalSize(long total) {
        this.originalSize = total;
    }

    public boolean isEmpty() {
        return (getOriginalSize() <= 0L);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FilteredContent<?> that = (FilteredContent<?>) o;

        return Objects.equals(page, that.page)
                && Objects.equals(originalSize, that.originalSize);
    }

    @Override
    public int hashCode() {
        return Objects.hash(page, originalSize);
    }
}
