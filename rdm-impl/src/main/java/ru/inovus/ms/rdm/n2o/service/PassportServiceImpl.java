package ru.inovus.ms.rdm.n2o.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.entity.PassportAttributeEntity;
import ru.inovus.ms.rdm.entity.PassportValueEntity;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.n2o.model.refbook.RefBookUpdateRequest;
import ru.inovus.ms.rdm.n2o.model.version.RefBookVersion;
import ru.inovus.ms.rdm.repository.PassportValueRepository;
import ru.inovus.ms.rdm.repository.RefBookVersionRepository;
import ru.inovus.ms.rdm.n2o.service.api.*;
import ru.inovus.ms.rdm.validation.VersionValidation;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.springframework.util.StringUtils.isEmpty;
import static ru.inovus.ms.rdm.n2o.util.ModelGenerator.versionModel;

@Primary
@Service
@SuppressWarnings("unused")
public class PassportServiceImpl implements PassportService {

    private RefBookVersionRepository versionRepository;
    private PassportValueRepository passportValueRepository;

    private VersionValidation versionValidation;

    @Autowired
    public PassportServiceImpl(RefBookVersionRepository versionRepository,
                               PassportValueRepository passportValueRepository,
                               VersionValidation versionValidation) {
        this.versionRepository = versionRepository;
        this.passportValueRepository = passportValueRepository;

        this.versionValidation = versionValidation;
    }

    @Override
    @Transactional
    public RefBookVersion updatePassport(RefBookUpdateRequest refBookUpdateRequest) {

        Integer versionId = refBookUpdateRequest.getVersionId();
        versionValidation.validateVersionExists(versionId);

        RefBookVersionEntity refBookVersionEntity = versionRepository.getOne(versionId);
        updateVersionFromPassport(refBookVersionEntity, refBookUpdateRequest.getPassport());

        return versionModel(refBookVersionEntity);
    }

    private void updateVersionFromPassport(RefBookVersionEntity versionEntity,
                                           Map<String, String> newPassport) {
        if (newPassport == null) return;

        List<PassportValueEntity> valuesToRemove = versionEntity.getPassportValues().stream()
                .filter(passportValue ->
                        Objects.isNull(newPassport.get(passportValue.getAttribute().getCode()))
                ).collect(Collectors.toList());

        passportValueRepository.deleteAll(valuesToRemove);

        versionEntity.getPassportValues()
                .removeAll(valuesToRemove);

        newPassport.entrySet().stream()
                .filter(newValue -> !isEmpty(newValue.getValue()))
                .forEach(newValue -> {
                    PassportValueEntity oldValue = versionEntity.getPassportValues().stream()
                            .filter(pv -> newValue.getKey().equals(pv.getAttribute().getCode()))
                            .findFirst().orElse(null);

                    if (oldValue != null)
                        oldValue.setValue(newValue.getValue());
                    else {
                        PassportAttributeEntity entity = new PassportAttributeEntity(newValue.getKey());
                        versionEntity.getPassportValues()
                                .add(new PassportValueEntity(entity, newValue.getValue(), versionEntity));
                    }
                });
    }
}