package ru.inovus.ms.rdm.service;

import net.n2oapp.framework.access.simple.PermissionApi;
import net.n2oapp.framework.api.user.StaticUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import ru.inovus.ms.rdm.model.refbook.RefBook;
import ru.inovus.ms.rdm.model.refbook.RefBookCriteria;
import ru.inovus.ms.rdm.service.api.RefBookService;
import ru.inovus.ms.rdm.util.RdmPermission;

import static org.springframework.util.CollectionUtils.isEmpty;

@Controller
public class RefBookController {

    private RefBookService refBookService;

    private RdmPermission rdmPermission;

    @Autowired
    public RefBookController(RefBookService refBookService, PermissionApi permissionApi) {
        this.refBookService = refBookService;

        this.rdmPermission = new RdmPermission(StaticUserContext.getUserContext(), permissionApi);
    }

    /**
     * Поиск последней версии справочника для открытия на просмотр/редактирование.
     */
    @SuppressWarnings("unused")
    public RefBook searchLastVersion(RefBookCriteria criteria) {

        Page<RefBook> refBooks = refBookService.searchVersions(permitCriteria(criteria));
        return (refBooks != null && !isEmpty(refBooks.getContent())) ? refBooks.getContent().get(0) : null;
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
}
