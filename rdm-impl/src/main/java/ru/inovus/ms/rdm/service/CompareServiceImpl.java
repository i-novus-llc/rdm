package ru.inovus.ms.rdm.service;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.entity.PassportAttributeEntity;
import ru.inovus.ms.rdm.entity.PassportValueEntity;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.repositiory.PassportAttributeRepository;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.service.api.CompareService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Primary
public class CompareServiceImpl implements CompareService {

    private RefBookVersionRepository versionRepository;
    private PassportAttributeRepository passportAttributeRepository;

    @Autowired
    public CompareServiceImpl(RefBookVersionRepository versionRepository,
                              PassportAttributeRepository passportAttributeRepository) {
        this.versionRepository = versionRepository;
        this.passportAttributeRepository = passportAttributeRepository;
    }

    @Override
    public PassportDiff comparePassports(Integer leftVersionId, Integer rightVersionId) {
        RefBookVersionEntity leftVersion = versionRepository.getOne(leftVersionId);
        RefBookVersionEntity rightVersion = versionRepository.getOne(rightVersionId);
        if (leftVersion == null || rightVersion == null)
            throw new UserException(new Message("version.not.found", leftVersion == null ? leftVersionId : rightVersionId));

        List<PassportAttributeEntity> passportAttributes = passportAttributeRepository.findAllByComparableIsTrue();
        List<PassportAttributeDiff> passportAttributeDiffList = new ArrayList<>();

        passportAttributes.forEach(passportAttribute -> {
            PassportValueEntity leftPassportValue = leftVersion.getPassportValues().stream().filter(passportValue -> passportValue.getAttribute().equals(passportAttribute)).findFirst().orElse(null);
            PassportValueEntity rightPassportValue = rightVersion.getPassportValues().stream().filter(passportValue -> passportValue.getAttribute().equals(passportAttribute)).findFirst().orElse(null);

            if (!equalValues(leftPassportValue, rightPassportValue)) {
                PassportAttributeDiff passportAttributeDiff = new PassportAttributeDiff(
                        new PassportAttribute(passportAttribute.getCode(), passportAttribute.getName()),
                        leftPassportValue != null ? leftPassportValue.getValue() : null,
                        rightPassportValue != null ? rightPassportValue.getValue() : null);
                passportAttributeDiffList.add(passportAttributeDiff);
            }
        });
        return new PassportDiff(passportAttributeDiffList);
    }

    @Override
    @Transactional(readOnly = true)
    public StructureDiff compareStructures(Integer oldVersionId, Integer newVersionId) {

        RefBookVersionEntity oldVersion = versionRepository.findOne(oldVersionId);
        RefBookVersionEntity newVersion = versionRepository.findOne(newVersionId);
        if (oldVersion == null || newVersion == null)
            throw new IllegalArgumentException("invalid.version.ids");

        List<StructureDiff.AttributeDiff> inserted = new ArrayList<>();
        List<StructureDiff.AttributeDiff> updated = new ArrayList<>();
        List<StructureDiff.AttributeDiff> deleted = new ArrayList<>();

        newVersion.getStructure().getAttributes().forEach(newAttribute -> {
            Optional<Structure.Attribute> oldAttribute = oldVersion.getStructure().getAttributes().stream()
                    .filter(o -> Objects.equals(newAttribute.getCode(), o.getCode())).findAny();
            if (!oldAttribute.isPresent()) {
                inserted.add(new StructureDiff.AttributeDiff(null, newAttribute));
            } else if (oldAttribute.get().equals(newAttribute)) {
                updated.add(new StructureDiff.AttributeDiff(oldAttribute.get(), newAttribute));
            }
        });
        oldVersion.getStructure().getAttributes().stream()
                .filter(oldAttribute -> newVersion.getStructure().getAttributes().stream()
                        .noneMatch(n -> Objects.equals(oldAttribute.getCode(), n.getCode())))
                .map(oldAttribute -> new StructureDiff.AttributeDiff(oldAttribute, null))
                .forEach(deleted::add);

        return new StructureDiff(inserted, updated, deleted);
    }

    private boolean equalValues(PassportValueEntity leftPassportValue, PassportValueEntity rightPassportValue) {
        if (leftPassportValue != null && leftPassportValue.getValue() != null)
            return (rightPassportValue != null && leftPassportValue.getValue().equals(rightPassportValue.getValue()));
        else
            return (rightPassportValue == null || rightPassportValue.getValue() == null);
    }

}