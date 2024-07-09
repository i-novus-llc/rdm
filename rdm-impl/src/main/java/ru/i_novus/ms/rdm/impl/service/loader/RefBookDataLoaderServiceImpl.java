package ru.i_novus.ms.rdm.impl.service.loader;

import lombok.extern.log4j.Log4j;
import net.n2oapp.platform.i18n.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.model.draft.Draft;
import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.model.draft.PublishResponse;
import ru.i_novus.ms.rdm.api.model.loader.RefBookDataRequest;
import ru.i_novus.ms.rdm.api.model.loader.RefBookDataResponse;
import ru.i_novus.ms.rdm.api.model.loader.RefBookDataUpdateTypeEnum;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCriteria;
import ru.i_novus.ms.rdm.api.service.DraftService;
import ru.i_novus.ms.rdm.api.service.PublishService;
import ru.i_novus.ms.rdm.api.service.RefBookService;
import ru.i_novus.ms.rdm.api.service.loader.RefBookDataLoaderService;
import ru.i_novus.ms.rdm.api.util.TimeUtils;
import ru.i_novus.ms.rdm.impl.repository.loader.RefBookDataLoadLogRepository;

import java.time.LocalDateTime;

import static ru.i_novus.ms.rdm.api.model.loader.RefBookDataUpdateTypeEnum.CREATE_ONLY;
import static ru.i_novus.ms.rdm.api.model.loader.RefBookDataUpdateTypeEnum.SKIP_ON_DRAFT;

@Service
@Log4j
@SuppressWarnings("java:S2139")
public class RefBookDataLoaderServiceImpl implements RefBookDataLoaderService {

    private static final Logger logger = LoggerFactory.getLogger(RefBookDataLoaderServiceImpl.class);

    private static final String REF_BOOK_ALREADY_EXISTS_EXCEPTION_CODE = "refbook.with.code.already.exists";
    public static final String LOG_REF_BOOK_IS_ALREADY_EXISTS = "RefBook '{}' is already exists";
    public static final String LOG_SKIP_CREATE_REF_BOOK = "Skip create RefBook from file '{}'";
    public static final String LOG_ERROR_CREATING_AND_PUBLISHING_REF_BOOK = "Error creating and publishing refBook from file '{}'";
    public static final String LOG_ERROR_CREATING_AND_PUBLISHING_DRAFT = "Error creating and publishing draft from file '{}'";
    public static final String LOG_ERROR_DATA_LOADING_WITH_EXCEPTION = "Error data loading from file '%s':";
    public static final String UNKNOWN_ERROR_EXCEPTION_TEXT = "Unknown error";

    @Autowired
    private RefBookService refBookService;

    @Autowired
    private DraftService draftService;

    @Autowired
    private PublishService publishService;

    @Transactional
    @Override
    public RefBookDataResponse load(RefBookDataRequest request) {

        final RefBookDataUpdateTypeEnum updateType = request.getUpdateType();
        switch (updateType) {
            case CREATE_ONLY:
                return createAndPublish(request);
            case FORCE_UPDATE:
            case SKIP_ON_DRAFT:
                return createOrUpdate(request);
            default:
                return null;
        }
    }

    private RefBookDataResponse createAndPublish(RefBookDataRequest request) {

        final FileModel fileModel = request.getFileModel();
        if (fileModel != null)
            return createAndPublishFromFile(request);

        return null; // to-do: Добавить поддержку code+structure+data.
    }

    private RefBookDataResponse createOrUpdate(RefBookDataRequest request) {

        final RefBook refBook = findRefBook(request.getCode());
        if (refBook == null)
            return createAndPublish(request);

        final RefBookDataUpdateTypeEnum updateType = request.getUpdateType();
        if (CREATE_ONLY.equals(updateType))
            return null;

        final Draft draft = draftService.findDraft(refBook.getCode());
        if (draft != null && SKIP_ON_DRAFT.equals(updateType))
            return null;

        return updateAndPublish(refBook, request);
    }

    private RefBookDataResponse updateAndPublish(RefBook refBook, RefBookDataRequest request) {

        final FileModel fileModel = request.getFileModel();
        if (fileModel != null)
            return updateAndPublishFromFile(refBook, request);

        return null; // to-do: Добавить поддержку code+structure+data.
    }

    private RefBook findRefBook(String refBookCode) {

        final RefBookCriteria refBookCriteria = new RefBookCriteria();
        refBookCriteria.setCodeExact(refBookCode);

        final Page<RefBook> refBooks = refBookService.search(refBookCriteria);
        return refBooks.getTotalElements() > 0 ? refBooks.getContent().get(0) : null;
    }

    private RefBookDataResponse createAndPublishFromFile(RefBookDataRequest request) {

        final FileModel fileModel = request.getFileModel();
        final String fileName = fileModel.getName();

        logger.info("Start refBook data loading from file '{}'", fileName);
        try {
            final Draft draft = refBookService.create(fileModel);

            final LocalDateTime executedDate = TimeUtils.now();
            final PublishResponse publishResponse = publishDraft(draft.getId(), executedDate);

            logger.info("Finish refBook data loading from file '{}'", fileName);

            final Integer refBookId = refBookService.getId(publishResponse.getRefBookCode());
            return new RefBookDataResponse(refBookId, executedDate);

        } catch (NotFoundException | IllegalArgumentException e) {

            final String errorMsg = String.format(LOG_ERROR_DATA_LOADING_WITH_EXCEPTION, fileName);
            logger.error(errorMsg, e);
            throw e;

        } catch (UserException e) {
            if (REF_BOOK_ALREADY_EXISTS_EXCEPTION_CODE.equals(e.getCode())) {

                logger.info(LOG_REF_BOOK_IS_ALREADY_EXISTS, e.getArgs()[0]);
                logger.info(LOG_SKIP_CREATE_REF_BOOK, fileName);
                return null;

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

    private RefBookDataResponse updateAndPublishFromFile(RefBook refBook, RefBookDataRequest request) {

        final FileModel fileModel = request.getFileModel();
        final String fileName = fileModel.getName();

        logger.info("Start draft data loading from file '{}'", fileName);
        try {
            final Draft draft = draftService.create(refBook.getRefBookId(), fileModel);

            final LocalDateTime executedDate = TimeUtils.now();
            final PublishResponse publishResponse = publishDraft(draft.getId(), executedDate);

            logger.info("Finish draft data loading from file '{}'", fileName);

            final Integer refBookId = refBookService.getId(publishResponse.getRefBookCode());
            return new RefBookDataResponse(refBookId, executedDate);

        } catch (NotFoundException | IllegalArgumentException e) {

            final String errorMsg = String.format(LOG_ERROR_DATA_LOADING_WITH_EXCEPTION, fileName);
            logger.error(errorMsg, e);
            throw e;

        } catch (UserException e) {
            logger.error(LOG_ERROR_CREATING_AND_PUBLISHING_DRAFT, fileName);

            final String errorMsg = String.format(LOG_ERROR_DATA_LOADING_WITH_EXCEPTION, fileName);
            logger.error(errorMsg, e);
            throw e;

        } catch (Exception e) {

            final String errorMsg = String.format(LOG_ERROR_DATA_LOADING_WITH_EXCEPTION, fileName);
            logger.error(errorMsg, e);
            throw new UserException(UNKNOWN_ERROR_EXCEPTION_TEXT, e);
        }
    }

    private PublishResponse publishDraft(int draftId, LocalDateTime fromDate) {

        final PublishRequest publishRequest = new PublishRequest(null);
        publishRequest.setFromDate(fromDate);

        return publishService.publish(draftId, publishRequest);
    }
}
