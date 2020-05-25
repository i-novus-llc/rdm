package ru.inovus.ms.rdm.n2o.service;

import net.n2oapp.platform.i18n.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ru.inovus.ms.rdm.api.enumeration.ConflictType;
import ru.inovus.ms.rdm.api.model.draft.PublishRequest;
import ru.inovus.ms.rdm.api.model.refbook.RefBook;
import ru.inovus.ms.rdm.api.model.version.RefBookVersion;
import ru.inovus.ms.rdm.api.service.ConflictService;
import ru.inovus.ms.rdm.api.service.DraftService;
import ru.inovus.ms.rdm.api.service.PublishService;
import ru.inovus.ms.rdm.api.service.RefBookService;
import ru.inovus.ms.rdm.n2o.model.UiRefBookPublish;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;
import static ru.inovus.ms.rdm.api.util.StringUtils.addDoubleQuotes;

@Controller
@SuppressWarnings("unused") // used in: publish.*.xml
public class RefBookPublishController {

    private static final Logger logger = LoggerFactory.getLogger(RefBookPublishController.class);

    private static final String PUBLISHING_DRAFT_STRUCTURE_NOT_FOUND_EXCEPTION_CODE = "publishing.draft.structure.not.found";
    private static final String PUBLISHING_DRAFT_DATA_NOT_FOUND_EXCEPTION_CODE = "publishing.draft.data.not.found";

    private static final ConflictType[] CONFLICT_TYPE_VALUES = ConflictType.values();

    private static final String PASSPORT_ATTRIBUTE_NAME = "name";
    private static final String REFERRER_NAME_SEPARATOR = ", ";

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

    public UiRefBookPublish getByVersionId(Integer versionId) {

        RefBook refBook = refBookService.getByVersionId(versionId);

        UiRefBookPublish uiRefBookPublish = new UiRefBookPublish(refBook);

        if (refBook.getStructure() == null || refBook.getStructure().isEmpty()) {
            String message = messages.getMessage(PUBLISHING_DRAFT_STRUCTURE_NOT_FOUND_EXCEPTION_CODE);
            uiRefBookPublish.setErrorMessage(message);
            return uiRefBookPublish;
        }

        if (!Boolean.TRUE.equals(draftService.hasData(versionId))) {
            String message = messages.getMessage(PUBLISHING_DRAFT_DATA_NOT_FOUND_EXCEPTION_CODE);
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

    /**
     * Публикация черновика справочника.
     *
     * @param draftId идентификатор черновика
     */
    public UUID publishDraft(Integer draftId) {

        PublishRequest request = new PublishRequest(draftId);
        return publishService.publishAsync(request);
    }

    /**
     * Публикация черновика справочника с обновлением ссылок.
     *
     * @param draftId идентификатор черновика
     */
    public UUID publishAndRefresh(Integer draftId) {

        PublishRequest request = new PublishRequest(draftId);
        request.setResolveConflicts(true);
        return publishService.publishAsync(request);
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
     * @return Названия справочников (через запятую)
     */
    private String getConflictTypeReferrerNames(Integer versionId, ConflictType conflictType) {

        return conflictService.getConflictingReferrers(versionId, conflictType).stream()
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
