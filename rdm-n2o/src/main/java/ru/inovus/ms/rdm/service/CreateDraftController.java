package ru.inovus.ms.rdm.service;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.model.Draft;
import ru.inovus.ms.rdm.model.FileModel;
import ru.inovus.ms.rdm.model.RefBookVersion;
import ru.inovus.ms.rdm.service.api.DraftService;
import ru.inovus.ms.rdm.service.api.VersionService;

@Controller
public class CreateDraftController {

    @Autowired
    private VersionService versionService;
    @Autowired
    private DraftService draftService;

    public Draft uploadFromFile(Integer versionId, FileModel fileModel) {

        RefBookVersion version = versionService.getById(versionId);
        if (version == null)
            throw new UserException(new Message("version.not.found", versionId));

        if (RefBookVersionStatus.DRAFT.equals(version.getStatus())) {
            draftService.updateData(versionId, fileModel);
            return new Draft(versionId, version.getStorageCode());
        } else {
            draftService.create(version.getRefBookId(), version.getStructure());
            return draftService.create(version.getRefBookId(), fileModel);
        }
    }

}
