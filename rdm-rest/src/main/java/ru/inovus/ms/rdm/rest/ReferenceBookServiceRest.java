package ru.inovus.ms.rdm.rest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Controller;
import ru.inovus.ms.rdm.model.ReferenceBook;
import ru.inovus.ms.rdm.model.ReferenceBookCreateRequest;
import ru.inovus.ms.rdm.model.ReferenceBookCriteria;
import ru.inovus.ms.rdm.service.ReferenceBookService;

import java.util.Collections;

@Controller
public class ReferenceBookServiceRest implements ReferenceBookService {

    @Override
    public Page<ReferenceBook> search(ReferenceBookCriteria referenceBookCriteria) {
        ReferenceBook o = new ReferenceBook();

        return new PageImpl<>(Collections.singletonList(o));
    }

    @Override
    public ReferenceBook create(ReferenceBookCreateRequest referenceBookCreateRequest) {
        throw new UnsupportedOperationException("Нет ещё");
    }

}
