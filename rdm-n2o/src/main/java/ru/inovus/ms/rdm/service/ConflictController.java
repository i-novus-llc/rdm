package ru.inovus.ms.rdm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ru.inovus.ms.rdm.enumeration.ConflictType;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.service.api.ConflictService;

import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class ConflictController {

    private static final String PASSPORT_ATTRIBUTE_NAME = "name";
    private static final String REFERRER_NAME_SEPARATOR = ", ";

    @Autowired
    private ConflictService conflictService;

    /**
     * Получение названий справочников, имеющих конфликтные ссылки на версию.
     *
     * @param versionId    идентификатор версии справочника
     * @param conflictType тип конфликта
     * @return Названия справочников (через запятую)
     */
    public String getConflictReferrerNames(Integer versionId, ConflictType conflictType) {
        return conflictService.getConflictReferrers(versionId, conflictType)
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
                ? passport.get(PASSPORT_ATTRIBUTE_NAME)
                : version.getCode();
    }
}
