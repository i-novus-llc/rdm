package ru.inovus.ms.rdm.impl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.impl.entity.RefBookOperationEntity;

import javax.persistence.LockModeType;
import java.util.Set;

public interface RefBookOperationRepository extends
        JpaRepository<RefBookOperationEntity, Integer>,
        QuerydslPredicateExecutor<RefBookOperationEntity> {

        @Lock(value = LockModeType.PESSIMISTIC_WRITE)
        RefBookOperationEntity findByRefBookId(Integer refBookId);

        void deleteByRefBookId(Integer refBookId);

        @Transactional
        int deleteAllByLockId(Set<String> lockIds);

}
