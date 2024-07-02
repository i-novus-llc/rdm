package ru.i_novus.ms.rdm.rest.service;

import lombok.extern.log4j.Log4j;
import net.n2oapp.platform.i18n.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.model.draft.Draft;
import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.service.PublishService;
import ru.i_novus.ms.rdm.api.service.RefBookService;
import ru.i_novus.ms.rdm.rest.loader.RefBookDataRequest;

@Service
@Log4j
@SuppressWarnings("java:S2139")
public class RefBookDataLoaderService {

    private static final Logger logger = LoggerFactory.getLogger(RefBookDataLoaderService.class);

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

    @Transactional
    public boolean createAndPublish(RefBookDataRequest request) {

        final FileModel fileModel = request.getFileModel();
        if (fileModel != null)
            return createAndPublishFromFile(request);

        return false; // to-do: Добавить поддержку code+structure+data.
    }

    @Transactional
    public boolean createOrUpdate(RefBookDataRequest request) {

        

        // to-do: Добавить поддержку code+structure+data.
        final FileModel fileModel = request.getFileModel();
        if (fileModel == null)
            return false;

        final String fileName = fileModel.getName();
        logger.info("Start data loading from file '{}'", fileName);

        return false;
    }

    private boolean createAndPublishFromFile(RefBookDataRequest request) {

        final FileModel fileModel = request.getFileModel();
        final String fileName = fileModel.getName();

        logger.info("Start data loading from file '{}'", fileName);
        try {
            final Draft draft = refBookService.create(fileModel);

            final PublishRequest publishRequest = new PublishRequest(null);
            publishService.publish(draft.getId(), publishRequest);

            logger.info("Finish data loading from file '{}'", fileName);

            return true;

        } catch (NotFoundException | IllegalArgumentException e) {

            final String errorMsg = String.format(LOG_ERROR_DATA_LOADING_WITH_EXCEPTION, fileName);
            logger.error(errorMsg, e);
            throw e;

        } catch (UserException e) {
            if (REF_BOOK_ALREADY_EXISTS_EXCEPTION_CODE.equals(e.getCode())) {

                logger.info(LOG_REF_BOOK_IS_ALREADY_EXISTS, e.getArgs()[0]);
                logger.info(LOG_SKIP_CREATE_REF_BOOK, fileName);
                return false;

            } else {
                logger.error(LOG_ERROR_CREATING_AND_PUBLISHING_REF_BOOK, fileName);

                final String errorMsg = String.format(LOG_ERROR_DATA_LOADING_WITH_EXCEPTION, fileName);
                logger.error(errorMsg, e);
                throw e;
            }

        } catch (Exception e) {

            final String errorMsg = String.format(LOG_ERROR_DATA_LOADING_WITH_EXCEPTION, fileName);
            logger.error(errorMsg, e);
            throw new UserException(UNKNOWN_ERROR_EXCEPTION_TEXT, e);
        }
    }

    private boolean updateAndPublish(RefBookDataRequest request) {

        return false;
    }
}
