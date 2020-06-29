package ru.inovus.ms.rdm.impl.service;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.api.enumeration.RefBookOperation;
import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.impl.entity.RefBookOperationEntity;
import ru.inovus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.impl.repository.RefBookOperationRepository;
import ru.inovus.ms.rdm.impl.repository.RefBookVersionRepository;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

@Service
@SuppressWarnings("unused")
public class RefBookLockServiceImpl implements RefBookLockService {

    private static final ThreadLocal<Pair<String, Integer>> LOCKS_COUNTER = new ThreadLocal<>();
    private static final Lock WRITE_WAL_LOCK = new ReentrantReadWriteLock().writeLock();

    private static final Path WAL_PATH = Path.of("rdm_log", "lock_wal.txt");
    private static final String LOCK_ACQUIRED = "ACQUIRED";
    private static final String LOCK_RELEASED = "RELEASED";

    private static final Logger logger = LoggerFactory.getLogger(RefBookLockServiceImpl.class);

    private static final Duration OPERATION_MAX_LIVE_PERIOD = Duration.ofHours(4);
    private static final String DEFAULT_USER = "admin";

    private RefBookOperationRepository operationRepository;
    private RefBookVersionRepository versionRepository;

    @Autowired
    public RefBookLockServiceImpl(RefBookOperationRepository operationRepository,
                                  RefBookVersionRepository versionRepository) {
        this.operationRepository = operationRepository;
        this.versionRepository = versionRepository;
    }

    @PostConstruct
    public void cleanOperations() {
        if (Files.exists(WAL_PATH)) {
            try {
                Set<String> unreleasedLocks = new HashSet<>();
                try (Stream<String> lines = Files.lines(WAL_PATH)) {
                    lines.filter(s -> !s.isBlank()).forEach(str -> {
                        String[] split = str.split("\\s+");
                        if (split[0].startsWith(LOCK_ACQUIRED)) {
                            unreleasedLocks.add(split[1]);
                        } else
                            unreleasedLocks.remove(split[1]);
                    });
                }
                int released = operationRepository.deleteAllByLockIdIn(unreleasedLocks);
                logger.info("{} unreleased locks detected.", released);
                clearWal();
            } catch (IOException e) {
                logger.error("Can't release previously acquired locks due to IO exception.", e);
            }
        } else {
            try {
                Files.createDirectories(WAL_PATH.subpath(0, WAL_PATH.getNameCount() - 1));
                Files.createFile(WAL_PATH);
            } catch (IOException e) {
                logger.error("Can't create WAL file. Shutting down the application.", e);
                System.exit(-1); //NOSONAR
            }
        }
    }

    private void clearWal() {
        try (Writer in = Files.newBufferedWriter(WAL_PATH)) {
            /*Удаляем, все что было в файле, так как оно нам больше не нужно*/
        } catch (IOException e) {
            logger.warn("Can't remove previous content of the WAL file.", e);
        }
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

    private void addRefBookOperation(Integer refBookId, RefBookOperation operation) {
        if (LOCKS_COUNTER.get() == null) {
            validateRefBookNotBusyByRefBookId(refBookId);
            String lockId = UUID.randomUUID().toString();
            WRITE_WAL_LOCK.lock();
            try {
                Files.write(WAL_PATH, (LOCK_ACQUIRED + " " + lockId + "\n").getBytes(), StandardOpenOption.APPEND);
            } catch (IOException e) {
                logger.error("Can't acquire lock due to IO exception.", e);
                throw new RdmException(e);
            } finally {
                WRITE_WAL_LOCK.unlock();
            }
            operationRepository.save(new RefBookOperationEntity(refBookId, operation, lockId, DEFAULT_USER));
            LOCKS_COUNTER.set(Pair.of(lockId, 1));
        } else {
            int locksAcquired = LOCKS_COUNTER.get().getSecond();
            LOCKS_COUNTER.set(Pair.of(LOCKS_COUNTER.get().getFirst(), locksAcquired + 1));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void deleteRefBookOperation(Integer refBookId) {
        int locksAcquired = LOCKS_COUNTER.get() == null ? 0 : LOCKS_COUNTER.get().getSecond();
        if (locksAcquired == 0) {
            logger.warn("Current thread {} tries to release non-existent lock.", Thread.currentThread().getName());
            throw new RdmException("No locks acquired.");
        }
        LOCKS_COUNTER.set(Pair.of(LOCKS_COUNTER.get().getFirst(), --locksAcquired));
        if (locksAcquired == 0) {
            String lockId = LOCKS_COUNTER.get().getFirst();
            int n;
            try {
                n = operationRepository.deleteByRefBookId(refBookId);
            } finally {
                LOCKS_COUNTER.remove();
            }
            if (n == 1) {
                WRITE_WAL_LOCK.lock();
                try {
                    Files.write(WAL_PATH, (LOCK_RELEASED + " " + lockId + "\n").getBytes(), StandardOpenOption.APPEND);
                } catch (IOException e) {
                    logger.error("Can't access WAL. Lock release will be silently ignored.", e);
                } finally {
                    WRITE_WAL_LOCK.unlock();
                }
            } else {
                logger.error("Lock with id {} was not deleted from database.", lockId);
            }
        }
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

        RefBookOperationEntity refBookOperationEntity;
        try {
            refBookOperationEntity = operationRepository.findByRefBookId(refBookId);
        } catch (Exception e) {
            logger.error("Error occurred on database level while trying to acquire exclusive lock.", e);
            throw new UserException(new Message("refbook.lock.cannot-be-acquired", refBookId));
        }
        if (refBookOperationEntity == null)
            return;

        if (Duration.between(refBookOperationEntity.getCreationDate(), LocalDateTime.now(Clock.systemUTC()))
                .compareTo(OPERATION_MAX_LIVE_PERIOD) > 0) {
            operationRepository.deleteById(refBookOperationEntity.getId());
            return;
        }

        if (RefBookOperation.PUBLISHING.equals(refBookOperationEntity.getOperation())) {
            throw new UserException(new Message("refbook.lock.draft.is.publishing", refBookId));
        } else if (RefBookOperation.UPDATING.equals(refBookOperationEntity.getOperation())) {
            throw new UserException(new Message("refbook.lock.draft.is.updating", refBookId));
        }
    }

}
