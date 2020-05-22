package ru.inovus.ms.rdm.impl.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.api.async.AsyncOperation;
import ru.inovus.ms.rdm.api.enumeration.RefBookSourceType;
import ru.inovus.ms.rdm.api.model.conflict.RefBookConflict;
import ru.inovus.ms.rdm.api.model.conflict.RefBookConflictCriteria;
import ru.inovus.ms.rdm.api.model.draft.PublishRequest;
import ru.inovus.ms.rdm.api.model.draft.PublishResponse;
import ru.inovus.ms.rdm.api.service.ConflictService;
import ru.inovus.ms.rdm.api.service.DraftPublishService;
import ru.inovus.ms.rdm.api.service.PublishService;
import ru.inovus.ms.rdm.api.service.ReferenceService;
import ru.inovus.ms.rdm.impl.async.AsyncOperationQueue;
import ru.inovus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.inovus.ms.rdm.impl.util.ReferrerEntityIteratorProvider;

import java.util.UUID;

@Primary
@Service
public class PublishServiceImpl implements PublishService {

    private RefBookVersionRepository versionRepository;

    private DraftPublishService draftPublishService;

    private ConflictService conflictService;
    private ReferenceService referenceService;

    @Autowired
    private AsyncOperationQueue queue;

    @Autowired
    @SuppressWarnings("squid:S00107")
    public PublishServiceImpl(RefBookVersionRepository versionRepository,
                              DraftPublishService draftPublishService,
                              ConflictService conflictService, ReferenceService referenceService) {
        this.versionRepository = versionRepository;

        this.draftPublishService = draftPublishService;

        this.conflictService = conflictService;
        this.referenceService = referenceService;
    }

    /**
     * Публикация справочника.
     *
     * @param request параметры публикации
     */
    @Override
    public void publish(PublishRequest request) {

        PublishResponse response = draftPublishService.publish(request);
        if (response == null)
            return;

        // Конфликты могут быть только при наличии
        // ссылочных атрибутов со значениями для ранее опубликованной версии.
        if (response.getOldId() == null)
            return;

        conflictService.discoverConflicts(response.getOldId(), response.getNewId());

        if (request.getResolveConflicts()) {
            referenceService.refreshLastReferrers(response.getRefBookCode());
            publishNonConflictReferrers(response.getRefBookCode(), response.getNewId());
        }
    }

    @Override
    @Transactional
    public UUID publishAsync(PublishRequest request) {
        String code = versionRepository.getOne(request.getDraftId()).getRefBook().getCode();
        return queue.add(AsyncOperation.PUBLICATION, code, new Object[] { request });
    }

    /**
     * Публикация бесконфликтных справочников, который ссылаются на указанный справочник.
     *
     * @param refBookCode        код справочника, на который ссылаются
     * @param publishedVersionId идентификатор версии справочника, на который ссылаются
     */
    private void publishNonConflictReferrers(String refBookCode, Integer publishedVersionId) {

        new ReferrerEntityIteratorProvider(versionRepository, refBookCode, RefBookSourceType.DRAFT)
                .iterate().forEachRemaining(referrers ->
                referrers.stream()
                        .filter(referrer ->
                                isConflictsEmpty(referrer.getId(), publishedVersionId))
                        .forEach(referrer -> publish(new PublishRequest(referrer.getId())))
        );
    }

    /**
     * Проверка на отсутствие конфликтов версий справочников.
     *
     * @param referrerVersionId  идентификатор версии, которая ссылается
     * @param publishedVersionId идентификатор версии, на которую ссылаются
     * @return Отсутствие конфликта
     */
    private boolean isConflictsEmpty(Integer referrerVersionId, Integer publishedVersionId) {

        RefBookConflictCriteria criteria = new RefBookConflictCriteria(referrerVersionId, publishedVersionId);
        criteria.setPageSize(1);

        Page<RefBookConflict> conflicts = conflictService.search(criteria);
        return conflicts.getContent().isEmpty();
    }
}