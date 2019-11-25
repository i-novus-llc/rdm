package ru.inovus.ms.rdm.impl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.impl.entity.RefBookOperationEntity;

public interface RefBookOperationRepository extends
        JpaRepository<RefBookOperationEntity, Integer>,
        QuerydslPredicateExecutor<RefBookOperationEntity> {

        RefBookOperationEntity findByRefBookId(Integer refBookId);

        void deleteByRefBookId(Integer refBookId);

        @Transactional
        Integer deleteAllByInstanceId(String instanceId);
}