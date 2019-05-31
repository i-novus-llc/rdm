package ru.inovus.ms.rdm.repositiory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import ru.inovus.ms.rdm.entity.RefBookConflictEntity;
import ru.inovus.ms.rdm.enumeration.ConflictType;

import java.util.List;

public interface RefBookConflictRepository extends
        JpaRepository<RefBookConflictEntity, Integer>,
        QuerydslPredicateExecutor<RefBookConflictEntity> {

    List<RefBookConflictEntity> findAllByReferrerVersionId(Integer referrerId);

    List<RefBookConflictEntity> findAllByReferrerVersionIdAndConflictType(Integer referrerId, ConflictType conflictType);

}
