package ru.i_novus.ms.rdm.impl.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
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
import ru.i_novus.ms.rdm.api.service.DraftService;

import java.util.List;

@Primary
@Service
public class DraftRestServiceImpl implements DraftRestService {

    private DraftService draftService;

    @Autowired
    public DraftRestServiceImpl(DraftService draftService) {
        this.draftService = draftService;
    }

    @Override
    public Draft create(Integer refBookId, FileModel fileModel) {
        return draftService.create(refBookId, fileModel);
    }

    @Override
    public Draft create(CreateDraftRequest request) {
        return draftService.create(request);
    }

    @Override
    public Draft createFromVersion(Integer versionId) {
        return draftService.createFromVersion(versionId);
    }

    @Override
    public void updateData(Integer draftId, UpdateDataRequest request) {
        draftService.updateData(draftId, request);
    }

    @Override
    public void deleteData(Integer draftId, DeleteDataRequest request) {
        draftService.deleteData(draftId, request);
    }

    @Override
    public void deleteAllData(Integer draftId, DeleteAllDataRequest request) {
        draftService.deleteAllData(draftId, request);
    }

    @Override
    public void updateFromFile(Integer draftId, UpdateFromFileRequest request) {
        draftService.updateFromFile(draftId, request);
    }

    @Override
    public Page<RefBookRowValue> search(Integer draftId, SearchDataCriteria criteria) {
        return draftService.search(draftId, criteria);
    }

    @Override
    public Boolean hasData(Integer draftId) {
        return draftService.hasData(draftId);
    }

    @Override
    public void remove(Integer draftId) {
        draftService.remove(draftId);
    }

    @Override
    public Draft getDraft(Integer draftId) {
        return draftService.getDraft(draftId);
    }

    @Override
    public Draft findDraft(String refBookCode) {
        return draftService.findDraft(refBookCode);
    }

    @Override
    public void createAttribute(Integer draftId, CreateAttributeRequest request) {
        draftService.createAttribute(draftId, request);
    }

    @Override
    public void updateAttribute(Integer draftId, UpdateAttributeRequest request) {
        draftService.updateAttribute(draftId, request);
    }

    @Override
    public void deleteAttribute(Integer draftId, DeleteAttributeRequest request) {
        draftService.deleteAttribute(draftId, request);
    }

    @Override
    public void addAttributeValidation(Integer draftId, String attribute, AttributeValidation attributeValidation) {
        draftService.addAttributeValidation(draftId, attribute, attributeValidation);
    }

    @Override
    public void deleteAttributeValidation(Integer draftId, String attribute, AttributeValidationType type) {
        draftService.deleteAttributeValidation(draftId, attribute, type);
    }

    @Override
    public List<AttributeValidation> getAttributeValidations(Integer draftId, String attribute) {
        return draftService.getAttributeValidations(draftId, attribute);
    }

    @Override
    public void updateAttributeValidations(Integer draftId, AttributeValidationRequest request) {
        draftService.updateAttributeValidations(draftId, request);
    }

    @Override
    public ExportFile getDraftFile(Integer draftId, FileType fileType) {
        return draftService.getDraftFile(draftId, fileType);
    }
}