package ru.inovus.ms.rdm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.inovus.ms.rdm.RdmClientSyncConfig;
import ru.inovus.ms.rdm.model.RefBook;
import ru.inovus.ms.rdm.model.RefBookCriteria;
import ru.inovus.ms.rdm.service.api.RefBookService;

import javax.ws.rs.core.Response;

/**
 * @author lgalimova
 * @since 20.02.2019
 */

public class RdmSyncRestImpl implements RdmSyncRest {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private RefBookService refBookService;
    private RdmClientSyncConfig config;

    public RdmSyncRestImpl(RdmClientSyncConfig config) {
        this.config = config;
    }

    @Override
    public Response update() {
        RefBookCriteria refBookCriteria = new RefBookCriteria();
        refBookCriteria.setCode("S007");
        Page<RefBook> list = refBookService.search(refBookCriteria);
        return null;
    }

}
