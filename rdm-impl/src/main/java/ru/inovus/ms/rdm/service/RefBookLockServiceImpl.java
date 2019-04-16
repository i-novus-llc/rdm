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
import ru.inovus.ms.rdm.repositiory.RefBookRepository;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

@Service
public class RefBookLockServiceImpl implements RefBookLockService {

    private static final Logger logger = LoggerFactory.getLogger(RefBookLockServiceImpl.class);

    private static final String DEFAULT_USER = "admin";
    private String instanceId = "localhost";

    private RefBookRepository refBookRepository;
    private RefBookOperationRepository operationRepository;
    private RefBookVersionRepository versionRepository;

    @Autowired
    public RefBookLockServiceImpl(RefBookRepository refBookRepository, RefBookOperationRepository operationRepository,
                                  RefBookVersionRepository versionRepository) {
        this.refBookRepository = refBookRepository;
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
        RefBookEntity refBook = refBookRepository.getOne(refBookId);
        validateRefBookNotBusy(refBook);
        operationRepository.save(new RefBookOperationEntity(refBook, RefBookOperation.PUBLISHING, instanceId, DEFAULT_USER));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void setRefBookUploading(Integer refBookId) {
        RefBookEntity refBook = refBookRepository.getOne(refBookId);
        validateRefBookNotBusy(refBook);
        operationRepository.save(new RefBookOperationEntity(refBook, RefBookOperation.UPLOADING, instanceId, DEFAULT_USER));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void deleteRefBookAction(Integer refBookId) {
        Optional<RefBookEntity> refBook = refBookRepository.findById(refBookId);
        if (refBook.isPresent() && refBook.get().getCurrentOperation() != null) {
            operationRepository.deleteById(refBook.get().getCurrentOperation().getId());
            refBook.get().setCurrentOperation(null);
        }
    }

    @Override
    public void validateRefBookNotBusyByVersionId(Integer versionId) {
        Optional<RefBookVersionEntity> versionEntity = versionRepository.findById(versionId);
        versionEntity.ifPresent(
                refBookVersionEntity ->
                        validateRefBookNotBusy(refBookVersionEntity.getRefBook())
        );
    }

    @Override
    public void validateRefBookNotBusy(RefBookEntity refBookEntity) {
        if (refBookEntity.getCurrentOperation() != null) {
            if (RefBookOperation.PUBLISHING.equals(refBookEntity.getCurrentOperation().getOperation())) {
                throw new UserException("draft.is.publishing");
            } else if (RefBookOperation.UPLOADING.equals(refBookEntity.getCurrentOperation().getOperation())) {
                throw new UserException("draft.is.uploading");
            }
        }
    }
}
