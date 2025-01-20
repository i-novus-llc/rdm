package ru.i_novus.ms.rdm.impl.strategy.publish;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.draft.PostPublishRequest;
import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.model.draft.PublishResponse;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.service.ConflictService;
import ru.i_novus.ms.rdm.api.service.VersionFileService;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.api.util.StringUtils;
import ru.i_novus.ms.rdm.api.util.TimeUtils;
import ru.i_novus.ms.rdm.api.validation.VersionPeriodPublishValidation;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.file.export.PerRowFileGeneratorFactory;
import ru.i_novus.ms.rdm.impl.file.export.VersionDataIterator;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.service.PostPublishService;
import ru.i_novus.ms.rdm.impl.service.RefBookLockService;
import ru.i_novus.ms.rdm.impl.strategy.version.number.VersionNumberStrategy;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.DropDataService;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.singletonList;
import static ru.i_novus.ms.rdm.impl.predicate.RefBookVersionPredicates.*;

@Component
public class DefaultBasePublishStrategy implements BasePublishStrategy {

    private static final String INVALID_VERSION_NAME_EXCEPTION_CODE = "invalid.version.name";
    private static final String INVALID_VERSION_PERIOD_EXCEPTION_CODE = "invalid.version.period";
    private static final String PUBLISHING_DRAFT_STRUCTURE_NOT_FOUND_EXCEPTION_CODE = "publishing.draft.structure.not.found";
    private static final String PUBLISHING_DRAFT_DATA_NOT_FOUND_EXCEPTION_CODE = "publishing.draft.data.not.found";

    @Autowired
    private RefBookVersionRepository versionRepository;

    @Autowired
    private DraftDataService draftDataService;
    @Autowired
    private SearchDataService searchDataService;
    @Autowired
    private DropDataService dropDataService;

    @Autowired
    private RefBookLockService refBookLockService;
    @Autowired
    private VersionService versionService;
    @Autowired
    private ConflictService conflictService;

    @Autowired
    private VersionFileService versionFileService;
    @Autowired
    private VersionNumberStrategy versionNumberStrategy;

    @Autowired
    private VersionValidation versionValidation;
    @Autowired
    private VersionPeriodPublishValidation versionPeriodPublishValidation;

    @Autowired
    private PostPublishService postPublishService;

    @Autowired
    @Qualifier("defaultPublishEndStrategy")
    private PublishEndStrategy publishEndStrategy;

    @Override
    @Transactional
    public PublishResponse publish(RefBookVersionEntity entity, PublishRequest request) {

        // Проверка черновика на возможность публикации
        if (RefBookVersionStatus.PUBLISHED.equals(entity.getStatus()))
            return null;

        validatePublishingDraft(entity);

        // Предварительное заполнение значений
        PublishResponse result = new PublishResponse();

        Integer refBookId = entity.getRefBook().getId();
        String oldStorageCode = entity.getStorageCode();
        String newStorageCode = null;

        refBookLockService.setRefBookPublishing(refBookId);
        try {
            versionValidation.validateOptLockValue(entity.getId(), entity.getOptLockValue(), request.getOptLockValue());

            // Дополнительное заполнение значений с валидацией
            // NB: Получение versionName должно быть в одной транзации с сохранением в версии.
            String versionName = nextVersionNumberOrThrow(request.getVersionName(), refBookId);

            LocalDateTime fromDate = request.getFromDate();
            if (fromDate == null) fromDate = TimeUtils.now();

            LocalDateTime toDate = request.getToDate();
            if (toDate != null && fromDate.isAfter(toDate))
                throw new UserException(INVALID_VERSION_PERIOD_EXCEPTION_CODE);

            versionPeriodPublishValidation.validate(fromDate, toDate, refBookId);

            // Получение старой версии
            RefBookVersionEntity lastPublishedEntity = getLastPublishedVersionEntity(entity);

            // Создание и заполнение хранилища новой версии на основе старой версии и версии-черновика
            String lastStorageCode = lastPublishedEntity != null ? lastPublishedEntity.getStorageCode() : null;
            newStorageCode = draftDataService.applyDraft(lastStorageCode, oldStorageCode, fromDate, toDate);

            // Смена версии-черновика на опубликованную версию
            entity.setStorageCode(newStorageCode);
            entity.setVersion(versionName);
            entity.setStatus(RefBookVersionStatus.PUBLISHED);
            entity.setFromDate(fromDate);
            entity.setToDate(toDate);

            resolveOverlappingPeriodsInFuture(fromDate, toDate, refBookId, entity.getId());

            entity.refreshLastActionDate();
            versionRepository.save(entity);

            // Заполнение результата публикации
            result.setRefBookCode(entity.getRefBook().getCode());
            result.setOldId(lastPublishedEntity != null ? lastPublishedEntity.getId() : null);
            result.setNewId(entity.getId());

            // Обнаружение конфликтов
            // NB: Обнаружение должно быть до удаления хранилища oldStorageCode.

            // Конфликты могут быть только при наличии
            // ссылочных атрибутов со значениями для ранее опубликованной версии.
            if (result.getOldId() != null) {
                conflictService.discoverConflicts(result.getOldId(), result.getNewId());
            }

            // Удаление ненужных хранилищ
            Set<String> droppedDataStorages = new HashSet<>();
            droppedDataStorages.add(oldStorageCode);

            if (lastPublishedEntity != null && lastStorageCode != null
                    && entity.getStructure().storageEquals(lastPublishedEntity.getStructure())) {
                droppedDataStorages.add(lastStorageCode);

                replaceStorageCode(lastStorageCode, newStorageCode);
            }
            dropDataService.drop(droppedDataStorages);

            // Генерация файлов для опубликованной версии
            saveVersionToFiles(entity.getId());

            // Выполнение действий после публикации
            final PostPublishRequest postRequest = new PostPublishRequest();
            postRequest.setRefBookCode(entity.getRefBook().getCode());
            postRequest.setLastStorageCode(lastStorageCode);
            postRequest.setOldStorageCode(oldStorageCode);
            postRequest.setNewStorageCode(newStorageCode);
            postRequest.setFromDate(fromDate);
            postRequest.setToDate(toDate);

            postPublishService.process(postRequest);

        } catch (Exception e) {
            // Откат создания хранилища
            if (!StringUtils.isEmpty(newStorageCode)) {
                dropDataService.drop(newStorageCode);
            }

            throw e;

        } finally {
            refBookLockService.deleteRefBookOperation(refBookId);
        }

        publishEndStrategy.apply(entity, result);

        return result;
    }

    /** Проверка черновика на возможность публикации. */
    private void validatePublishingDraft(RefBookVersionEntity draftEntity) {

        if (draftEntity.hasEmptyStructure())
            throw new UserException(new Message(PUBLISHING_DRAFT_STRUCTURE_NOT_FOUND_EXCEPTION_CODE, draftEntity.getRefBook().getCode()));

        if (!searchDataService.hasData(draftEntity.getStorageCode()))
            throw new UserException(new Message(PUBLISHING_DRAFT_DATA_NOT_FOUND_EXCEPTION_CODE, draftEntity.getRefBook().getCode()));
    }

    public String nextVersionNumberOrThrow(String version, Integer refBookId) {

        if (version == null)
            return versionNumberStrategy.next(refBookId);

        if (!versionNumberStrategy.check(version, refBookId))
            throw new UserException(new Message(INVALID_VERSION_NAME_EXCEPTION_CODE, version));

        return version;
    }

    private RefBookVersionEntity getLastPublishedVersionEntity(RefBookVersionEntity draftVersion) {

        final Integer id = draftVersion.getRefBook().getId();
        return versionRepository.findFirstByRefBookIdAndStatusOrderByFromDateDesc(id, RefBookVersionStatus.PUBLISHED);
    }

    /** Замена старого кода хранилища на новый в версиях справочника. */
    private void replaceStorageCode(String oldStorageCode, String newStorageCode) {

        versionRepository.findByStorageCode(oldStorageCode).forEach(entity -> {
            entity.setStorageCode(newStorageCode);
            versionRepository.save(entity);
        });
    }

    /** Корректировка времён в версиях справочника с перекрывающимся периодом времени. */
    private void resolveOverlappingPeriodsInFuture(LocalDateTime fromDate, LocalDateTime toDate,
                                                   Integer refBookId, Integer draftId) {
        if (toDate == null)
            toDate = MAX_TIMESTAMP;

        Iterable<RefBookVersionEntity> entities = versionRepository.findAll(
                hasOverlappingPeriods(fromDate, toDate)
                        .and(isVersionOfRefBook(refBookId))
                        .and(isPublished())
                        // NB: Exclude error "deleted instance passed to merge".
                        .and(hasVersionId(draftId).not())
        );

        entities.forEach(entity -> {
            if (fromDate.isAfter(entity.getFromDate())) {
                // Опубликованные ранее версии закрываем:
                entity.setToDate(fromDate);
                versionRepository.save(entity);

            } else {
                // Опубликованные позднее - удаляем:
                versionRepository.deleteById(entity.getId());
            }
        });
    }

    private void saveVersionToFiles(Integer versionId) {

        RefBookVersion draftVersion = versionService.getById(versionId);

        for (FileType fileType : PerRowFileGeneratorFactory.getAvailableTypes()) {
            VersionDataIterator dataIterator = new VersionDataIterator(versionService, singletonList(draftVersion.getId()));
            versionFileService.save(draftVersion, fileType, versionFileService.generate(draftVersion, fileType, dataIterator));
        }
    }
}
