package ru.i_novus.ms.rdm.rest.loader;

import net.n2oapp.platform.i18n.UserException;
import net.n2oapp.platform.loader.server.ServerLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.model.draft.Draft;
import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.service.PublishService;
import ru.i_novus.ms.rdm.api.service.RefBookService;

import java.util.List;

import static org.apache.cxf.common.util.CollectionUtils.isEmpty;

/** Загрузчик справочника. */
@Component
public class RefBookDataServerLoader implements ServerLoader<RefBookDataRequest> {

    private static final Logger logger = LoggerFactory.getLogger(RefBookDataServerLoader.class);

    private static final String REF_BOOK_ALREADY_EXISTS_EXCEPTION_CODE = "refbook.with.code.already.exists";
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
    public Class<RefBookDataRequest> getDataType() {
        return RefBookDataRequest.class;
    }

    @Override
    public void load(List<RefBookDataRequest> data, String subject) {

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

    @SuppressWarnings("java:S2139")
    private void createAndPublishRefBook(RefBookDataRequest request) {

        FileModel fileModel = request.getFileModel();
        if (fileModel == null)
            return;

        logger.info("Start data loading from file '{}'", fileModel.getName());
        try {
            Draft draft = refBookService.create(fileModel);

            PublishRequest publishRequest = new PublishRequest(null);
            publishService.publish(draft.getId(), publishRequest);

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
