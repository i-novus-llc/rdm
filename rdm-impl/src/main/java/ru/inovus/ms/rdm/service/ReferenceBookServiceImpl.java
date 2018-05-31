package ru.inovus.ms.rdm.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import ru.inovus.ms.rdm.model.ReferenceBook;
import ru.inovus.ms.rdm.model.ReferenceBookCreateRequest;
import ru.inovus.ms.rdm.model.ReferenceBookCriteria;

import java.util.List;

/**
 * Created by tnurdinov on 24.05.2018.
 */
@Service
public class ReferenceBookServiceImpl implements ReferenceBookService {
    @Override
    public Page<ReferenceBook> search(ReferenceBookCriteria referenceBookCriteria) {
        return null;
    }

    @Override
    public ReferenceBook create(ReferenceBookCreateRequest referenceBookCreateRequest) {
        return null;
    }
}
