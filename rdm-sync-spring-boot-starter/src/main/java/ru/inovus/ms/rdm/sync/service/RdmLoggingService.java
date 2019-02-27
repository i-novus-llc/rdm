package ru.inovus.ms.rdm.sync.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author lgalimova
 * @since 27.02.2019
 */
public class RdmLoggingService {
    private enum Status {error, ok}

    @Autowired
    private RdmSyncDao dao;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logError(String refbookCode, String oldVersion, String newVersion, String message, String stack) {
        dao.log(Status.error.name(), refbookCode, oldVersion, newVersion, message, stack);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logOk(String refbookCode, String oldVersion, String newVersion) {
        dao.log(Status.ok.name(), refbookCode, oldVersion, newVersion, null, null);
    }
}
