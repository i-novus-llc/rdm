package ru.i_novus.ms.rdm.impl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.impl.entity.RefBookOperationEntity;

import javax.persistence.LockModeType;
import java.time.LocalDateTime;

public interface RefBookOperationRepository extends
        JpaRepository<RefBookOperationEntity, Integer>,
        QuerydslPredicateExecutor<RefBookOperationEntity> {

    @Lock(value = LockModeType.PESSIMISTIC_WRITE)
    RefBookOperationEntity findByRefBookId(Integer refBookId);

    int deleteByRefBookId(Integer refBookId);

    @Transactional
    int deleteAllByCreationDateLessThan(LocalDateTime creationDate);
}
