package ru.inovus.ms.rdm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.RdmClientSyncConfig;
import ru.inovus.ms.rdm.model.RefBook;
import ru.inovus.ms.rdm.model.VersionMapping;
import ru.inovus.ms.rdm.service.api.RefBookService;

import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * @author lgalimova
 * @since 20.02.2019
 */

public class RdmSyncRestImpl implements RdmSyncRest {
    @Autowired
    private RefBookService refBookService;
    @Autowired
    private RdmSyncService rdmSyncService;
    private RdmClientSyncConfig config;

    public RdmSyncRestImpl(RdmClientSyncConfig config) {
        this.config = config;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Response update() {
        Map<VersionMapping, RefBook> refbooksToUpdate = rdmSyncService.getRefbooksToUpdate();
        for (Map.Entry<VersionMapping, RefBook> entry : refbooksToUpdate.entrySet()) {
            rdmSyncService.update();
        }

        return null;
    }

}
