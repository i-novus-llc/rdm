package ru.inovus.ms.rdm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ru.inovus.ms.rdm.enumeration.ConflictType;
import ru.inovus.ms.rdm.model.RefBook;
import ru.inovus.ms.rdm.model.RefBookVersion;
import ru.inovus.ms.rdm.model.UiRefBookPublish;
import ru.inovus.ms.rdm.service.api.ConflictService;
import ru.inovus.ms.rdm.service.api.RefBookService;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;
import static ru.inovus.ms.rdm.util.StringUtils.addDoubleQuotes;

@Controller
public class RefBookPublishController {

    private static final String PASSPORT_ATTRIBUTE_NAME = "name";
    private static final String REFERRER_NAME_SEPARATOR = ", ";

    private RefBookService refBookService;
    private ConflictService conflictService;

    @Autowired
    public RefBookPublishController(RefBookService refBookService, ConflictService conflictService) {
        this.refBookService = refBookService;
        this.conflictService = conflictService;
    }

    UiRefBookPublish getByVersionId(Integer versionId) {

        RefBook refBook = refBookService.getByVersionId(versionId);

        UiRefBookPublish uiRefBookPublish = new UiRefBookPublish(refBook);

        Map<String, String> conflictReferrerNames =
                Stream.of(ConflictType.values())
                        .collect(toMap(ConflictType::name,
                                conflictType -> getCheckConflictReferrerNames(versionId, conflictType)
                                )
                        );

        uiRefBookPublish.setConflictReferrerNames(conflictReferrerNames);

        return uiRefBookPublish;
    }

    /**
     * Получение названий справочников, имеющих конфликтные ссылки на версию.
     *
     * @param versionId    идентификатор версии справочника
     * @param conflictType тип конфликта
     * @return Названия справочников (через запятую)
     */
    private String getCheckConflictReferrerNames(Integer versionId, ConflictType conflictType) {
        return conflictService.getCheckConflictReferrers(versionId, conflictType)
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
