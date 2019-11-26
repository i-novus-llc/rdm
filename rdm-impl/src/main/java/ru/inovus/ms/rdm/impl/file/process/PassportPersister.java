package ru.inovus.ms.rdm.impl.file.process;

import ru.inovus.ms.rdm.api.model.refbook.RefBookUpdateRequest;
import ru.inovus.ms.rdm.api.service.PassportService;

import java.util.Map;

public class PassportPersister implements PassportProcessor {

    private PassportService passportService;
    private Integer versionId;

    public PassportPersister(PassportService passportService, Integer versionId) {
        this.passportService = passportService;
        this.versionId = versionId;
    }

    @Override
    public void process(Map<String, String> passport) {
        RefBookUpdateRequest request = new RefBookUpdateRequest();
        request.setVersionId(versionId);
        request.setPassport(passport);
        passportService.updatePassport(request);
    }

}
