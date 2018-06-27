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
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tnurdinov on 24.05.2018.
 */
@Service
@Qualifier("impl")
public class RefBookServiceImpl implements RefBookService {

    private RefBookVersionRepository repository;

    @Autowired
    public RefBookServiceImpl(RefBookVersionRepository repository) {
        this.repository = repository;
    }


    @Override
    public Page<RefBook> search(RefBookCriteria refBookCriteria) {

        List<RefBook> res = new ArrayList<>();

        Predicate predicate = null;
        if (!StringUtils.isEmpty(refBookCriteria.getCode()))
            predicate = QRefBookVersionEntity.refBookVersionEntity.refBook.code.equalsIgnoreCase(refBookCriteria.getCode());


        Iterable<RefBookVersionEntity> list;
        if (predicate == null)
            list = repository.findAll();
        else
            list = repository.findAll(predicate);
        for (RefBookVersionEntity refBookVersionEntity : list) {
            RefBook refBook = new RefBook();
            refBook.setCode(refBookVersionEntity.getRefBook().getCode());
            res.add(refBook);
        }

        return new PageImpl<>(res);
    }

    @Override
    public RefBook create(RefBookCreateRequest refBookCreateRequest) {
        RefBookVersionEntity rbv = new RefBookVersionEntity();
        rbv.setStatus(RefBookVersionStatus.DRAFT);

        RefBookEntity rb =new RefBookEntity();
        rb.setRemovable(false);
        rb.setArchived(false);
        rb.setCode(refBookCreateRequest.getCode());
        rbv.setRefBook(rb);


        rbv.setFullName("11");
        rbv.setShortName("1");

        rbv = repository.save(rbv);

        RefBook refBook = new RefBook();
        refBook.setCode(rbv.getRefBook().getCode());
        return refBook;
    }

    @Override
    public Page<RefBookVersion> getVersions(String refBookId) {
        throw new UnsupportedOperationException();
    }
}
