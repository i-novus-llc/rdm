package ru.i_novus.ms.rdm.rest.client.impl;

import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.model.draft.Draft;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCreateRequest;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCriteria;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookUpdateRequest;
import ru.i_novus.ms.rdm.api.model.refdata.RdmChangeDataRequest;
import ru.i_novus.ms.rdm.api.service.RefBookService;
import ru.i_novus.ms.rdm.rest.client.feign.RefBookServiceFeignClient;

public class RefBookServiceRestClient implements RefBookService {

    private final RefBookServiceFeignClient client;

    public RefBookServiceRestClient(RefBookServiceFeignClient client) {
        this.client = client;
    }

    @Override
    public Page<RefBook> search(RefBookCriteria criteria) {
        return client.search(criteria);
    }

    @Override
    public Page<RefBook> searchVersions(RefBookCriteria criteria) {
        return client.searchVersions(criteria);
    }

    @Override
    public RefBook getByVersionId(Integer versionId) {
        return client.getByVersionId(versionId);
    }

    @Override
    public String getCode(Integer refBookId) {
        return client.getCode(refBookId);
    }

    @Override
    public Integer getId(String refBookCode) {
        return client.getId(refBookCode);
    }

    @Override
    public RefBook create(RefBookCreateRequest refBookCreateRequest) {
        return client.create(refBookCreateRequest);
    }

    @Override
    public Draft create(FileModel fileModel) {
        return client.create(fileModel);
    }

    @Override
    public RefBook update(RefBookUpdateRequest refBookUpdateRequest) {
        return client.update(refBookUpdateRequest);
    }

    @Override
    public void delete(int refBookId) {
        client.delete(refBookId);
    }

    @Override
    public void toArchive(int refBookId) {
        client.toArchive(refBookId);
    }

    @Override
    public void fromArchive(int refBookId) {
        client.fromArchive(refBookId);
    }

    @Override
    public void changeData(RdmChangeDataRequest request) {
        client.changeData(request);
    }
}
