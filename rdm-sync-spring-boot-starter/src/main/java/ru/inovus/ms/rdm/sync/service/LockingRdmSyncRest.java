package ru.inovus.ms.rdm.sync.service;

import org.springframework.beans.factory.annotation.Autowired;

public class LockingRdmSyncRest extends RdmSyncRestImpl {

    @Autowired
    private RdmSyncDao dao;

    @Override
    public void update(String refBookCode) {
        if (dao.lockRefBookForUpdate(refBookCode, false))
            super.update(refBookCode);
    }
}
