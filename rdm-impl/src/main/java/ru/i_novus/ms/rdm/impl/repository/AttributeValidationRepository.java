package ru.i_novus.ms.rdm.impl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.i_novus.ms.rdm.impl.entity.AttributeValidationEntity;
import ru.i_novus.ms.rdm.api.model.validation.AttributeValidationType;

import java.util.List;

public interface AttributeValidationRepository extends
        JpaRepository<AttributeValidationEntity, Integer> {

    List<AttributeValidationEntity> findAllByVersionIdAndAttributeAndType(Integer versionId, String attribute, AttributeValidationType type);

    List<AttributeValidationEntity> findAllByVersionIdAndAttribute(Integer versionId, String attribute);

    List<AttributeValidationEntity> findAllByVersionId(Integer versionId);

    void deleteByVersionIdAndAttribute(Integer versionId, String attribute);
}