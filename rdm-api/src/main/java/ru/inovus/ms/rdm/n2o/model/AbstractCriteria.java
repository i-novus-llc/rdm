package ru.inovus.ms.rdm.n2o.model;

import net.n2oapp.platform.jaxrs.RestCriteria;

public class AbstractCriteria extends RestCriteria {

    private static final int DEFAULT_PAGE_NUMBER = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int NO_PAGINATION_SIZE = Integer.MAX_VALUE;

    public AbstractCriteria() {
        super(DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE);
    }

    public void noPagination() {
        setPageSize(NO_PAGINATION_SIZE);
        setPageNumber(DEFAULT_PAGE_NUMBER);
    }
}
