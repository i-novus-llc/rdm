package ru.i_novus.ms.rdm.n2o.service;

import net.n2oapp.platform.jaxrs.RestPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.model.version.VersionCriteria;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.n2o.model.UiRefBook;
import ru.i_novus.ms.rdm.n2o.util.RefBookAdapter;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Controller
public class VersionController {

    private final VersionRestService versionService;

    private final RefBookAdapter refBookAdapter;

    @Autowired
    public VersionController(VersionRestService versionService,
                             RefBookAdapter refBookAdapter) {

        this.versionService = versionService;
        this.refBookAdapter = refBookAdapter;
    }

    /**
     * Поиск списка версий справочника.
     */
    @SuppressWarnings("unused") // used in: versionList.query.xml
    public Page<UiRefBook> getList(VersionCriteria criteria) {

        // NB: criteria.getExcludeDraft() ignored now.
        Page<RefBookVersion> versions = versionService.getVersions(criteria);
        List<UiRefBook> list = versions.getContent().stream().map(this::toUiRefBook).collect(toList());
        return new RestPage<>(list, criteria, versions.getTotalElements());
    }

    /**
     * Поиск версии справочника для открытия на просмотр/редактирование.
     */
    @SuppressWarnings("unused") // used in: versionList.query.xml
    public UiRefBook getVersion(VersionCriteria criteria) {

        // NB: criteria.getExcludeDraft() ignored now.
        return toUiRefBook(versionService.getById(criteria.getId()));
    }

    private UiRefBook toUiRefBook(RefBookVersion version) {

        return version != null ? refBookAdapter.toUiRefBook(new RefBook(version)) : null;
    }
}
