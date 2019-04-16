package ru.inovus.ms.rdm.repositiory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import ru.inovus.ms.rdm.entity.AuditLogEntity;

public interface AuditLogRepository extends
        JpaRepository<AuditLogEntity, Integer>,
        QuerydslPredicateExecutor<AuditLogEntity> {

}
