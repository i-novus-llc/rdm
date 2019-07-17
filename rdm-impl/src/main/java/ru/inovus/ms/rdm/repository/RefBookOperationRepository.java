package ru.inovus.ms.rdm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.entity.RefBookOperationEntity;

public interface RefBookOperationRepository extends
        JpaRepository<RefBookOperationEntity, Integer>,
        QuerydslPredicateExecutor<RefBookOperationEntity> {

        @Transactional
        Integer deleteAllByInstanceId(String instanceId);
}
