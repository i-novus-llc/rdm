package ru.inovus.ms.rdm.validation;

public interface VersionValidation {

    void validateRefBook(Integer refBookId);

    void validateVersion(Integer versionId);

    void validateDraft(Integer draftId);

    void validateRefBookExists(Integer refBookId);

    void validateVersionExists(Integer versionId);

    void validateDraftExists(Integer draftId);

    void validateAttributeExists(Integer versionId, String attribute);

}