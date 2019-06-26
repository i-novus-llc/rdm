package ru.inovus.ms.rdm.service;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.DropDataService;
import ru.inovus.ms.rdm.entity.*;
import ru.inovus.ms.rdm.enumeration.FileType;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.file.export.*;
import ru.inovus.ms.rdm.model.version.RefBookVersion;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.service.api.ConflictService;
import ru.inovus.ms.rdm.service.api.PublishService;
import ru.inovus.ms.rdm.service.api.VersionFileService;
import ru.inovus.ms.rdm.service.api.VersionService;
import ru.inovus.ms.rdm.util.TimeUtils;
import ru.inovus.ms.rdm.util.VersionNumberStrategy;
import ru.inovus.ms.rdm.validation.VersionPeriodPublishValidation;
import ru.inovus.ms.rdm.validation.VersionValidation;

import java.time.LocalDateTime;
import java.util.*;

import static java.util.Collections.singletonList;
import static ru.inovus.ms.rdm.repositiory.RefBookVersionPredicates.*;

@Primary
@Service
public class PublishServiceImpl implements PublishService {

    private RefBookVersionRepository versionRepository;

    private DraftDataService draftDataService;
    private DropDataService dropDataService;

    private RefBookLockService refBookLockService;
    private VersionService versionService;
    private ConflictService conflictService;

    private VersionFileService versionFileService;
    private VersionNumberStrategy versionNumberStrategy;

    private VersionValidation versionValidation;
    private VersionPeriodPublishValidation versionPeriodPublishValidation;

    private static final String INVALID_VERSION_NAME_EXCEPTION_CODE = "invalid.version.name";
    private static final String INVALID_VERSION_PERIOD_EXCEPTION_CODE = "invalid.version.period";

    @Autowired
    @SuppressWarnings("all")
    public PublishServiceImpl(RefBookVersionRepository versionRepository,
                              DraftDataService draftDataService, DropDataService dropDataService,
                              RefBookLockService refBookLockService, VersionService versionService, ConflictService conflictService,
                              VersionFileService versionFileService, VersionNumberStrategy versionNumberStrategy,
                              VersionValidation versionValidation, VersionPeriodPublishValidation versionPeriodPublishValidation) {
        this.versionRepository = versionRepository;

        this.draftDataService = draftDataService;
        this.dropDataService = dropDataService;

        this.refBookLockService = refBookLockService;
        this.versionService = versionService;
        this.conflictService = conflictService;

        this.versionFileService = versionFileService;
        this.versionNumberStrategy = versionNumberStrategy;

        this.versionValidation = versionValidation;
        this.versionPeriodPublishValidation = versionPeriodPublishValidation;
    }

    /**
     * Публикация черновика справочника.
     *
     * @param draftId                    идентификатор черновика справочника
     * @param versionName                версия, под которой публикуется черновик
     *                                   (если не указано, используется встроенная нумерация)
     * @param fromDate                   дата начала действия опубликованной версии
     * @param toDate                     дата окончания действия опубликованной версии
     * @param resolveConflicts признак обработка разрешаемых конфликтов
     */
    @Override
    // NB: Добавление Transactional приводит к падению в тестах.
    public void publish(Integer draftId, String versionName,
                        LocalDateTime fromDate, LocalDateTime toDate,
                        boolean resolveConflicts) {

        versionValidation.validateDraft(draftId);

        RefBookVersionEntity draftEntity = versionRepository.findById(draftId).orElseThrow();
        Integer refBookId = draftEntity.getRefBook().getId();

        refBookLockService.setRefBookPublishing(refBookId);
        try {
            if (versionName == null) {
                versionName = versionNumberStrategy.next(refBookId);

            } else if (!versionNumberStrategy.check(versionName, refBookId)) {
                throw new UserException(new Message(INVALID_VERSION_NAME_EXCEPTION_CODE, versionName));
            }

            if (fromDate == null) fromDate = TimeUtils.now();
            if (toDate != null && fromDate.isAfter(toDate))
                throw new UserException(INVALID_VERSION_PERIOD_EXCEPTION_CODE);

            versionPeriodPublishValidation.validate(fromDate, toDate, refBookId);

            RefBookVersionEntity lastPublishedVersion = getLastPublishedVersionEntity(draftEntity);
            String storageCode = draftDataService.applyDraft(
                    lastPublishedVersion != null ? lastPublishedVersion.getStorageCode() : null,
                    draftEntity.getStorageCode(),
                    fromDate,
                    toDate
            );

            Set<String> dataStorageToDelete = new HashSet<>();
            dataStorageToDelete.add(draftEntity.getStorageCode());

            draftEntity.setStorageCode(storageCode);
            draftEntity.setVersion(versionName);
            draftEntity.setStatus(RefBookVersionStatus.PUBLISHED);
            draftEntity.setFromDate(fromDate);
            draftEntity.setToDate(toDate);
            resolveOverlappingPeriodsInFuture(fromDate, toDate, refBookId);
            versionRepository.save(draftEntity);

            if (lastPublishedVersion != null && lastPublishedVersion.getStorageCode() != null
                    && draftEntity.getStructure().storageEquals(lastPublishedVersion.getStructure())) {
                dataStorageToDelete.add(lastPublishedVersion.getStorageCode());
                versionRepository.findByStorageCode(lastPublishedVersion.getStorageCode()).stream()
                        .peek(version -> version.setStorageCode(storageCode))
                        .forEach(versionRepository::save);
            }
            dropDataService.drop(dataStorageToDelete);

            RefBookVersion versionModel = versionService.getById(draftId);
            for (FileType fileType : PerRowFileGeneratorFactory.getAvailableTypes()) {
                VersionDataIterator dataIterator = new VersionDataIterator(versionService, singletonList(versionModel.getId()));
                versionFileService.save(versionModel, fileType,
                        versionFileService.generate(versionModel, fileType, dataIterator));
            }

            if (lastPublishedVersion != null) {
                // NB: Конфликты могут быть только при наличии
                // ссылочных атрибутов со значениями для ранее опубликованной версии.
                conflictService.dropPublishedConflicts(lastPublishedVersion.getRefBook().getId());
                conflictService.discoverConflicts(lastPublishedVersion.getId(), draftId);

                if (resolveConflicts) {
                    conflictService.refreshLastReferrersByPrimary(lastPublishedVersion.getRefBook().getCode());
                }
            }

        } finally {
            refBookLockService.deleteRefBookAction(refBookId);
        }
    }

    private RefBookVersionEntity getLastPublishedVersionEntity(RefBookVersionEntity draftVersion) {
        return versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(draftVersion.getRefBook().getCode(), RefBookVersionStatus.PUBLISHED);
    }

    private void resolveOverlappingPeriodsInFuture(LocalDateTime fromDate, LocalDateTime toDate, Integer refBookId) {

        if (toDate == null)
            toDate = MAX_TIMESTAMP;

        Iterable<RefBookVersionEntity> versions = versionRepository.findAll(
                hasOverlappingPeriods(fromDate, toDate)
                        .and(isVersionOfRefBook(refBookId))
                        .and(isPublished())
        );
        versions.forEach(version -> {
            if (fromDate.isAfter(version.getFromDate())) {
                version.setToDate(fromDate);
                versionRepository.save(version);
            } else {
                versionRepository.deleteById(version.getId());
            }
        });
    }
}