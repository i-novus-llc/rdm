package ru.i_novus.ms.rdm.impl.service;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.api.enumeration.RefBookOperation;
import ru.i_novus.ms.rdm.impl.entity.RefBookOperationEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookOperationRepository;

import javax.annotation.PostConstruct;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@SuppressWarnings("unused")
public class RefBookLockServiceImpl implements RefBookLockService {

    private static final Logger logger = LoggerFactory.getLogger(RefBookLockServiceImpl.class);

    private static final int OPERATION_MAX_LIVE_HOURS = 4;
    private static final Duration OPERATION_MAX_LIVE_PERIOD = Duration.ofHours(OPERATION_MAX_LIVE_HOURS);
    private static final String DEFAULT_USER = "admin";

    private final RefBookOperationRepository operationRepository;

    @Autowired
    public RefBookLockServiceImpl(RefBookOperationRepository operationRepository) {
        this.operationRepository = operationRepository;
    }

    @PostConstruct
    public void cleanOperations() {

        final LocalDateTime creationDate = getNowDateTime().minusHours(OPERATION_MAX_LIVE_HOURS);
        final int released = operationRepository.deleteAllByCreationDateLessThan(creationDate);
        logger.info("{} unreleased locks detected.", released);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void setRefBookPublishing(Integer refBookId) {
        addRefBookOperation(refBookId, RefBookOperation.PUBLISHING);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void setRefBookUpdating(Integer refBookId) {
        addRefBookOperation(refBookId, RefBookOperation.UPDATING);
    }

    @SuppressWarnings("java:S2139")
    private void addRefBookOperation(Integer refBookId, RefBookOperation operation) {

        validateRefBookNotBusy(refBookId);

        operationRepository.save(new RefBookOperationEntity(refBookId, operation, DEFAULT_USER));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void deleteRefBookOperation(Integer refBookId) {

        int deletedCount = operationRepository.deleteByRefBookId(refBookId);
        if (deletedCount != 1) {
            logger.error("Lock with refBookId {} was not deleted from database.", refBookId);
        }
    }

    @Override
    public void validateRefBookNotBusy(Integer refBookId) {

        RefBookOperationEntity refBookOperationEntity;
        try {
            refBookOperationEntity = operationRepository.findByRefBookId(refBookId);

        } catch (Exception e) {
            logger.error("Error occurred on database level while trying to acquire exclusive lock.", e);
            throw new UserException(new Message("refbook.lock.cannot-be-acquired", refBookId));
        }
        if (refBookOperationEntity == null)
            return;

        if (Duration.between(refBookOperationEntity.getCreationDate(), getNowDateTime())
                .compareTo(OPERATION_MAX_LIVE_PERIOD) > 0) {
            operationRepository.deleteById(refBookOperationEntity.getId());
            return;
        }

        if (RefBookOperation.PUBLISHING.equals(refBookOperationEntity.getOperation()))
            throw new UserException(new Message("refbook.lock.draft.is.publishing", refBookId));

        if (RefBookOperation.UPDATING.equals(refBookOperationEntity.getOperation()))
            throw new UserException(new Message("refbook.lock.draft.is.updating", refBookId));
    }

    /* Текущее время для определения старых заблокированных записей. */
    private LocalDateTime getNowDateTime() {
        return LocalDateTime.now(Clock.systemUTC());
    }
}
