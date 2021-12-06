package ru.i_novus.ms.rdm.impl.strategy.publish;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.model.draft.PublishResponse;
import ru.i_novus.ms.rdm.api.util.TimeUtils;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.service.RefBookLockService;

import java.time.LocalDateTime;

@Component
public class UnversionedBasePublishStrategy implements BasePublishStrategy {

    private final RefBookVersionRepository versionRepository;

    private final RefBookLockService refBookLockService;

    private final VersionValidation versionValidation;

    private final AfterPublishStrategy afterPublishStrategy;

    public UnversionedBasePublishStrategy(
            RefBookVersionRepository versionRepository,
            RefBookLockService refBookLockService,
            VersionValidation versionValidation,
            @Qualifier("defaultAfterPublishStrategy") AfterPublishStrategy afterPublishStrategy
    ) {
        this.versionRepository = versionRepository;

        this.refBookLockService = refBookLockService;

        this.versionValidation = versionValidation;

        this.afterPublishStrategy = afterPublishStrategy;
    }

    @Override
    @Transactional
    public PublishResponse publish(RefBookVersionEntity entity, PublishRequest request) {

        // Проверка черновика на возможность публикации
        if (RefBookVersionStatus.PUBLISHED.equals(entity.getStatus()))
            return null;

        // Предварительное заполнение значений
        PublishResponse result = new PublishResponse();

        Integer refBookId = entity.getRefBook().getId();

        refBookLockService.setRefBookPublishing(refBookId);
        try {
            versionValidation.validateOptLockValue(entity.getId(), entity.getOptLockValue(), request.getOptLockValue());

            LocalDateTime fromDate = request.getFromDate();
            if (fromDate == null) fromDate = TimeUtils.now();

            // Изменение версии
            entity.setFromDate(fromDate);

            entity.refreshLastActionDate();
            versionRepository.save(entity);

            // Заполнение результата публикации
            result.setRefBookCode(entity.getRefBook().getCode());
            result.setOldId(entity.getId());
            result.setNewId(entity.getId());

        } finally {
            refBookLockService.deleteRefBookOperation(refBookId);
        }

        afterPublishStrategy.apply(entity, result);

        return result;
    }
}
