package ru.inovus.ms.rdm.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import ru.inovus.ms.rdm.model.RefBook;
import ru.inovus.ms.rdm.model.RefBookCreateRequest;
import ru.inovus.ms.rdm.model.RefBookCriteria;
import ru.inovus.ms.rdm.service.RefBookService;

@Controller
@Qualifier("rest")
public class RefBookServiceRest implements RefBookService {

    private final RefBookService refBookService;

    @Autowired
    public RefBookServiceRest(@Qualifier("impl") RefBookService refBookService) {
        this.refBookService = refBookService;
    }

    @Override
    public Page<RefBook> search(RefBookCriteria refBookCriteria) {
        return refBookService.search(refBookCriteria);
    }

    @Override
    public RefBook create(RefBookCreateRequest refBookCreateRequest) {
        return refBookService.create(refBookCreateRequest);
    }

}
