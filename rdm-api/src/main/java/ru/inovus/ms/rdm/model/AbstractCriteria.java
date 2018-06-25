package ru.inovus.ms.rdm.model;

import net.n2oapp.platform.jaxrs.RestCriteria;

public class AbstractCriteria extends RestCriteria {

    private static final int DAFAULT_PAGE = 1;
    private static final int DAFAULT_PAGE_SIZE = 10;


    public AbstractCriteria() {
        super(DAFAULT_PAGE, DAFAULT_PAGE_SIZE);
    }
}
