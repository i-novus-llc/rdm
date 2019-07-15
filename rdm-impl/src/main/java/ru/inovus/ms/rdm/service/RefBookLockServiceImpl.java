package ru.inovus.ms.rdm.service;

import net.n2oapp.platform.i18n.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.entity.RefBookEntity;
import ru.inovus.ms.rdm.entity.RefBookOperationEntity;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.RefBookOperation;
import ru.inovus.ms.rdm.repositiory.RefBookOperationRepository;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class RefBookLockServiceImpl implements RefBookLockService {

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
        saveRefBookOperation(refBookId, RefBookOperation.PUBLISHING);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void setRefBookUploading(Integer refBookId) {
        saveRefBookOperation(refBookId, RefBookOperation.UPLOADING);
    }

    private void saveRefBookOperation(Integer refBookId, RefBookOperation operation) {
        validateRefBookNotBusyByRefBookId(refBookId);

        RefBookEntity refBook = new RefBookEntity();
        refBook.setId(refBookId);
        operationRepository.save(new RefBookOperationEntity(refBook, operation, instanceId, DEFAULT_USER));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void deleteRefBookAction(Integer refBookId) {
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

        if (refBookOperationEntity != null) {
            if (Duration.between(refBookOperationEntity.getCreationDate(), LocalDateTime.now())
                    .compareTo(OPERATION_MAX_LIVE_PERIOD) > 0) {
                operationRepository.deleteById(refBookOperationEntity.getId());
                return;
            }

            if (RefBookOperation.PUBLISHING.equals(refBookOperationEntity.getOperation())) {
                throw new UserException("draft.is.publishing");
            } else if (RefBookOperation.UPLOADING.equals(refBookOperationEntity.getOperation())) {
                throw new UserException("draft.is.uploading");
            }
        }
    }
}
