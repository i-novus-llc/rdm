package ru.inovus.ms.rdm.rest.loader;

import net.n2oapp.platform.jaxrs.RestException;
import net.n2oapp.platform.loader.server.ServerLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.inovus.ms.rdm.api.model.FileModel;
import ru.inovus.ms.rdm.api.model.draft.Draft;
import ru.inovus.ms.rdm.api.service.PublishService;
import ru.inovus.ms.rdm.api.service.RefBookService;

import java.util.List;

import static org.apache.cxf.common.util.CollectionUtils.isEmpty;

@Component
public class RefBookDataServerLoader implements ServerLoader<FileModel> {

    private static final Logger logger = LoggerFactory.getLogger(RefBookDataServerLoader.class);

    private static final String REF_BOOK_ALREADY_EXISTS_EXCEPTION_CODE = "refbook.already.exists";
    public static final String LOG_REF_BOOK_IS_ALREADY_EXISTS_SKIP_CREATE = "RefBook is already exists. Skip create from file '{}'";
    public static final String LOG_ERROR_CREATING_AND_PUBLISHING_REF_BOOK = "Error creating and publishing refBook from file '{}'";
    public static final String LOG_ERROR_DATA_LOADING_WITH_EXCEPTION = "Error data loading from file '%s':";

    @Autowired
    @Qualifier("refBookServiceJaxRsProxyClient")
    private RefBookService refBookService;

    @Autowired
    @Qualifier("publishServiceJaxRsProxyClient")
    private PublishService publishService;

    @Override
    public String getTarget() {
        return "refBookData";
    }

    @Override
    public Class<FileModel> getDataType() {
        return FileModel.class;
    }

    @Override
    public void load(List<FileModel> data, String subject) {

        if (isEmpty(data)) {
            logger.info("No data loading from subject = {}", subject);
            return;
        }

        logger.info("Start data loading from subject = {}, {} file(s)", subject, data.size());
        try {
            data.forEach(this::createAndPublishRefBook);

            logger.info("Finish data loading from subject = {}", subject);

        } catch (Exception e) {
            logger.error("Error data loading from subject = {}", subject);
            throw e;
        }
    }

    private void createAndPublishRefBook(FileModel fileModel) {

        logger.info("Start data loading from file '{}'", fileModel.getName());
        try {
            Draft draft = refBookService.create(fileModel);
            publishService.publish(draft.getId(), null, null, null, false);

            logger.info("Finish data loading from file '{}'", fileModel.getName());

        } catch (RestException e) {
            if (REF_BOOK_ALREADY_EXISTS_EXCEPTION_CODE.equals(e.getMessage())) {
                logger.info(LOG_REF_BOOK_IS_ALREADY_EXISTS_SKIP_CREATE, fileModel.getName());

            } else {
                logger.error(LOG_ERROR_CREATING_AND_PUBLISHING_REF_BOOK, fileModel.getName());
                String errorMsg = String.format(LOG_ERROR_DATA_LOADING_WITH_EXCEPTION, fileModel.getName());
                logger.error(errorMsg, e);
                throw e;
            }
        }
    }
}
