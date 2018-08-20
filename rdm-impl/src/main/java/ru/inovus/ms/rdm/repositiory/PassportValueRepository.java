package ru.inovus.ms.rdm.repositiory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import ru.inovus.ms.rdm.entity.PassportValueEntity;
import ru.inovus.ms.rdm.entity.RefBookEntity;

public interface PassportValueRepository extends
        JpaRepository<PassportValueEntity, Integer>,
        QueryDslPredicateExecutor<RefBookEntity> {
}
