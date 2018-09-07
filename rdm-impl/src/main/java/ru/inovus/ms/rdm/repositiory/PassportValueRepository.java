package ru.inovus.ms.rdm.repositiory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import ru.inovus.ms.rdm.entity.PassportValueEntity;
import ru.inovus.ms.rdm.entity.RefBookEntity;

import java.util.List;

public interface PassportValueRepository extends
        JpaRepository<PassportValueEntity, Integer>,
        QueryDslPredicateExecutor<RefBookEntity> {
        List<PassportValueEntity> findAllByVersionIdOrderByAttributePosition(Integer versionId);}
