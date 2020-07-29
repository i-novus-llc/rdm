package ru.i_novus.ms.rdm.n2o.service;

import net.n2oapp.platform.i18n.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.api.enumeration.ConflictType;
import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.service.*;
import ru.i_novus.ms.rdm.n2o.model.UiRefBookPublish;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;
import static ru.i_novus.ms.rdm.api.util.StringUtils.addDoubleQuotes;

@Controller
@SuppressWarnings("unused") // used in: publish.*.xml
public class RefBookPublishController {

    private static final Logger logger = LoggerFactory.getLogger(RefBookPublishController.class);

    private static final String PUBLISHING_DRAFT_STRUCTURE_NOT_FOUND_EXCEPTION_CODE = "publishing.draft.structure.not.found";
    private static final String PUBLISHING_DRAFT_DATA_NOT_FOUND_EXCEPTION_CODE = "publishing.draft.data.not.found";

    private static final ConflictType[] CONFLICT_TYPE_VALUES = ConflictType.values();

    private static final String PASSPORT_ATTRIBUTE_NAME = "name";
    private static final String REFERRER_NAME_SEPARATOR = ", ";
    private static final String REFERRER_NAME_LIST_END = ".";

    private RefBookService refBookService;
    private DraftService draftService;
    private PublishService publishService;
    private ConflictService conflictService;

    private Messages messages;

    @Autowired
    public RefBookPublishController(RefBookService refBookService, DraftService draftService,
                                    PublishService publishService, ConflictService conflictService,
                                    Messages messages) {
        this.refBookService = refBookService;
        this.draftService = draftService;

        this.publishService = publishService;
        this.conflictService = conflictService;

        this.messages = messages;
    }

    /**
     * Поиск черновика справочника для публикации.
     *
     * @param versionId    идентификатор версии
     * @param optLockValue значение оптимистической блокировки
     * @return Публикуемый черновик
     */
    public UiRefBookPublish getDraft(Integer versionId, Integer optLockValue) {

        RefBook refBook = refBookService.getByVersionId(versionId);

        UiRefBookPublish uiRefBookPublish = new UiRefBookPublish(refBook);

        String message = checkPublishedDraft(versionId);
        if (!StringUtils.isEmpty(message)) {
            uiRefBookPublish.setErrorMessage(message);
            return uiRefBookPublish;
        }

        try {
            uiRefBookPublish.setConflictingReferrerNames(getConflictingReferrerNames(versionId));

        } catch (Exception e) {
            logger.error("Error on check conflicting referrers", e);
            uiRefBookPublish.setErrorMessage(e.getMessage());
        }

        return uiRefBookPublish;
    }

    /** Проверка публикуемого черновика перед открытием окна публикации. */
    public String checkPublishedDraft(Integer versionId) {

        RefBook refBook = refBookService.getByVersionId(versionId);

        if (refBook.getStructure() == null || refBook.getStructure().isEmpty()) {
            return messages.getMessage(PUBLISHING_DRAFT_STRUCTURE_NOT_FOUND_EXCEPTION_CODE);
        }

        if (!Boolean.TRUE.equals(draftService.hasData(versionId))) {
            return messages.getMessage(PUBLISHING_DRAFT_DATA_NOT_FOUND_EXCEPTION_CODE);
        }

        return null;
    }

    /**
     * Публикация черновика справочника.
     *
     * @param draftId идентификатор черновика
     */
    public UUID publishDraft(Integer draftId, Integer optLockValue) {

        PublishRequest request = new PublishRequest(optLockValue);
        return publishService.publishAsync(draftId, request);
    }

    /**
     * Публикация черновика справочника с обновлением ссылок.
     *
     * @param draftId идентификатор черновика
     */
    public UUID publishAndRefresh(Integer draftId, Integer optLockValue) {

        PublishRequest request = new PublishRequest(optLockValue);
        request.setResolveConflicts(true);
        return publishService.publishAsync(draftId, request);
    }

    /**
     * Получение названий справочников, имеющих конфликтные ссылки на версию.
     *
     * @param versionId идентификатор версии справочника
     * @return Набор названий справочников для каждого типа конфликта
     */
    private Map<String, String> getConflictingReferrerNames(Integer versionId) {

        return Stream.of(CONFLICT_TYPE_VALUES).collect(
                toMap(ConflictType::name,
                        conflictType -> getConflictTypeReferrerNames(versionId, conflictType)
                )
        );
    }

    /**
     * Получение названий справочников, имеющих конфликтные ссылки заданного типа на версию.
     *
     * @param versionId    идентификатор версии справочника
     * @param conflictType тип конфликта
     * @return Названия справочников (через запятую и с точкой в конце)
     */
    private String getConflictTypeReferrerNames(Integer versionId, ConflictType conflictType) {

        String result = conflictService.getConflictingReferrers(versionId, conflictType).stream()
                .map(this::getReferrerDisplayName)
                .collect(Collectors.joining(REFERRER_NAME_SEPARATOR));

        return StringUtils.isEmpty(result) ? "" : result + REFERRER_NAME_LIST_END;
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
