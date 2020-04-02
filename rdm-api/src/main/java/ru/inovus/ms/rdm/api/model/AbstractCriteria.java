package ru.inovus.ms.rdm.api.model;

import net.n2oapp.platform.jaxrs.RestCriteria;
import org.springframework.data.domain.Sort;

import java.util.Collections;
import java.util.List;

public class AbstractCriteria extends RestCriteria {

    private static final int DEFAULT_PAGE_NUMBER = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int NO_PAGINATION_SIZE = Integer.MAX_VALUE;

    public AbstractCriteria() {
        super(DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE);
    }

    @Override
    protected List<Sort.Order> getDefaultOrders() {
        return Collections.emptyList();
    }

    public void noPagination() {
        setPageSize(NO_PAGINATION_SIZE);
        setPageNumber(DEFAULT_PAGE_NUMBER);
    }
}
