package ru.inovus.ms.rdm.model;

import net.n2oapp.platform.jaxrs.RestCriteria;

public class AbstractCriteria extends RestCriteria {

    private static final int DAFAULT_PAGE = 1;
    private static final int DAFAULT_PAGE_SIZE = 10;
    private static final int MAX_SIZE = Integer.MAX_VALUE;


    public AbstractCriteria() {
        super(DAFAULT_PAGE, DAFAULT_PAGE_SIZE);
    }

    public void noPagination() {
        setPageSize(MAX_SIZE);
        setPageNumber(DAFAULT_PAGE);
    }
}
