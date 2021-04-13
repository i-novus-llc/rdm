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
import ru.i_novus.ms.rdm.api.model.refbook.RefBookType;
import ru.i_novus.ms.rdm.api.service.RefBookService;
import ru.i_novus.ms.rdm.api.util.RdmPermission;
import ru.i_novus.ms.rdm.n2o.criteria.RefBookStatusCriteria;
import ru.i_novus.ms.rdm.n2o.model.RefBookStatus;
import ru.i_novus.ms.rdm.n2o.model.UiRefBookStatus;
import ru.i_novus.ms.rdm.n2o.model.UiRefBookType;

import java.util.ArrayList;
import java.util.List;

@Controller
public class RefBookController {

    private static final String REFBOOK_NOT_FOUND_EXCEPTION_CODE = "refbook.not.found";

    private static final String REFBOOK_TYPE_UNVERSIONED = "refbook.type.unversioned";

    private static final String REFBOOK_STATUS_ARCHIVED = "refbook.status.archived";
    private static final String REFBOOK_STATUS_HAS_DRAFT = "refbook.status.has_draft";
    private static final String REFBOOK_STATUS_PUBLISHED = "refbook.status.published";

    private RefBookService refBookService;

    private Messages messages;

    private RdmPermission rdmPermission;

    @Autowired
    public RefBookController(RefBookService refBookService,
                             Messages messages,
                             RdmPermission rdmPermission) {
        this.refBookService = refBookService;

        this.messages = messages;
        this.rdmPermission = rdmPermission;
    }

    /**
     * Поиск справочника по версии.
     * Обёртка над сервисным методом для учёта прав доступа.
     *
     * @param criteria критерий поиска справочника
     * @return Справочник
     */
    @SuppressWarnings("unused") // used in: refBook.query.xml
    public RefBook searchRefBook(RefBookCriteria criteria) {

        RefBook refBook = refBookService.getByVersionId(permitCriteria(criteria).getVersionId());
        if (refBook == null)
            throw new UserException(REFBOOK_NOT_FOUND_EXCEPTION_CODE);

        if (criteria.getExcludeDraft())
            refBook.setDraftVersionId(null);

        return refBook;
    }

    /**
     * Поиск последней версии справочника для открытия на просмотр/редактирование.
     *
     * @param criteria критерий поиска версии справочника
     * @return Справочник по последней версии
     */
    @SuppressWarnings("unused") // used in: refBookVersion.query.xml
    public RefBook searchLastVersion(RefBookCriteria criteria) {

        Page<RefBook> refBooks = refBookService.searchVersions(permitCriteria(criteria));

        if (refBooks == null || CollectionUtils.isEmpty(refBooks.getContent()))
            throw new UserException(REFBOOK_NOT_FOUND_EXCEPTION_CODE);

        return refBooks.getContent().get(0);
    }

    /**
     * Поиск справочников, на которые можно ссылаться.
     * Обёртка над сервисным методом для учёта ограничений.
     *
     * @param criteria критерий поиска
     * @return Список справочников
     */
    @SuppressWarnings("unused") // used in: referenceRefBook.query.xml
    public Page<RefBook> searchReferenceRefBooks(RefBookCriteria criteria) {

        criteria.setHasPublished(true);
        criteria.setExcludeDraft(true);
        criteria.setSourceType(RefBookSourceType.LAST_PUBLISHED);

        return refBookService.search(permitCriteria(criteria));
    }

    /**
     * Корректировка критерия с учётом разрешений пользователя.
     *
     * @param criteria критерий поиска
     * @return Скорректированный критерий поиска
     */
    private RefBookCriteria permitCriteria(RefBookCriteria criteria) {

        criteria.setExcludeDraft(rdmPermission.excludeDraft());

        return criteria;
    }

    @SuppressWarnings("unused") // used in: refBookTypeList.query.xml
    public Page<UiRefBookType> getTypeList() {

        List<UiRefBookType> list = new ArrayList<>();
        list.add(getRefBookType(RefBookType.UNVERSIONED, REFBOOK_TYPE_UNVERSIONED));

        return new RestPage<>(list, Pageable.unpaged(), list.size());
    }

    private UiRefBookType getRefBookType(RefBookType type, String code) {
        return new UiRefBookType(type, messages.getMessage(code));
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
