package ru.i_novus.ms.rdm.rest.loader;

import lombok.extern.log4j.Log4j;
import net.n2oapp.platform.loader.server.ServerLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.loader.RefBookDataRequest;
import ru.i_novus.ms.rdm.api.model.loader.RefBookDataResponse;
import ru.i_novus.ms.rdm.api.service.loader.RefBookDataLoaderService;

import java.util.List;

import static org.springframework.util.CollectionUtils.isEmpty;

/** Загрузчик справочника. */
@Component
@Log4j
public class RefBookDataServerLoader implements ServerLoader<RefBookDataRequest> {

    private static final Logger logger = LoggerFactory.getLogger(RefBookDataServerLoader.class);

    @Autowired
    private RefBookDataLoaderService service;

    @Override
    public String getTarget() {
        return "refBookData";
    }

    @Override
    public Class<RefBookDataRequest> getDataType() {
        return RefBookDataRequest.class;
    }

    @Override
    public void load(List<RefBookDataRequest> data, String subject) {

        if (isEmpty(data)) {
            logger.info("No data loading from subject = {}", subject);
            return;
        }

        logger.info("Start data loading from subject = {}, {} request(s)", subject, data.size());
        try {
            data.forEach(this::load);

            logger.info("Finish data loading from subject = {}", subject);

        } catch (Exception e) {
            logger.error("Error data loading from subject = {}", subject);
            throw e;
        }
    }

    private void load(RefBookDataRequest request) {

        logger.info("Data loading request = {}", request);
        final RefBookDataResponse response = service.load(request);
        logger.info("Data loading response = {}", response);
    }
}
