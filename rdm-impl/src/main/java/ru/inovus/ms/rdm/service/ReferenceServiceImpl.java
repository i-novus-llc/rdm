package ru.inovus.ms.rdm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.platform.datastorage.temporal.model.DisplayExpression;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.value.ReferenceFieldValue;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.inovus.ms.rdm.entity.RefBookConflictEntity;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.*;
import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.model.conflict.DeleteRefBookConflictCriteria;
import ru.inovus.ms.rdm.model.conflict.RefBookConflictCriteria;
import ru.inovus.ms.rdm.model.draft.Draft;
import ru.inovus.ms.rdm.queryprovider.RefBookConflictQueryProvider;
import ru.inovus.ms.rdm.repository.RefBookConflictRepository;
import ru.inovus.ms.rdm.repository.RefBookVersionRepository;
import ru.inovus.ms.rdm.service.api.*;
import ru.inovus.ms.rdm.util.PageIterator;
import ru.inovus.ms.rdm.util.ReferrerEntityIteratorProvider;
import ru.inovus.ms.rdm.validation.VersionValidation;

import java.util.List;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;

@Primary
@Service
@SuppressWarnings("unused")
public class ReferenceServiceImpl implements ReferenceService {

    private static final String VERSION_IS_NOT_LAST_PUBLISHED_EXCEPTION_CODE = "version.is.not.last.published";

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
     * @param referrerVersionId идентификатор версии справочника
     */
    @Override
    @Transactional
    public void refreshReferrer(Integer referrerVersionId) {

        versionValidation.validateVersionExists(referrerVersionId);

        RefBookVersionEntity referrerEntity = getOrCreateDraftEntity(referrerVersionId);
        List<Structure.Reference> references = referrerEntity.getStructure().getReferences();
        if (isEmpty(references))
            return;

        references.forEach(reference -> {
            refreshReference(referrerEntity, reference, ConflictType.UPDATED);
            refreshReference(referrerEntity, reference, ConflictType.ALTERED);
        });
    }

    /**
     * Получение или создание entity версии-черновика справочника.
     *
     * @param versionId версия справочника
     * @return Entity версии-черновика справочника
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

        Function<RefBookConflictCriteria, Page<RefBookConflictEntity>> pageSource = conflictQueryProvider::search;
        PageIterator<RefBookConflictEntity, RefBookConflictCriteria> pageIterator = new PageIterator<>(pageSource, criteria);
        pageIterator.forEachRemaining(page -> {
            List<Object> systemIds = page.getContent().stream()
                    .map(RefBookConflictEntity::getRefRecordId)
                    .collect(toList());

            draftDataService.updateReferenceInRows(referrerEntity.getStorageCode(), fieldValue, systemIds);
        });
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
                        referrers -> referrers.forEach(referrer -> refreshReferrer(referrer.getId())
                        )
        );
    }
}