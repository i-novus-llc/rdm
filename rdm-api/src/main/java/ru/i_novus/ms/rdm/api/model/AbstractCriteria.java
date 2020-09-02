package ru.i_novus.ms.rdm.api.model;

import net.n2oapp.platform.jaxrs.RestCriteria;
import org.springframework.data.domain.Sort;

import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class AbstractCriteria extends RestCriteria {

    private static final int DEFAULT_PAGE_NUMBER = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int NO_PAGINATION_SIZE = Integer.MAX_VALUE;

    public AbstractCriteria() {
        this(DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE);
    }

    public AbstractCriteria(int pageNumber, int pageSize) {
        super(pageNumber, pageSize);
    }

    @Override
    protected List<Sort.Order> getDefaultOrders() {
        return Collections.emptyList();
    }

    public void noPagination() {
        setPageSize(NO_PAGINATION_SIZE);
        setPageNumber(DEFAULT_PAGE_NUMBER);
    }

    public List<Sort.Order> getOrders() {
        return getSort().get().collect(toList());
    }
}
