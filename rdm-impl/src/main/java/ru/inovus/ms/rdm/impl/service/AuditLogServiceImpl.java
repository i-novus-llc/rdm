package ru.inovus.ms.rdm.impl.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ru.inovus.ms.rdm.api.model.audit.AuditAction;
import ru.inovus.ms.rdm.api.model.audit.AuditLog;
import ru.inovus.ms.rdm.api.model.audit.AuditLogCriteria;
import ru.inovus.ms.rdm.api.service.AuditLogService;
import ru.inovus.ms.rdm.api.util.StringUtils;
import ru.inovus.ms.rdm.impl.entity.AuditLogEntity;
import ru.inovus.ms.rdm.impl.repository.AuditLogRepository;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static ru.inovus.ms.rdm.impl.entity.QAuditLogEntity.auditLogEntity;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private AuditLogRepository auditLogRepository;

    private EnumSet<AuditAction> disabledActions;

    @Value("${rdm.audit.disabledActions}")
    public void setDisabled(String disabled) {
        List<String> l = StringUtils.splitStripSpaces(disabled).stream().filter(s -> !s.isEmpty()).collect(toList());
        AuditAction[] actionsArr = new AuditAction[l.size()];
        for (int i = 0; i < l.size(); i++)
            actionsArr[i] = AuditAction.valueOf(l.get(i).toUpperCase());
        disabledActions = EnumSet.copyOf(List.of(actionsArr));
    }

    @Autowired
    @SuppressWarnings("unused")
    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public AuditLog addAction(AuditLog action) {
        if (!disabledActions.contains(action.getAction())) {
            AuditLogEntity actionEntity = new AuditLogEntity(
                    action.getUser(),
                    action.getDate(),
                    action.getAction(),
                    action.getContext());
            actionEntity = auditLogRepository.save(actionEntity);
            return auditActionModel(actionEntity);
        }
        return null;
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
