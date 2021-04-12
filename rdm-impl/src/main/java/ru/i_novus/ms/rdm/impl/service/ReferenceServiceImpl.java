package ru.i_novus.ms.rdm.impl.service;

import net.n2oapp.platform.i18n.UserException;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.platform.datastorage.temporal.model.DisplayExpression;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.value.ReferenceFieldValue;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.ms.rdm.api.enumeration.ConflictType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookSourceType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.conflict.DeleteRefBookConflictCriteria;
import ru.i_novus.ms.rdm.api.model.conflict.RefBookConflictCriteria;
import ru.i_novus.ms.rdm.api.model.draft.Draft;
import ru.i_novus.ms.rdm.api.service.DraftService;
import ru.i_novus.ms.rdm.api.service.ReferenceService;
import ru.i_novus.ms.rdm.api.util.PageIterator;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.entity.RefBookConflictEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.queryprovider.RefBookConflictQueryProvider;
import ru.i_novus.ms.rdm.impl.repository.RefBookConflictRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.util.ReferrerEntityIteratorProvider;

import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;

@Primary
@Service
@SuppressWarnings("unused")
public class ReferenceServiceImpl implements ReferenceService {

    private static final String VERSION_IS_NOT_LAST_PUBLISHED_EXCEPTION_CODE = "version.is.not.last.published";
    private static final String OPTIMISTIC_LOCK_ERROR_EXCEPTION_CODE = "optimistic.lock.error";

    private RefBookVersionRepository versionRepository;
    private RefBookConflictRepository conflictRepository;
    private RefBookConflictQueryProvider conflictQueryProvider;

    private DraftDataService draftDataService;

    private DraftService draftService;

    private VersionValidation versionValidation;

    @Autowired
    @SuppressWarnings("squid:S00107")
    public ReferenceServiceImpl(RefBookVersionRepository versionRepository,
                                RefBookConflictRepository conflictRepository,
                                RefBookConflictQueryProvider conflictQueryProvider,
                                DraftDataService draftDataService,
                                DraftService draftService,
                                VersionValidation versionValidation) {
        this.versionRepository = versionRepository;
        this.conflictRepository = conflictRepository;
        this.conflictQueryProvider = conflictQueryProvider;

        this.draftDataService = draftDataService;

        this.draftService = draftService;

        this.versionValidation = versionValidation;
    }

    /**
     * Обновление ссылок в справочнике по таблице конфликтов.
     *
     * @param referrerVersionId идентификатор версии справочника, который ссылается
     * @param optLockValue      значение оптимистической блокировки версии
     */
    @Override
    @Transactional
    public void refreshReferrer(Integer referrerVersionId, Integer optLockValue) {

        versionValidation.validateVersionExists(referrerVersionId);

        RefBookVersionEntity referrerEntity = getOrCreateDraftEntity(referrerVersionId);

        if (Objects.equals(referrerVersionId, referrerEntity.getId())) {
            versionValidation.validateOptLockValue(referrerVersionId, referrerEntity.getOptLockValue(), optLockValue);
        }

        List<Structure.Reference> references = referrerEntity.getStructure().getReferences();
        if (isEmpty(references))
            return;

        references.stream()
                .filter(reference -> BooleanUtils.isNotTrue(
                        conflictRepository.hasReferrerConflict(referrerVersionId, reference.getAttribute(),
                                ConflictType.DISPLAY_DAMAGED, RefBookVersionStatus.PUBLISHED)
                ))
                .forEach(reference -> {
            refreshReference(referrerEntity, reference, ConflictType.UPDATED);
            refreshReference(referrerEntity, reference, ConflictType.ALTERED);
        });

        forceUpdateOptLockValue(referrerEntity);
    }

    /**
     * Обновление ссылок в связанных справочниках по таблице конфликтов.
     *
     * @param refBookCode код справочника, на который ссылаются
     */
    @Override
    @Transactional
    public void refreshLastReferrers(String refBookCode) {
        new ReferrerEntityIteratorProvider(versionRepository, refBookCode, RefBookSourceType.LAST_VERSION)
                .iterate().forEachRemaining(
                referrers -> referrers.forEach(referrer -> refreshReferrer(referrer.getId(), null)
                )
        );
    }

    /**
     * Получение или создание entity версии-черновика справочника.
     *
     * @param versionId версия справочника
     * @return Сущность версии-черновика справочника
     */
    private RefBookVersionEntity getOrCreateDraftEntity(Integer versionId) {

        RefBookVersionEntity versionEntity = versionRepository.getOne(versionId);
        if (versionEntity.isDraft())
            return versionEntity;

        RefBookVersionEntity refLastEntity =
                versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(
                        versionEntity.getRefBook().getCode(),
                        RefBookVersionStatus.PUBLISHED
                );
        if (refLastEntity != null && !refLastEntity.getId().equals(versionId))
            throw new RdmException(VERSION_IS_NOT_LAST_PUBLISHED_EXCEPTION_CODE);

        // Изменение данных возможно только в черновике.
        Draft draft = draftService.createFromVersion(versionId);
        return versionRepository.getOne(draft.getId());
    }

    /**
     * Обновление заданной ссылки в справочнике по таблице конфликтов.
     *
     * @param referrerEntity версия справочника, который ссылается
     * @param reference      поле справочника, которое ссылается
     * @param conflictType   тип конфликта
     */
    private void refreshReference(RefBookVersionEntity referrerEntity, Structure.Reference reference, ConflictType conflictType) {

        List<RefBookVersionEntity> publishedEntities = conflictRepository.findRefreshingPublishedVersions(
                referrerEntity.getId(), reference.getAttribute(), conflictType, RefBookVersionStatus.PUBLISHED
        );
        publishedEntities.forEach(publishedEntity -> refreshReference(referrerEntity, publishedEntity, reference, conflictType));

        DeleteRefBookConflictCriteria deleteCriteria = new DeleteRefBookConflictCriteria();
        deleteCriteria.setReferrerVersionId(referrerEntity.getId());
        deleteCriteria.setRefFieldCode(reference.getAttribute());
        deleteCriteria.setConflictType(conflictType);
        conflictQueryProvider.delete(deleteCriteria);
    }

    /**
     * Обновление заданной ссылки в справочнике,
     * связанном с указанным справочником, по таблице конфликтов.
     *
     * @param referrerEntity  версия справочника, который ссылается
     * @param publishedEntity версия справочника, на который ссылаются
     * @param reference       поле справочника, которое ссылается
     * @param conflictType    тип конфликта
     */
    private void refreshReference(RefBookVersionEntity referrerEntity,
                                  RefBookVersionEntity publishedEntity,
                                  Structure.Reference reference,
                                  ConflictType conflictType) {

        Structure.Attribute referenceAttribute = reference.findReferenceAttribute(publishedEntity.getStructure());

        Reference updatedReference = new Reference(
                publishedEntity.getStorageCode(),
                publishedEntity.getFromDate(), // SYS_PUBLISH_TIME is not exist for draft
                referenceAttribute.getCode(),
                new DisplayExpression(reference.getDisplayExpression()),
                null, // Old value must not changed
                null // Display value will be recalculated
        );
        ReferenceFieldValue fieldValue = new ReferenceFieldValue(reference.getAttribute(), updatedReference);

        RefBookConflictCriteria criteria = new RefBookConflictCriteria(referrerEntity.getId(),
                publishedEntity.getId(), reference.getAttribute(), conflictType);
        criteria.setOrders(RefBookConflictQueryProvider.getSortRefBookConflicts());
        criteria.setPageSize(RefBookConflictQueryProvider.REF_BOOK_CONFLICT_PAGE_SIZE);

        PageIterator<RefBookConflictEntity, RefBookConflictCriteria> pageIterator = new PageIterator<>(conflictQueryProvider::search, criteria);
        pageIterator.forEachRemaining(page -> {
            List<Object> systemIds = page.getContent().stream()
                    .map(RefBookConflictEntity::getRefRecordId)
                    .collect(toList());

            draftDataService.updateReferenceInRows(referrerEntity.getStorageCode(), fieldValue, systemIds);
        });
    }

    /** Принудительное обновление значения оптимистической блокировки версии. */
    private void forceUpdateOptLockValue(RefBookVersionEntity versionEntity) {
        try {
            versionEntity.refreshLastActionDate();
            versionRepository.save(versionEntity);

        } catch (ObjectOptimisticLockingFailureException e) {
            throw new UserException(OPTIMISTIC_LOCK_ERROR_EXCEPTION_CODE, e);
        }
    }
}