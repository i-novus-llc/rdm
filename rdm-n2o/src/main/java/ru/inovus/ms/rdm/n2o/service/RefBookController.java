package ru.inovus.ms.rdm.n2o.service;

import net.n2oapp.platform.i18n.Messages;
import net.n2oapp.platform.jaxrs.RestPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import ru.inovus.ms.rdm.api.service.RefBookService;
import ru.inovus.ms.rdm.n2o.criteria.RefBookStatusCriteria;
import ru.inovus.ms.rdm.n2o.model.RefBookStatus;
import ru.inovus.ms.rdm.n2o.model.UiRefBookStatus;
import ru.inovus.ms.rdm.api.model.refbook.RefBook;
import ru.inovus.ms.rdm.api.model.refbook.RefBookCriteria;
import ru.inovus.ms.rdm.api.util.RdmPermission;

import java.util.ArrayList;
import java.util.List;

@Controller
public class RefBookController {

    private static final String REF_BOOK_STATUS_ARCHIVED = "refbook.status.archived";
    private static final String REF_BOOK_STATUS_HAS_DRAFT = "refbook.status.has_draft";
    private static final String REF_BOOK_STATUS_PUBLISHED = "refbook.status.published";

    private Messages messages;

    private RefBookService refBookService;

    private RdmPermission rdmPermission;

    @Autowired
    public RefBookController(Messages messages,
                             RefBookService refBookService,
                             RdmPermission rdmPermission) {
        this.messages = messages;
        this.refBookService = refBookService;

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
            return null;

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
        return (refBooks != null && !CollectionUtils.isEmpty(refBooks.getContent())) ? refBooks.getContent().get(0) : null;
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

    @SuppressWarnings("unused") // used in: refBookStatusList.query.xml
    public Page<UiRefBookStatus> getStatusList(RefBookStatusCriteria criteria) {

        List<UiRefBookStatus> list = new ArrayList<>();
        list.add(getRefBookStatus(RefBookStatus.PUBLISHED, REF_BOOK_STATUS_PUBLISHED));

        if (!criteria.getExcludeDraft())
            list.add(getRefBookStatus(RefBookStatus.HAS_DRAFT, REF_BOOK_STATUS_HAS_DRAFT));

        if (!criteria.getNonArchived())
            list.add(getRefBookStatus(RefBookStatus.ARCHIVED, REF_BOOK_STATUS_ARCHIVED));

        return new RestPage<>(list, Pageable.unpaged(), list.size());
    }

    private UiRefBookStatus getRefBookStatus(RefBookStatus status, String code) {
        return new UiRefBookStatus(status, messages.getMessage(code));
    }
}
