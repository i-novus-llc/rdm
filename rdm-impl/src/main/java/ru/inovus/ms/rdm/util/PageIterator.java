package ru.inovus.ms.rdm.util;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.inovus.ms.rdm.model.AbstractCriteria;

import java.util.Iterator;
import java.util.function.Function;

public class PageIterator<T, C extends AbstractCriteria> implements Iterator<Page<T>> {

    private Function<C, Page<T>> pageSource;

    private C criteria;

    private Page<T> nextPage;

    private int currentPage;

    public PageIterator(Function<C, Page<T>> pageSource, C criteria) {
        if(criteria.getSort() == null) {
            throw new IllegalArgumentException("sort cannot be null");
        }
        this.pageSource = pageSource;
        this.criteria = criteria;
        currentPage = criteria.getPageNumber() - 1;
    }

    @Override
    public boolean hasNext() {
        criteria.setPageNumber(currentPage + 1);
        nextPage = pageSource.apply(criteria);
        return !nextPage.isEmpty();
    }

    @Override
    public Page<T> next() {
        Page<T> result;
        if (nextPage != null) {
            result = nextPage;
            nextPage = null;
        } else {
            criteria.setPageNumber(currentPage + 1);
            result = pageSource.apply(criteria);
        }
        currentPage++;
        return result;
    }

}

