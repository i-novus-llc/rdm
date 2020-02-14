package ru.inovus.ms.rdm.rest.loader;

import net.n2oapp.platform.i18n.UserException;
import net.n2oapp.platform.loader.server.ServerLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.inovus.ms.rdm.api.exception.NotFoundException;
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
    public static final String LOG_REF_BOOK_IS_ALREADY_EXISTS = "RefBook '{}' is already exists";
    public static final String LOG_SKIP_CREATE_REF_BOOK = "Skip create RefBook from file '{}'";
    public static final String LOG_ERROR_CREATING_AND_PUBLISHING_REF_BOOK = "Error creating and publishing refBook from file '{}'";
    public static final String LOG_ERROR_DATA_LOADING_WITH_EXCEPTION = "Error data loading from file '%s':";
    public static final String UNKNOWN_ERROR_EXCEPTION_TEXT = "Unknown error";

    @Autowired
    private RefBookService refBookService;

    @Autowired
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

        } catch (NotFoundException | IllegalArgumentException e) {
            String errorMsg = String.format(LOG_ERROR_DATA_LOADING_WITH_EXCEPTION, fileModel.getName());
            logger.error(errorMsg, e);
            throw e;

        } catch (UserException e) {
            if (REF_BOOK_ALREADY_EXISTS_EXCEPTION_CODE.equals(e.getCode())) {
                logger.info(LOG_REF_BOOK_IS_ALREADY_EXISTS, e.getArgs()[0]);
                logger.info(LOG_SKIP_CREATE_REF_BOOK, fileModel.getName());

            } else {
                logger.error(LOG_ERROR_CREATING_AND_PUBLISHING_REF_BOOK, fileModel.getName());
                String errorMsg = String.format(LOG_ERROR_DATA_LOADING_WITH_EXCEPTION, fileModel.getName());
                logger.error(errorMsg, e);
                throw e;
            }

        } catch (Exception e) {
            String errorMsg = String.format(LOG_ERROR_DATA_LOADING_WITH_EXCEPTION, fileModel.getName());
            logger.error(errorMsg, e);
            throw new UserException(UNKNOWN_ERROR_EXCEPTION_TEXT, e);
        }
    }
}
