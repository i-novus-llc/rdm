package ru.inovus.ms.rdm.impl.service;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.api.enumeration.RefBookOperation;
import ru.inovus.ms.rdm.impl.entity.RefBookEntity;
import ru.inovus.ms.rdm.impl.entity.RefBookOperationEntity;
import ru.inovus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.impl.repository.RefBookOperationRepository;
import ru.inovus.ms.rdm.impl.repository.RefBookVersionRepository;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@SuppressWarnings("unused")
public class RefBookLockServiceImpl implements RefBookLockService {

    private static final String REFBOOK_LOCK_DRAFT_IS_PUBLISHING = "refbook.lock.draft.is.publishing";
    private static final String REFBOOK_LOCK_DRAFT_IS_UPLOADING = "refbook.lock.draft.is.uploading";

    private static final Logger logger = LoggerFactory.getLogger(RefBookLockServiceImpl.class);

    private static final Duration OPERATION_MAX_LIVE_PERIOD = Duration.ofHours(4);
    private static final String DEFAULT_USER = "admin";

    private String instanceId = "localhost";

    private RefBookOperationRepository operationRepository;
    private RefBookVersionRepository versionRepository;

    @Autowired
    public RefBookLockServiceImpl(RefBookOperationRepository operationRepository,
                                  RefBookVersionRepository versionRepository) {
        this.operationRepository = operationRepository;
        this.versionRepository = versionRepository;
        try {
            this.instanceId = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            logger.error("cannot.read.host.address", e);
        }
    }

    @PostConstruct
    @Override
    public void cleanOperations() {
        operationRepository.deleteAllByInstanceId(instanceId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void setRefBookPublishing(Integer refBookId) {
        addRefBookOperation(refBookId, RefBookOperation.PUBLISHING);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void setRefBookUploading(Integer refBookId) {
        addRefBookOperation(refBookId, RefBookOperation.UPLOADING);
    }

    private void addRefBookOperation(Integer refBookId, RefBookOperation operation) {
        validateRefBookNotBusyByRefBookId(refBookId);

        RefBookEntity refBook = new RefBookEntity();
        refBook.setId(refBookId);
        operationRepository.save(new RefBookOperationEntity(refBook, operation, instanceId, DEFAULT_USER));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void deleteRefBookOperation(Integer refBookId) {
        operationRepository.deleteByRefBookId(refBookId);
    }

    @Override
    public void validateRefBookNotBusyByVersionId(Integer versionId) {
        Optional<RefBookVersionEntity> versionEntity = versionRepository.findById(versionId);
        versionEntity.ifPresent(
                refBookVersionEntity ->
                        validateRefBookNotBusyByRefBookId(refBookVersionEntity.getRefBook().getId())
        );
    }

    @Override
    public void validateRefBookNotBusyByRefBookId(Integer refBookId) {

        RefBookOperationEntity refBookOperationEntity = operationRepository.findByRefBookId(refBookId);
        if (refBookOperationEntity == null)
            return;

        if (Duration.between(refBookOperationEntity.getCreationDate(), LocalDateTime.now(Clock.systemUTC()))
                .compareTo(OPERATION_MAX_LIVE_PERIOD) > 0) {
            operationRepository.deleteById(refBookOperationEntity.getId());
            return;
        }

        if (RefBookOperation.PUBLISHING.equals(refBookOperationEntity.getOperation())) {
            throw new UserException(new Message(REFBOOK_LOCK_DRAFT_IS_PUBLISHING, refBookId));
        } else if (RefBookOperation.UPLOADING.equals(refBookOperationEntity.getOperation())) {
            throw new UserException(new Message(REFBOOK_LOCK_DRAFT_IS_UPLOADING, refBookId));
        }
    }
}
