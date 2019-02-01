package ru.inovus.ms.rdm.repositiory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import ru.inovus.ms.rdm.entity.RefBookEntity;

public interface RefBookRepository  extends
        JpaRepository<RefBookEntity, Integer>,
        QueryDslPredicateExecutor<RefBookEntity> {

    RefBookEntity findByCode(String code);

    boolean existsByCode(String code);

}
