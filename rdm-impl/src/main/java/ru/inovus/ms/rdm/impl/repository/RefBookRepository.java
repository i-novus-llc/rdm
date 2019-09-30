package ru.inovus.ms.rdm.impl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import ru.inovus.ms.rdm.impl.entity.RefBookEntity;

public interface RefBookRepository extends
        JpaRepository<RefBookEntity, Integer>,
        QuerydslPredicateExecutor<RefBookEntity> {

    RefBookEntity findByCode(String code);

    boolean existsByCode(String code);

}