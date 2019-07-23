package ru.inovus.ms.rdm.file.process;

import ru.inovus.ms.rdm.model.refbook.RefBookUpdateRequest;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.util.Map;

public class PassportPersister implements PassportProcessor {

    private VersionService versionService;
    private Integer versionId;

    public PassportPersister(VersionService versionService, Integer versionId) {
        this.versionService = versionService;
        this.versionId = versionId;
    }

    @Override
    public void process(Map<String, String> passport) {
        RefBookUpdateRequest request = new RefBookUpdateRequest();
        request.setVersionId(versionId);
        request.setPassport(passport);
        versionService.updatePassport(request);
    }

}
