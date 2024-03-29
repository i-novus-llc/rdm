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
import ru.i_novus.ms.rdm.n2o.model.RefBookStatus;
import ru.i_novus.ms.rdm.n2o.model.UiRefBook;
import ru.i_novus.ms.rdm.n2o.model.UiRefBookStatus;
import ru.i_novus.ms.rdm.n2o.model.UiRefBookType;
import ru.i_novus.ms.rdm.n2o.util.RefBookAdapter;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

@Controller
public class RefBookController extends BaseController {

    private static final String REFBOOK_NOT_FOUND_EXCEPTION_CODE = "refbook.not.found";

    private static final String REFBOOK_TYPE_PREFIX = "refbook.type.";
    private static final String REFBOOK_STATUS_PREFIX = "refbook.status.";

    private final RefBookService refBookService;

    private final RefBookAdapter refBookAdapter;

    private final RdmPermission rdmPermission;

    @Autowired
    public RefBookController(RefBookService refBookService,
                             RefBookAdapter refBookAdapter,
                             Messages messages,
                             RdmPermission rdmPermission) {
        super(messages);

        this.refBookService = refBookService;
        this.refBookAdapter = refBookAdapter;

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

        return toUiRefBook(refBook);
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

        return toUiRefBook(refBooks.getContent().get(0));
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
        List<UiRefBook> list = refBooks.getContent().stream().map(this::toUiRefBook).collect(toList());
        return new RestPage<>(list, criteria, refBooks.getTotalElements());
    }

    /**
     * Получение модели справочника для UI по исходной модели.
     *
     * @param refBook исходная модель справочника
     * @return Модель справочника для UI
     */
    private UiRefBook toUiRefBook(RefBook refBook) {

        if (refBook == null)
            return null;

        UiRefBook result = refBookAdapter.toUiRefBook(refBook);
        result.setTypeName(toEnumLocaleName(REFBOOK_TYPE_PREFIX, refBook.getType()));

        return result;
    }

    /**
     * Корректировка критерия с учётом разрешений пользователя.
     *
     * @param criteria критерий поиска
     * @return Скорректированный критерий поиска
     */
    private RefBookCriteria permitCriteria(RefBookCriteria criteria) {

        if (!criteria.getExcludeDraft() && rdmPermission.excludeDraft()) {
            criteria.setExcludeDraft(true);
        }

        return criteria;
    }

    // NB: used as list in: refBookTypeList.query.xml
    public Page<UiRefBookType> getTypeList() {

        List<UiRefBookType> list = asList(
                toRefBookType(RefBookTypeEnum.DEFAULT),
                toRefBookType(RefBookTypeEnum.UNVERSIONED)
        );
        return new RestPage<>(list, Pageable.unpaged(), list.size());
    }

    // NB: used as unique in: refBookTypeList.query.xml
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

    /** Тип справочника с локализованным наименованием. */
    private UiRefBookType toRefBookType(RefBookTypeEnum type) {
        return new UiRefBookType(type, toEnumLocaleName(REFBOOK_TYPE_PREFIX, type));
    }

    @SuppressWarnings("unused") // used in: refBookStatusList.query.xml
    public Page<UiRefBookStatus> getStatusList(RefBookStatusCriteria criteria) {

        List<UiRefBookStatus> list = new ArrayList<>(3);
        list.add(toRefBookStatus(RefBookStatus.PUBLISHED));

        if (!criteria.getExcludeDraft())
            list.add(toRefBookStatus(RefBookStatus.HAS_DRAFT));

        if (!criteria.getNonArchived())
            list.add(toRefBookStatus(RefBookStatus.ARCHIVED));

        return new RestPage<>(list, Pageable.unpaged(), list.size());
    }

    /** Статус справочника с локализованным наименованием. */
    private UiRefBookStatus toRefBookStatus(RefBookStatus status) {
        return new UiRefBookStatus(status, toEnumLocaleName(REFBOOK_STATUS_PREFIX, status));
    }
}
