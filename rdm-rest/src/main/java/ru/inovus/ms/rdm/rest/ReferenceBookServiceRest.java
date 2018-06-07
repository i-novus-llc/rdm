package ru.inovus.ms.rdm.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import ru.inovus.ms.rdm.model.ReferenceBook;
import ru.inovus.ms.rdm.model.ReferenceBookCreateRequest;
import ru.inovus.ms.rdm.model.ReferenceBookCriteria;
import ru.inovus.ms.rdm.service.ReferenceBookService;

@Controller
@Qualifier("rest")
public class ReferenceBookServiceRest implements ReferenceBookService {

    private final ReferenceBookService refBookService;

    @Autowired
    public ReferenceBookServiceRest(@Qualifier("impl") ReferenceBookService refBookService) {
        this.refBookService = refBookService;
    }

    @Override
    public Page<ReferenceBook> search(ReferenceBookCriteria referenceBookCriteria) {
        return refBookService.search(referenceBookCriteria);
    }

    @Override
    public ReferenceBook create(ReferenceBookCreateRequest referenceBookCreateRequest) {
        return refBookService.create(referenceBookCreateRequest);
    }

}
