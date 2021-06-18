package ru.i_novus.ms.rdm.n2o.service;

import net.n2oapp.platform.i18n.Messages;
import net.n2oapp.platform.i18n.UserException;
import net.n2oapp.platform.jaxrs.RestPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import ru.i_novus.ms.rdm.api.enumeration.RefBookSourceType;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCriteria;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;
import ru.i_novus.ms.rdm.api.service.RefBookService;
import ru.i_novus.ms.rdm.api.util.RdmPermission;
import ru.i_novus.ms.rdm.n2o.criteria.RefBookStatusCriteria;
import ru.i_novus.ms.rdm.n2o.criteria.RefBookTypeCriteria;
import ru.i_novus.ms.rdm.n2o.model.*;
import ru.i_novus.ms.rdm.n2o.util.RefBookAdapter;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Controller
public class RefBookController {

    private static final String REFBOOK_NOT_FOUND_EXCEPTION_CODE = "refbook.not.found";

    private static final String REFBOOK_TYPE_DEFAULT = "refbook.type.default";
    private static final String REFBOOK_TYPE_UNVERSIONED = "refbook.type.unversioned";

    private static final String REFBOOK_STATUS_ARCHIVED = "refbook.status.archived";
    private static final String REFBOOK_STATUS_HAS_DRAFT = "refbook.status.has_draft";
    private static final String REFBOOK_STATUS_PUBLISHED = "refbook.status.published";

    private final RefBookService refBookService;

    private final RefBookAdapter refBookAdapter;

    private final Messages messages;

    private final RdmPermission rdmPermission;

    @Autowired
    public RefBookController(RefBookService refBookService,
                             RefBookAdapter refBookAdapter,
                             Messages messages,
                             RdmPermission rdmPermission) {

        this.refBookService = refBookService;
        this.refBookAdapter = refBookAdapter;

        this.messages = messages;
        this.rdmPermission = rdmPermission;
    }

    /**
     * Поиск справочников.
     * Обёртка над сервисным методом для учёта прав доступа.
     *
     * @param criteria критерий поиска
     * @return Список справочников
     */
    @SuppressWarnings("unused") // used in: refBook.query.xml
    public Page<UiRefBook> getList(RefBookCriteria criteria) {

        return search(permitCriteria(criteria));
    }

    /**
     * Поиск справочника по версии.
     * Обёртка над сервисным методом для учёта прав доступа.
     *
     * @param criteria критерий поиска
     * @return Справочник
     */
    @SuppressWarnings("unused") // used in: refBook.query.xml
    public UiRefBook getVersionRefBook(RefBookCriteria criteria) {

        RefBook refBook = refBookService.getByVersionId(permitCriteria(criteria).getVersionId());
        if (refBook == null)
            throw new UserException(REFBOOK_NOT_FOUND_EXCEPTION_CODE);

        if (criteria.getExcludeDraft())
            refBook.setDraftVersionId(null);

        return refBookAdapter.toUiRefBook(refBook);
    }

    /**
     * Поиск последней версии справочника для открытия на просмотр/редактирование.
     *
     * @param criteria критерий поиска версии справочника
     * @return Справочник по последней версии
     */
    @SuppressWarnings("unused") // used in: refBookVersion.query.xml
    public UiRefBook getLastVersion(RefBookCriteria criteria) {

        Page<RefBook> refBooks = refBookService.searchVersions(permitCriteria(criteria));
        if (CollectionUtils.isEmpty(refBooks.getContent()))
            throw new UserException(REFBOOK_NOT_FOUND_EXCEPTION_CODE);

        return refBookAdapter.toUiRefBook(refBooks.getContent().get(0));
    }

    /**
     * Поиск справочников, на которые можно ссылаться.
     * Обёртка над сервисным методом для учёта ограничений.
     *
     * @param criteria критерий поиска
     * @return Список справочников
     */
    @SuppressWarnings("unused") // used in: referenceRefBook.query.xml
    public Page<UiRefBook> searchReferenceRefBooks(RefBookCriteria criteria) {

        criteria.setHasPublished(true);
        criteria.setExcludeDraft(true);
        criteria.setSourceType(RefBookSourceType.LAST_PUBLISHED);

        return search(criteria);
    }

    /** Поиск справочников без учёта permission. */
    private Page<UiRefBook> search(RefBookCriteria criteria) {

        Page<RefBook> refBooks = refBookService.search(criteria);
        List<UiRefBook> list = refBooks.getContent().stream().map(refBookAdapter::toUiRefBook).collect(toList());
        return new RestPage<>(list, criteria, refBooks.getTotalElements());
    }

    /**
     * Корректировка критерия с учётом разрешений пользователя.
     *
     * @param criteria критерий поиска
     * @return Скорректированный критерий поиска
     */
    private RefBookCriteria permitCriteria(RefBookCriteria criteria) {

        if (!criteria.getExcludeDraft()) {
            boolean excludeDraft = rdmPermission.excludeDraft();
            if (excludeDraft) criteria.setExcludeDraft(excludeDraft);
        }

        return criteria;
    }

    @SuppressWarnings("unused") // used in: refBookTypeList.query.xml
    public Page<UiRefBookType> getTypeList() {

        List<UiRefBookType> list = new ArrayList<>();
        list.add(getRefBookType(RefBookTypeEnum.DEFAULT, REFBOOK_TYPE_DEFAULT));
        list.add(getRefBookType(RefBookTypeEnum.UNVERSIONED, REFBOOK_TYPE_UNVERSIONED));

        return new RestPage<>(list, Pageable.unpaged(), list.size());
    }

    private UiRefBookType getRefBookType(RefBookTypeEnum type, String code) {
        return new UiRefBookType(type, messages.getMessage(code));
    }

    public UiRefBookType getTypeItem(RefBookTypeCriteria criteria) {

        Page<UiRefBookType> types = getTypeList();

        return types.getContent().stream()
                .filter(type -> filterType(criteria, type))
                .findFirst().orElse(null);
    }

    private boolean filterType(RefBookTypeCriteria criteria, UiRefBookType type) {

        return (criteria.getId() == null || criteria.getId().equals(type.getId())) &&
                (criteria.getName() == null || criteria.getName().equals(type.getName()));
    }

    @SuppressWarnings("unused") // used in: refBookStatusList.query.xml
    public Page<UiRefBookStatus> getStatusList(RefBookStatusCriteria criteria) {

        List<UiRefBookStatus> list = new ArrayList<>();
        list.add(getRefBookStatus(RefBookStatus.PUBLISHED, REFBOOK_STATUS_PUBLISHED));

        if (!criteria.getExcludeDraft())
            list.add(getRefBookStatus(RefBookStatus.HAS_DRAFT, REFBOOK_STATUS_HAS_DRAFT));

        if (!criteria.getNonArchived())
            list.add(getRefBookStatus(RefBookStatus.ARCHIVED, REFBOOK_STATUS_ARCHIVED));

        return new RestPage<>(list, Pageable.unpaged(), list.size());
    }

    private UiRefBookStatus getRefBookStatus(RefBookStatus status, String code) {
        return new UiRefBookStatus(status, messages.getMessage(code));
    }
}
