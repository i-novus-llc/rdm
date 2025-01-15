package ru.i_novus.ms.rdm.impl.service.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.service.PublishService;
import ru.i_novus.ms.rdm.async.api.service.AsyncOperationMessageService;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;

import java.io.Serializable;

import static ru.i_novus.ms.rdm.api.async.AsyncOperationTypeEnum.PUBLICATION;

@Service
public class AsyncPublishService implements PublishService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncPublishService.class);

    private final RefBookVersionRepository versionRepository;

    private final AsyncOperationMessageService asyncOperationMessageService;

    @Autowired
    @SuppressWarnings("squid:S00107")
    public AsyncPublishService(
            RefBookVersionRepository versionRepository,
            @Lazy AsyncOperationMessageService asyncOperationMessageService
    ) {
        this.versionRepository = versionRepository;

        this.asyncOperationMessageService = asyncOperationMessageService;
    }

    /**
     * Публикация справочника.
     *
     * @param request параметры публикации
     */
    @Override
    @Transactional
    public void publish(Integer draftId, PublishRequest request) {

        final String code = versionRepository.getReferenceById(draftId).getRefBook().getCode();
        asyncOperationMessageService.send(PUBLICATION, code, new Serializable[] {draftId, request});
    }
}