package ru.inovus.ms.rdm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ru.inovus.ms.rdm.enumeration.ConflictType;
import ru.inovus.ms.rdm.model.refbook.RefBook;
import ru.inovus.ms.rdm.model.version.RefBookVersion;
import ru.inovus.ms.rdm.model.UiRefBookPublish;
import ru.inovus.ms.rdm.service.api.ConflictService;
import ru.inovus.ms.rdm.service.api.PublishService;
import ru.inovus.ms.rdm.service.api.RefBookService;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;
import static ru.inovus.ms.rdm.util.StringUtils.addDoubleQuotes;

@Controller
@SuppressWarnings("unused")
public class RefBookPublishController {

    private static final String PASSPORT_ATTRIBUTE_NAME = "name";
    private static final String REFERRER_NAME_SEPARATOR = ", ";

    private RefBookService refBookService;
    private PublishService publishService;
    private ConflictService conflictService;

    @Autowired
    public RefBookPublishController(RefBookService refBookService,
                                    PublishService publishService, ConflictService conflictService) {
        this.refBookService = refBookService;

        this.publishService = publishService;
        this.conflictService = conflictService;
    }

    public UiRefBookPublish getByVersionId(Integer versionId) {

        RefBook refBook = refBookService.getByVersionId(versionId);

        UiRefBookPublish uiRefBookPublish = new UiRefBookPublish(refBook);
        Map<String, String> conflictingReferrerNames =
                Stream.of(ConflictType.values())
                        .collect(toMap(ConflictType::name,
                                conflictType -> getConflictingReferrerNames(versionId, conflictType)
                                )
                        );
        uiRefBookPublish.setConflictingReferrerNames(conflictingReferrerNames);

        return uiRefBookPublish;
    }

    /**
     * Публикация черновика справочника.
     *
     * @param draftId идентификатор черновика
     */
    public void publishDraft(Integer draftId) {
        publishService.publish(draftId, null, null, null, false);
    }

    /**
     * Публикация черновика справочника с обновлением ссылок.
     *
     * @param draftId идентификатор черновика
     */
    public void publishAndRefresh(Integer draftId) {
        publishService.publish(draftId, null, null, null, true);
    }

    /**
     * Получение названий справочников, имеющих конфликтные ссылки на версию.
     *
     * @param versionId    идентификатор версии справочника
     * @param conflictType тип конфликта
     * @return Названия справочников (через запятую)
     */
    private String getConflictingReferrerNames(Integer versionId, ConflictType conflictType) {
        return conflictService.getConflictingReferrers(versionId, conflictType)
                .stream()
                .map(this::getReferrerDisplayName)
                .collect(Collectors.joining(REFERRER_NAME_SEPARATOR));
    }

    /**
     * Получение названия справочника для отображения.
     *
     * @param version версия справочника
     * @return Отображаемое название
     */
    private String getReferrerDisplayName(RefBookVersion version) {
        Map<String, String> passport = version.getPassport();
        return (passport != null && passport.get(PASSPORT_ATTRIBUTE_NAME) != null)
                ? addDoubleQuotes(passport.get(PASSPORT_ATTRIBUTE_NAME))
                : version.getCode();
    }
}