package ru.inovus.ms.rdm.service;

import com.querydsl.core.types.Predicate;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import ru.inovus.ms.rdm.entity.QRefBookVersionEntity;
import ru.inovus.ms.rdm.entity.RefBookEntity;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.model.RefBookVersionStatus;
import ru.inovus.ms.rdm.model.ReferenceBook;
import ru.inovus.ms.rdm.model.ReferenceBookCreateRequest;
import ru.inovus.ms.rdm.model.ReferenceBookCriteria;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tnurdinov on 24.05.2018.
 */
@Service
@Qualifier("impl")
public class ReferenceBookServiceImpl implements ReferenceBookService {

    private RefBookVersionRepository repository;

    @Autowired
    public ReferenceBookServiceImpl(RefBookVersionRepository repository) {
        this.repository = repository;
    }


    @Override
    public Page<ReferenceBook> search(ReferenceBookCriteria referenceBookCriteria) {

        List<ReferenceBook> res = new ArrayList<>();

        Predicate predicate = null;
        if (!StringUtils.isEmpty(referenceBookCriteria.getCode()))
            predicate = QRefBookVersionEntity.refBookVersionEntity.refBook.code.equalsIgnoreCase(referenceBookCriteria.getCode());


        Iterable<RefBookVersionEntity> list;
        if (predicate == null)
            list = repository.findAll();
        else
            list = repository.findAll(predicate);
        for (RefBookVersionEntity refBookVersionEntity : list) {
            ReferenceBook referenceBook = new ReferenceBook();
            referenceBook.setCode(refBookVersionEntity.getRefBook().getCode());
            res.add(referenceBook);
        }

        return new PageImpl<>(res);
    }

    @Override
    public ReferenceBook create(ReferenceBookCreateRequest referenceBookCreateRequest) {
        RefBookVersionEntity rbv = new RefBookVersionEntity();
        rbv.setStatus(RefBookVersionStatus.DRAFT);

        RefBookEntity rb =new RefBookEntity();
        rb.setRemovable(false);
        rb.setArchived(false);
        rb.setCode(referenceBookCreateRequest.getCode());
        rbv.setRefBook(rb);


        rbv.setFullName("11");
        rbv.setShortName("1");

        rbv = repository.save(rbv);

        ReferenceBook referenceBook = new ReferenceBook();
        referenceBook.setCode(rbv.getRefBook().getCode());
        return referenceBook;
    }
}
