package ru.i_novus.ms.rdm.rest.client.impl;

import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.model.ExportFile;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.model.draft.CreateDraftRequest;
import ru.i_novus.ms.rdm.api.model.draft.Draft;
import ru.i_novus.ms.rdm.api.model.refdata.*;
import ru.i_novus.ms.rdm.api.model.validation.AttributeValidation;
import ru.i_novus.ms.rdm.api.model.validation.AttributeValidationRequest;
import ru.i_novus.ms.rdm.api.model.validation.AttributeValidationType;
import ru.i_novus.ms.rdm.api.model.version.CreateAttributeRequest;
import ru.i_novus.ms.rdm.api.model.version.DeleteAttributeRequest;
import ru.i_novus.ms.rdm.api.model.version.UpdateAttributeRequest;
import ru.i_novus.ms.rdm.api.rest.DraftRestService;
import ru.i_novus.ms.rdm.rest.client.feign.DraftRestServiceFeignClient;

import java.util.List;

public class DraftRestServiceRestClient implements DraftRestService {

    private final DraftRestServiceFeignClient client;

    public DraftRestServiceRestClient(DraftRestServiceFeignClient client) {
        this.client = client;
    }

    @Override
    public Draft create(CreateDraftRequest request) {
        return client.create(request);
    }

    @Override
    public Draft createFromVersion(Integer versionId) {
        return client.createFromVersion(versionId);
    }

    @Override
    public Draft create(Integer refBookId, FileModel fileModel) {
        return client.create(refBookId, fileModel);
    }

    @Override
    public void updateData(Integer draftId, UpdateDataRequest request) {
        client.updateData(draftId, request);
    }

    @Override
    public void deleteData(Integer draftId, DeleteDataRequest request) {
        client.deleteData(draftId, request);
    }

    @Override
    public void deleteAllData(Integer draftId, DeleteAllDataRequest request) {
        client.deleteAllData(draftId, request);
    }

    @Override
    public void updateFromFile(Integer draftId, UpdateFromFileRequest request) {
        client.updateFromFile(draftId, request);
    }

    @Override
    public Page<RefBookRowValue> search(Integer draftId, SearchDataCriteria criteria) {
        return client.search(draftId, criteria);
    }

    @Override
    public Boolean hasData(Integer draftId) {
        return client.hasData(draftId);
    }

    @Override
    public void remove(Integer draftId) {
        client.remove(draftId);
    }

    @Override
    public Draft getDraft(Integer draftId) {
        return client.getDraft(draftId);
    }

    @Override
    public Draft findDraft(String refBookCode) {
        return client.findDraft(refBookCode);
    }

    @Override
    public void createAttribute(Integer draftId, CreateAttributeRequest request) {
        client.createAttribute(draftId, request);
    }

    @Override
    public void updateAttribute(Integer draftId, UpdateAttributeRequest request) {
        client.updateAttribute(draftId, request);
    }

    @Override
    public void deleteAttribute(Integer draftId, DeleteAttributeRequest request) {
        client.deleteAttribute(draftId, request);
    }

    @Override
    public void addAttributeValidation(Integer draftId, String attribute, AttributeValidation attributeValidation) {
        client.addAttributeValidation(draftId, attribute, attributeValidation);
    }

    @Override
    public void deleteAttributeValidation(Integer draftId, String attribute, AttributeValidationType type) {
        client.deleteAttributeValidation(draftId, attribute, type);
    }

    @Override
    public List<AttributeValidation> getAttributeValidations(Integer draftId, String attribute) {
        return client.getAttributeValidations(draftId, attribute);
    }

    @Override
    public void updateAttributeValidations(Integer draftId, AttributeValidationRequest request) {
        client.updateAttributeValidations(draftId, request);
    }

    @Override
    public ExportFile getDraftFile(Integer draftId, FileType fileType) {
        return client.getDraftFile(draftId, fileType);
    }
}
