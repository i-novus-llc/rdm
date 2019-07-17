package ru.inovus.ms.rdm.service;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
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
import ru.inovus.ms.rdm.model.conflict.RefBookConflict;
import ru.inovus.ms.rdm.model.conflict.RefBookConflictCriteria;
import ru.inovus.ms.rdm.model.version.RefBookVersion;
import ru.inovus.ms.rdm.model.version.ReferrerVersionCriteria;
import ru.inovus.ms.rdm.repository.RefBookVersionRepository;
import ru.inovus.ms.rdm.service.api.*;
import ru.inovus.ms.rdm.util.PageIterator;
import ru.inovus.ms.rdm.util.TimeUtils;
import ru.inovus.ms.rdm.util.VersionNumberStrategy;
import ru.inovus.ms.rdm.validation.VersionPeriodPublishValidation;
import ru.inovus.ms.rdm.validation.VersionValidation;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Collections.singletonList;
import static ru.inovus.ms.rdm.predicate.RefBookVersionPredicates.*;

@Primary
@Service
public class PublishServiceImpl implements PublishService {

    private static final int REF_BOOK_VERSION_PAGE_SIZE = 100;

    private static final String VERSION_ID_SORT_PROPERTY = "id";

    private static final List<Sort.Order> SORT_REFERRER_VERSIONS = singletonList(
            new Sort.Order(Sort.Direction.ASC, VERSION_ID_SORT_PROPERTY)
    );

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

            RefBookVersionEntity lastPublishedEntity = getLastPublishedVersionEntity(draftEntity);
            String lastStorageCode = lastPublishedEntity != null ? lastPublishedEntity.getStorageCode() : null;
            String newStorageCode = draftDataService.applyDraft(lastStorageCode, draftEntity.getStorageCode(), fromDate, toDate);

            Set<String> droppedDataStorages = new HashSet<>();
            droppedDataStorages.add(draftEntity.getStorageCode());

            draftEntity.setStorageCode(newStorageCode);
            draftEntity.setVersion(versionName);
            draftEntity.setStatus(RefBookVersionStatus.PUBLISHED);
            draftEntity.setFromDate(fromDate);
            draftEntity.setToDate(toDate);

            resolveOverlappingPeriodsInFuture(fromDate, toDate, refBookId, draftEntity.getId());

            versionRepository.save(draftEntity);

            if (lastPublishedEntity != null && lastStorageCode != null
                    && draftEntity.getStructure().storageEquals(lastPublishedEntity.getStructure())) {
                droppedDataStorages.add(lastStorageCode);

                versionRepository.findByStorageCode(lastStorageCode).stream()
                        .peek(entity -> entity.setStorageCode(newStorageCode))
                        .forEach(versionRepository::save);
            }
            dropDataService.drop(droppedDataStorages);

            saveDraftToFiles(draftId);

            // NB: Конфликты могут быть только при наличии
            // ссылочных атрибутов со значениями для ранее опубликованной версии.
            if (lastPublishedEntity != null) {
                conflictService.discoverConflicts(lastPublishedEntity.getId(), draftId);
                processDiscoveredConflicts(lastPublishedEntity, draftId, resolveConflicts);
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

        Iterable<RefBookVersionEntity> entities = versionRepository.findAll(
                hasOverlappingPeriods(fromDate, toDate)
                        .and(isVersionOfRefBook(refBookId))
                        .and(isPublished())
                        // NB: Exclude error "deleted instance passed to merge".
                        .and(hasVersionId(draftId).not())
        );

        entities.forEach(entity -> {
            if (fromDate.isAfter(entity.getFromDate())) {
                entity.setToDate(fromDate);
                versionRepository.save(entity);
            } else {
                versionRepository.deleteById(entity.getId());
            }
        });
    }

    private void saveDraftToFiles(Integer draftId) {

        RefBookVersion draftVersion = versionService.getById(draftId);

        for (FileType fileType : PerRowFileGeneratorFactory.getAvailableTypes()) {
            VersionDataIterator dataIterator = new VersionDataIterator(versionService, singletonList(draftVersion.getId()));
            versionFileService.save(draftVersion, fileType,
                    versionFileService.generate(draftVersion, fileType, dataIterator));
        }
    }

    private void processDiscoveredConflicts(RefBookVersionEntity oldVersion, Integer newVersionId, boolean resolveConflicts) {

        // NB: Old conflicts are not deleted.

        if (resolveConflicts) {
            resolveReferrerConflicts(oldVersion.getRefBook().getCode());
            publishNonConflictReferrers(oldVersion.getRefBook().getCode(), newVersionId);
        }
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

        Consumer<List<RefBookVersion>> consumer = referrers ->
                referrers.forEach(referrerVersion -> {
                    if (notExistsConflict(referrerVersion.getId(), publishedVersionId))
                        publish(referrerVersion.getId(), null, null, null, false);
                });
        processReferrerVersions(refBookCode, RefBookSourceType.DRAFT, consumer);
    }

    /**
     * Проверка на отсутствие конфликтов версий справочников.
     *
     * @param referrerVersionId  идентификатор версии, которая ссылается
     * @param publishedVersionId идентификатор версии, на которую ссылаются
     * @return Отсутствие конфликта
     */
    private boolean notExistsConflict(Integer referrerVersionId, Integer publishedVersionId) {

        RefBookConflictCriteria criteria = new RefBookConflictCriteria(referrerVersionId, publishedVersionId);
        criteria.setPageSize(1);

        Page<RefBookConflict> conflicts = conflictService.search(criteria);
        return conflicts.getContent().isEmpty();
    }

    /**
     * Обработка версий справочников, ссылающихся на указанный справочник.
     *
     * @param refBookCode код справочника, на который ссылаются
     * @param sourceType  тип выбираемых версий справочников
     * @param consumer    обработчик списков версий
     */
    private void processReferrerVersions(String refBookCode, RefBookSourceType sourceType, Consumer<List<RefBookVersion>> consumer) {

        ReferrerVersionCriteria criteria = new ReferrerVersionCriteria(refBookCode, RefBookStatusType.USED, sourceType);
        criteria.setOrders(SORT_REFERRER_VERSIONS);
        criteria.setPageSize(REF_BOOK_VERSION_PAGE_SIZE);

        Function<ReferrerVersionCriteria, Page<RefBookVersion>> pageSource = refBookService::searchReferrerVersions;
        PageIterator<RefBookVersion, ReferrerVersionCriteria> pageIterator = new PageIterator<>(pageSource, criteria);
        pageIterator.forEachRemaining(page -> consumer.accept(page.getContent()));
    }
}