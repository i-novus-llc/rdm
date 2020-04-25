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
        throttle.throttleAndUpdateRequestTime();
        return refBookService.search(criteria);
    }

    public Page<RefBook> searchVersions(RefBookCriteria criteria) {
        throttle.throttleAndUpdateRequestTime();
        return refBookService.searchVersions(criteria);
    }

    public RefBook getByVersionId(Integer versionId) {
        throttle.throttleAndUpdateRequestTime();
        return refBookService.getByVersionId(versionId);
    }

    public String getCode(Integer refBookId) {
        throttle.throttleAndUpdateRequestTime();
        return refBookService.getCode(refBookId);
    }

    public Integer getId(String refBookCode) {
        throttle.throttleAndUpdateRequestTime();
        return refBookService.getId(refBookCode);
    }

    public RefBook create(RefBookCreateRequest refBookCreateRequest) {
        throttle.throttleAndUpdateRequestTime();
        return refBookService.create(refBookCreateRequest);
    }

    public Draft create(FileModel fileModel) {
        throttle.throttleAndUpdateRequestTime();
        return refBookService.create(fileModel);
    }

    public RefBook update(RefBookUpdateRequest refBookUpdateRequest) {
        throttle.throttleAndUpdateRequestTime();
        return refBookService.update(refBookUpdateRequest);
    }

    public void delete(int refBookId) {
        throttle.throttleAndUpdateRequestTime();
        refBookService.delete(refBookId);
    }

    public void toArchive(int refBookId) {
        throttle.throttleAndUpdateRequestTime();
        refBookService.toArchive(refBookId);
    }

    public void fromArchive(int refBookId) {
        throttle.throttleAndUpdateRequestTime();
        refBookService.fromArchive(refBookId);
    }

    public void changeData(RdmChangeDataRequest request) {
        throttle.throttleAndUpdateRequestTime();
        refBookService.changeData(request);
    }

}
