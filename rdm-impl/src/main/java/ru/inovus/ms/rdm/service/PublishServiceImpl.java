package ru.inovus.ms.rdm.service;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.DropDataService;
import ru.inovus.ms.rdm.entity.*;
import ru.inovus.ms.rdm.enumeration.FileType;
import ru.inovus.ms.rdm.enumeration.RefBookSourceType;
import ru.inovus.ms.rdm.enumeration.RefBookStatusType;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.file.export.*;
import ru.inovus.ms.rdm.model.conflict.DeleteRefBookConflictCriteria;
import ru.inovus.ms.rdm.model.conflict.RefBookConflict;
import ru.inovus.ms.rdm.model.conflict.RefBookConflictCriteria;
import ru.inovus.ms.rdm.model.version.RefBookVersion;
import ru.inovus.ms.rdm.model.version.ReferrerVersionCriteria;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.service.api.*;
import ru.inovus.ms.rdm.util.TimeUtils;
import ru.inovus.ms.rdm.util.VersionNumberStrategy;
import ru.inovus.ms.rdm.validation.VersionPeriodPublishValidation;
import ru.inovus.ms.rdm.validation.VersionValidation;

import java.time.LocalDateTime;
import java.util.*;

import static java.util.Collections.singletonList;
import static ru.inovus.ms.rdm.predicate.RefBookVersionPredicates.*;

@Primary
@Service
public class PublishServiceImpl implements PublishService {

    private static final int REF_BOOK_VERSION_PAGE_SIZE = 100;

    private static final String INVALID_VERSION_NAME_EXCEPTION_CODE = "invalid.version.name";
    private static final String INVALID_VERSION_PERIOD_EXCEPTION_CODE = "invalid.version.period";

    private RefBookVersionRepository versionRepository;

    private DraftDataService draftDataService;
    private DropDataService dropDataService;

    private RefBookService refBookService;
    private RefBookLockService refBookLockService;
    private VersionService versionService;
    private ConflictService conflictService;

    private VersionFileService versionFileService;
    private VersionNumberStrategy versionNumberStrategy;

    private VersionValidation versionValidation;
    private VersionPeriodPublishValidation versionPeriodPublishValidation;

    @Autowired
    @SuppressWarnings("squid:S00107")
    public PublishServiceImpl(RefBookVersionRepository versionRepository,
                              DraftDataService draftDataService, DropDataService dropDataService,
                              RefBookService refBookService, RefBookLockService refBookLockService,
                              VersionService versionService, ConflictService conflictService,
                              VersionFileService versionFileService, VersionNumberStrategy versionNumberStrategy,
                              VersionValidation versionValidation, VersionPeriodPublishValidation versionPeriodPublishValidation) {
        this.versionRepository = versionRepository;

        this.draftDataService = draftDataService;
        this.dropDataService = dropDataService;

        this.refBookService = refBookService;
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
     * @param draftId          идентификатор черновика справочника
     * @param versionName      версия, под которой публикуется черновик
     *                         (если не указано, используется встроенная нумерация)
     * @param fromDate         дата начала действия опубликованной версии
     * @param toDate           дата окончания действия опубликованной версии
     * @param resolveConflicts признак разрешения конфликтов
     */
    @Override
    @Transactional
    // NB: Use PublishCriteria, required for publishNonConflictReferrers.
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

            resolveOverlappingPeriodsInFuture(fromDate, toDate, refBookId, draftEntity.getId());

            versionRepository.save(draftEntity);

            if (lastPublishedVersion != null
                    && lastPublishedVersion.getStorageCode() != null
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

            // NB: Конфликты могут быть только при наличии
            // ссылочных атрибутов со значениями для ранее опубликованной версии.
            if (lastPublishedVersion != null) {
                conflictService.discoverConflicts(lastPublishedVersion.getId(), draftId);
                processDiscoveredConflicts(lastPublishedVersion, draftId, resolveConflicts);
            }

        } finally {
            refBookLockService.deleteRefBookAction(refBookId);
        }
    }

    private RefBookVersionEntity getLastPublishedVersionEntity(RefBookVersionEntity draftVersion) {
        return versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(draftVersion.getRefBook().getCode(), RefBookVersionStatus.PUBLISHED);
    }

    private void resolveOverlappingPeriodsInFuture(LocalDateTime fromDate, LocalDateTime toDate,
                                                   Integer refBookId, Integer draftId) {

        if (toDate == null)
            toDate = MAX_TIMESTAMP;

        Iterable<RefBookVersionEntity> versions = versionRepository.findAll(
                hasOverlappingPeriods(fromDate, toDate)
                        .and(isVersionOfRefBook(refBookId))
                        .and(isPublished())
                        // NB: Exclude error "deleted instance passed to merge".
                        .and(hasVersionId(draftId).not())
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

    private void processDiscoveredConflicts(RefBookVersionEntity oldVersion, Integer newVersionId, boolean resolveConflicts) {

        deleteOldConflicts(oldVersion.getRefBook().getId(), newVersionId);

        if (resolveConflicts) {
            resolveReferrerConflicts(oldVersion.getRefBook().getCode());
            publishNonConflictReferrers(oldVersion.getRefBook().getCode(), newVersionId);
        }
    }

    /**
     * Удаление конфликтов для всех версий справочника, на который ссылаются,
     * кроме указанной версии справочника, на которую будут ссылаться.
     *
     * @param refBookId        идентификатор справочника, на который ссылаются
     * @param excludeVersionId идентификатор исключаемой версии справочника
     */
    private void deleteOldConflicts(Integer refBookId, Integer excludeVersionId) {
        DeleteRefBookConflictCriteria deleteCriteria = new DeleteRefBookConflictCriteria();
        deleteCriteria.setPublishedVersionRefBookId(refBookId);
        deleteCriteria.setExcludedPublishedVersionId(excludeVersionId);
        conflictService.delete(deleteCriteria);
    }

    /**
     * Обработка разрешаемых конфликтов.
     *
     * @param refBookCode код справочника, на который ссылаются
     */
    private void resolveReferrerConflicts(String refBookCode) {
        conflictService.refreshLastReferrersByPrimary(refBookCode);
    }

    /**
     * Публикация бесконфликтных справочников, который ссылаются на указанный справочник.
     *
     * @param refBookCode        код справочника, на который ссылаются
     * @param publishedVersionId идентификатор версии справочника
     */
    private void publishNonConflictReferrers(String refBookCode, Integer publishedVersionId) {

        ReferrerVersionCriteria criteria = new ReferrerVersionCriteria(refBookCode, RefBookStatusType.USED, RefBookSourceType.DRAFT);
        criteria.firstPageNumber(REF_BOOK_VERSION_PAGE_SIZE);

        Page<RefBookVersion> page = refBookService.searchReferrerVersions(criteria);
        while (!page.getContent().isEmpty()) {
            page.getContent().forEach(referrerVersion -> {
                if (notExistsConflict(referrerVersion.getId(), publishedVersionId))
                    publish(referrerVersion.getId(), null, null, null, false);
            });

            criteria.nextPageNumber();
            page = refBookService.searchReferrerVersions(criteria);
        }
    }

    /**
     * Проверка на отсутствие конфликтов версий справочников.
     *
     * @param referrerVersionId  идентификатор версии, которая ссылается
     * @param publishedVersionId идентификатор версии, на которую ссылаются
     * @return Отсутствие конфликта
     */
    private boolean notExistsConflict(Integer referrerVersionId, Integer publishedVersionId) {
        RefBookConflictCriteria criteria = new RefBookConflictCriteria();
        criteria.setReferrerVersionId(referrerVersionId);
        criteria.setPublishedVersionId(publishedVersionId);

        criteria.firstPageNumber(1);
        Page<RefBookConflict> conflicts = conflictService.search(criteria);
        return conflicts.getContent().isEmpty();
    }
}