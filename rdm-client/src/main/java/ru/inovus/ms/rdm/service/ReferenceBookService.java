package ru.inovus.ms.rdm.service;

import java.util.List;

public interface ReferenceBookService {

    List<ru.inovus.ms.rdm.model.ReferenceBook> search(ru.inovus.ms.rdm.model.ReferenceBookCriteria referenceBookCriteria);

    ru.inovus.ms.rdm.model.ReferenceBook create(ru.inovus.ms.rdm.model.ReferenceBookCreateRequest referenceBookCreateRequest);

}
