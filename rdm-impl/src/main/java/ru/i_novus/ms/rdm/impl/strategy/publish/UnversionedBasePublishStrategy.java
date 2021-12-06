package ru.i_novus.ms.rdm.impl.strategy.publish;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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

    @Autowired
    private RefBookVersionRepository versionRepository;

    @Autowired
    private RefBookLockService refBookLockService;

    @Autowired
    private VersionValidation versionValidation;

    @Autowired
    @Qualifier("defaultAfterPublishStrategy")
    private AfterPublishStrategy afterPublishStrategy;

    @Override
    @Transactional
    public PublishResponse publish(RefBookVersionEntity entity, PublishRequest request) {

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

        // Запрет разрешения конфликтов
        // (до реализации стратегий разрешения после публикации).
        request.setResolveConflicts(false);

        return result;
    }
}
