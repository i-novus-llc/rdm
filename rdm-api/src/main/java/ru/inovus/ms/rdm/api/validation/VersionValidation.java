package ru.inovus.ms.rdm.api.validation;

import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.version.RefBookVersion;

public interface VersionValidation {

    void validateRefBook(Integer refBookId);

    void validateVersion(Integer versionId);

    void validateDraft(Integer draftId);

    void validateRefBookExists(Integer refBookId);

    void validateRefBookCodeExists(String refBookCode);

    void validateVersionExists(Integer versionId);

    void validateDraftExists(Integer draftId);

    void validateDraftNotArchived(Integer draftId);

    void validateAttributeExists(Integer versionId, Structure structure, String attribute);

    void validateDraftAttributeExists(Integer versionId, String attribute);

    void validateStructure(Structure structure);

    void validateReferenceDisplayExpression(String displayExpression,
                                            RefBookVersion referredVersion);
}