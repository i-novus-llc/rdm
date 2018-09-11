package ru.inovus.ms.rdm.service;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import ru.inovus.ms.rdm.entity.PassportAttributeEntity;
import ru.inovus.ms.rdm.entity.PassportValueEntity;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.model.PassportAttribute;
import ru.inovus.ms.rdm.model.PassportAttributeDiff;
import ru.inovus.ms.rdm.model.PassportDiff;
import ru.inovus.ms.rdm.repositiory.PassportAttributeRepository;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.service.api.CompareService;

import java.util.ArrayList;
import java.util.List;

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

    private boolean equalValues(PassportValueEntity leftPassportValue, PassportValueEntity rightPassportValue) {
        if (leftPassportValue != null && leftPassportValue.getValue() != null)
            return (rightPassportValue != null && leftPassportValue.getValue().equals(rightPassportValue.getValue()));
        else
            return (rightPassportValue == null || rightPassportValue.getValue() == null);
    }

}