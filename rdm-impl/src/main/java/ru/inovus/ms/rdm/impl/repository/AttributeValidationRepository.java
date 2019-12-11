package ru.inovus.ms.rdm.impl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.inovus.ms.rdm.impl.entity.AttributeValidationEntity;
import ru.inovus.ms.rdm.api.model.validation.AttributeValidationType;

import java.util.List;

public interface AttributeValidationRepository extends
        JpaRepository<AttributeValidationEntity, Integer> {
    List<AttributeValidationEntity> findAllByVersionIdAndAttributeAndType(Integer versionId, String attribute, AttributeValidationType type);

    List<AttributeValidationEntity> findAllByVersionIdAndAttribute(Integer versionId, String attribute);

    List<AttributeValidationEntity> findAllByVersionId(Integer versionId);


    @Modifying
    @Query(
        nativeQuery = true,
        value = "INSERT INTO " +
                    "n2o_rdm_management.attribute_validation (version_id, attribute, type, value) " +
                "SELECT " +
                    ":to, attribute, type, value " +
                "FROM " +
                    "n2o_rdm_management.attribute_validation " +
                "WHERE " +
                    "version_id = :from"
    )
    void copy(@Param("from") Integer fromId, @Param("to") Integer toId);

}