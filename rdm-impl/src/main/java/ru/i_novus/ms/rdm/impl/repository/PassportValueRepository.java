package ru.i_novus.ms.rdm.impl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import ru.i_novus.ms.rdm.impl.entity.PassportValueEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;

import java.util.List;

public interface PassportValueRepository extends
        JpaRepository<PassportValueEntity, Integer>,
        QuerydslPredicateExecutor<RefBookEntity> {

    List<PassportValueEntity> findAllByVersionIdOrderByAttributePosition(Integer versionId);
}
