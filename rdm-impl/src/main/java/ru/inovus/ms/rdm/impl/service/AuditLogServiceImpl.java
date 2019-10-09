package ru.inovus.ms.rdm.impl.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ru.inovus.ms.rdm.api.service.AuditLogService;
import ru.inovus.ms.rdm.impl.entity.AuditLogEntity;
import ru.inovus.ms.rdm.api.model.audit.AuditLog;
import ru.inovus.ms.rdm.api.model.audit.AuditLogCriteria;
import ru.inovus.ms.rdm.impl.repository.AuditLogRepository;

import java.util.Collections;

import static ru.inovus.ms.rdm.impl.entity.QAuditLogEntity.auditLogEntity;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private AuditLogRepository auditLogRepository;

    @Autowired
    @SuppressWarnings("unused")
    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public AuditLog addAction(AuditLog action) {
        AuditLogEntity actionEntity = new AuditLogEntity(
                action.getUser(),
                action.getDate(),
                action.getAction(),
                action.getContext());
        actionEntity = auditLogRepository.save(actionEntity);
        return auditActionModel(actionEntity);
    }

    @Override
    public Page<AuditLog> getActions(AuditLogCriteria criteria) {
        if(CollectionUtils.isEmpty(criteria.getOrders())) {
            criteria.setOrders(Collections.singletonList(new Sort.Order(Sort.Direction.DESC, "date")));
        }
        return auditLogRepository.findAll(toPredicate(criteria), criteria).map(this::auditActionModel);
    }

    private AuditLog auditActionModel(AuditLogEntity entity) {
        return new AuditLog(
                entity.getId(),
                entity.getUser(),
                entity.getDate(),
                entity.getAction(),
                entity.getContext());
    }

    private Predicate toPredicate(AuditLogCriteria criteria){
        BooleanBuilder where = new BooleanBuilder();

        if (criteria.getUser() != null)
            where.and(auditLogEntity.user.containsIgnoreCase(criteria.getUser()));

        if (criteria.getAction() != null)
            where.and(auditLogEntity.action.eq(criteria.getAction()));

        if (criteria.getContext() != null)
            where.and(auditLogEntity.context.containsIgnoreCase(criteria.getContext()));

        if (criteria.getFromDate() != null)
            where.and(auditLogEntity.date.after(criteria.getFromDate()));

        if (criteria.getToDate() != null)
            where.and(auditLogEntity.date.before(criteria.getToDate()));

        return where.getValue();
    }

}
