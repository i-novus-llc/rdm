package ru.inovus.ms.rdm.sync.service.throttle;

import org.springframework.data.domain.Page;
import ru.inovus.ms.rdm.api.model.FileModel;
import ru.inovus.ms.rdm.api.model.draft.Draft;
import ru.inovus.ms.rdm.api.model.refbook.RefBook;
import ru.inovus.ms.rdm.api.model.refbook.RefBookCreateRequest;
import ru.inovus.ms.rdm.api.model.refbook.RefBookCriteria;
import ru.inovus.ms.rdm.api.model.refbook.RefBookUpdateRequest;
import ru.inovus.ms.rdm.api.model.refdata.RdmChangeDataRequest;
import ru.inovus.ms.rdm.api.service.RefBookService;

public class ThrottlingRefBookService implements RefBookService {

    private final Throttle throttle;
    private final RefBookService refBookService;

    public ThrottlingRefBookService(Throttle throttle, RefBookService refBookService) {
        this.refBookService = refBookService;
        this.throttle = throttle;
    }

    public Page<RefBook> search(RefBookCriteria criteria) {
        throttle.throttleAndUpdatePrevRequestTime();
        return refBookService.search(criteria);
    }

    public Page<RefBook> searchVersions(RefBookCriteria criteria) {
        throttle.throttleAndUpdatePrevRequestTime();
        return refBookService.searchVersions(criteria);
    }

    public RefBook getByVersionId(Integer versionId) {
        throttle.throttleAndUpdatePrevRequestTime();
        return refBookService.getByVersionId(versionId);
    }

    public String getCode(Integer refBookId) {
        throttle.throttleAndUpdatePrevRequestTime();
        return refBookService.getCode(refBookId);
    }

    public Integer getId(String refBookCode) {
        throttle.throttleAndUpdatePrevRequestTime();
        return refBookService.getId(refBookCode);
    }

    public RefBook create(RefBookCreateRequest refBookCreateRequest) {
        throttle.throttleAndUpdatePrevRequestTime();
        return refBookService.create(refBookCreateRequest);
    }

    public Draft create(FileModel fileModel) {
        throttle.throttleAndUpdatePrevRequestTime();
        return refBookService.create(fileModel);
    }

    public RefBook update(RefBookUpdateRequest refBookUpdateRequest) {
        throttle.throttleAndUpdatePrevRequestTime();
        return refBookService.update(refBookUpdateRequest);
    }

    public void delete(int refBookId) {
        throttle.throttleAndUpdatePrevRequestTime();
        refBookService.delete(refBookId);
    }

    public void toArchive(int refBookId) {
        throttle.throttleAndUpdatePrevRequestTime();
        refBookService.toArchive(refBookId);
    }

    public void fromArchive(int refBookId) {
        throttle.throttleAndUpdatePrevRequestTime();
        refBookService.fromArchive(refBookId);
    }

    public void changeData(RdmChangeDataRequest request) {
        throttle.throttleAndUpdatePrevRequestTime();
        refBookService.changeData(request);
    }

}
