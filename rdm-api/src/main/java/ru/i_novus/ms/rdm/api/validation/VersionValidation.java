package ru.i_novus.ms.rdm.api.validation;

import ru.i_novus.ms.rdm.api.model.Structure;

import java.util.List;

public interface VersionValidation {

    void validateRefBook(Integer refBookId);

    void validateRefBookCode(String refBookCode);

    void validateRefBookExists(Integer refBookId);

    void validateRefBookCodeExists(String refBookCode);

    @SuppressWarnings("I-novus:MethodNameWordCountRule")
    void validateRefBookCodeNotExists(String refBookCode);

    boolean hasReferrerVersions(String refBookCode);

    void validateVersionExists(Integer versionId);

    void validateOptLockValue(Integer draftId, Integer draftLockValue, Integer optLockValue);

    void validateDraftNotArchived(Integer draftId);

    void validateAttributeExists(Integer versionId, Structure structure, String attribute);

    void validateDraftAttributeExists(Integer draftId, Structure structure, String attribute);

    void validateStructure(Structure structure);

    void validateAttribute(Structure.Attribute attribute);

    void validateReferenceAbility(Structure.Reference reference);

    void validateDraftStructure(String refBookCode, Structure draftStructure);

    void validateReferrerStructure(Structure structure);

    void validateNewAttribute(Structure.Attribute newAttribute,
                              Structure oldStructure, String refBookCode);

    void validateNewReference(Structure.Attribute newAttribute,
                              Structure.Reference newReference,
                              Structure oldStructure, String refBookCode);

    void validateOldAttribute(Structure.Attribute oldAttribute,
                              Structure oldStructure, String refBookCode);

    boolean equalsPrimaries(List<Structure.Attribute> primaries1,
                            List<Structure.Attribute> primaries2);
}