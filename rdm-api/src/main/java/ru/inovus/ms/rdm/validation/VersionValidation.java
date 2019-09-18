package ru.inovus.ms.rdm.validation;

import ru.inovus.ms.rdm.n2o.model.Structure;

public interface VersionValidation {

    void validateRefBook(Integer refBookId);

    void validateVersion(Integer versionId);

    void validateDraft(Integer draftId);

    void validateRefBookExists(Integer refBookId);

    void validateVersionExists(Integer versionId);

    void validateDraftExists(Integer draftId);

    void validateAttributeExists(Integer versionId, Structure structure, String attribute);

    void validateDraftAttributeExists(Integer versionId, String attribute);

}