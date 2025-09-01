package ru.i_novus.ms.rdm.impl.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookUpdateRequest;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.service.PassportService;
import ru.i_novus.ms.rdm.api.util.StringUtils;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.entity.PassportAttributeEntity;
import ru.i_novus.ms.rdm.impl.entity.PassportValueEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.PassportValueRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.util.ModelGenerator;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static ru.i_novus.ms.rdm.impl.audit.AuditAction.EDIT_PASSPORT;

@Primary
@Service
@SuppressWarnings("unused")
public class PassportServiceImpl implements PassportService {

    private final RefBookVersionRepository versionRepository;
    private final PassportValueRepository passportValueRepository;

    private final VersionValidation versionValidation;

    private final AuditLogService auditLogService;

    @Autowired
    public PassportServiceImpl(RefBookVersionRepository versionRepository,
                               PassportValueRepository passportValueRepository,
                               VersionValidation versionValidation,
                               AuditLogService auditLogService) {
        this.versionRepository = versionRepository;
        this.passportValueRepository = passportValueRepository;

        this.versionValidation = versionValidation;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public RefBookVersion updatePassport(RefBookUpdateRequest refBookUpdateRequest) {

        Integer versionId = refBookUpdateRequest.getVersionId();
        versionValidation.validateVersionExists(versionId);

        RefBookVersionEntity refBookVersionEntity = versionRepository.getReferenceById(versionId);
        updateVersionFromPassport(refBookVersionEntity, refBookUpdateRequest.getPassport());

        return ModelGenerator.versionModel(refBookVersionEntity);
    }

    private void updateVersionFromPassport(RefBookVersionEntity versionEntity,
                                           Map<String, String> newPassport) {
        if (newPassport == null) return;

        List<PassportValueEntity> valuesToRemove = versionEntity.getPassportValues().stream()
                .filter(passportValue ->
                        Objects.isNull(newPassport.get(passportValue.getAttribute().getCode()))
                ).collect(Collectors.toList());

        newPassport.entrySet().stream()
                .filter(newValue -> !StringUtils.isEmpty(newValue.getValue()))
                .forEach(newValue -> {
                    PassportValueEntity oldValue = versionEntity.getPassportValues().stream()
                            .filter(pv -> newValue.getKey().equals(pv.getAttribute().getCode()))
                            .findFirst().orElse(null);

                    if (oldValue != null) {
                        oldValue.setValue(newValue.getValue());
                    } else {
                        PassportAttributeEntity entity = new PassportAttributeEntity(newValue.getKey());
                        versionEntity.getPassportValues()
                                .add(new PassportValueEntity(entity, newValue.getValue(), versionEntity));
                    }
                });

        passportValueRepository.deleteAll(valuesToRemove);

        versionEntity.getPassportValues().removeAll(valuesToRemove);

        auditLogService.addAction(EDIT_PASSPORT, () -> versionEntity, Map.of("newPassport", newPassport));
    }
}