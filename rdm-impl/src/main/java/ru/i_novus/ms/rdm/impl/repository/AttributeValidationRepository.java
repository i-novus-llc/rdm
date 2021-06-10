package ru.i_novus.ms.rdm.impl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.i_novus.ms.rdm.api.model.validation.AttributeValidationType;
import ru.i_novus.ms.rdm.impl.entity.AttributeValidationEntity;

import java.util.List;

public interface AttributeValidationRepository extends
        JpaRepository<AttributeValidationEntity, Integer> {

    List<AttributeValidationEntity> findAllByVersionId(Integer versionId);

    List<AttributeValidationEntity> findAllByVersionIdAndAttribute(Integer versionId, String attribute);

    void deleteByVersionId(Integer versionId);

    void deleteByVersionIdAndAttribute(Integer versionId, String attribute);

    void deleteByVersionIdAndAttributeAndType(Integer versionId, String attribute, AttributeValidationType type);
}