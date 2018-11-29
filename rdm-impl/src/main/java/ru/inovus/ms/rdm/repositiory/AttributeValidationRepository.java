package ru.inovus.ms.rdm.repositiory;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.inovus.ms.rdm.entity.AttributeValidationEntity;
import ru.inovus.ms.rdm.model.validation.AttributeValidationType;

import java.util.List;

public interface AttributeValidationRepository extends
        JpaRepository<AttributeValidationEntity, Integer> {
    List<AttributeValidationEntity> findAllByVersionIdAndAttributeAndType(Integer versionId, String attribute, AttributeValidationType type);

    List<AttributeValidationEntity> findAllByVersionIdAndAttribute(Integer versionId, String attribute);

    List<AttributeValidationEntity> findAllByVersionId(Integer versionId);


}