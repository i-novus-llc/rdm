package ru.inovus.ms.rdm.sync.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.sync.model.Log;

import java.time.LocalDate;
import java.util.List;

/**
 * @author lgalimova
 * @since 27.02.2019
 */
public class RdmLoggingService {
    private enum Status {ERROR, OK}

    @Autowired
    private RdmSyncDao dao;

    public List<Log> getList(LocalDate date, String refbookCode) {
        return dao.getList(date, refbookCode);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logError(String refbookCode, String oldVersion, String newVersion, String message, String stack) {
        dao.log(Status.ERROR.name(), refbookCode, oldVersion, newVersion, message, stack);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logOk(String refbookCode, String oldVersion, String newVersion) {
        dao.log(Status.OK.name(), refbookCode, oldVersion, newVersion, null, null);
    }
}
