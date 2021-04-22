package ru.i_novus.ms.rdm.api.validation;

import ru.i_novus.ms.rdm.api.model.Structure;

import java.util.List;

public interface VersionValidation {

    void validateRefBook(Integer refBookId);

    void validateRefBookCode(String refBookCode);

    void validateRefBookExists(Integer refBookId);

    void validateRefBookCodeExists(String refBookCode);

    void validateRefBookCodeNotExists(String refBookCode);

    void validateVersionExists(Integer versionId);

    void validateDraftExists(Integer draftId);

    void validateOptLockValue(Integer draftId, Integer draftLockValue, Integer optLockValue);

    void validateDraftNotArchived(Integer draftId);

    void validateAttributeExists(Integer versionId, Structure structure, String attribute);

    void validateDraftAttributeExists(Integer draftId, Structure structure, String attribute);

    void validateStructure(Structure structure);

    void validateAttribute(Structure.Attribute attribute);

    void validateReferenceAbility(Structure.Reference reference);

    void validateDraftStructure(String refBookCode, Structure draftStructure);

    void validateReferrerStructure(Structure structure);

    boolean equalsPrimaries(List<Structure.Attribute> primaries1,
                            List<Structure.Attribute> primaries2);
}