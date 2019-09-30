package ru.inovus.ms.rdm.impl.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.api.model.refbook.RefBookUpdateRequest;
import ru.inovus.ms.rdm.api.model.version.RefBookVersion;
import ru.inovus.ms.rdm.api.service.PassportService;
import ru.inovus.ms.rdm.api.validation.VersionValidation;
import ru.inovus.ms.rdm.impl.audit.AuditAction;
import ru.inovus.ms.rdm.impl.entity.PassportAttributeEntity;
import ru.inovus.ms.rdm.impl.entity.PassportValueEntity;
import ru.inovus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.impl.repository.PassportValueRepository;
import ru.inovus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.inovus.ms.rdm.impl.util.JsonPayload;
import ru.inovus.ms.rdm.impl.util.ModelGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;
import static org.springframework.util.StringUtils.isEmpty;

@Primary
@Service
@SuppressWarnings("unused")
public class PassportServiceImpl implements PassportService {

    private RefBookVersionRepository versionRepository;
    private PassportValueRepository passportValueRepository;

    private VersionValidation versionValidation;

    private AuditLogService auditLogService;

    @Autowired
    public PassportServiceImpl(RefBookVersionRepository versionRepository,
                               PassportValueRepository passportValueRepository,
                               VersionValidation versionValidation, AuditLogService auditLogService) {
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

        RefBookVersionEntity refBookVersionEntity = versionRepository.getOne(versionId);
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

        List<Map.Entry<String, Map.Entry<String, String>>> editedValues = new ArrayList<>();
        List<Map.Entry<String, String>> addedValues = new ArrayList<>();
        newPassport.entrySet().stream()
                .filter(newValue -> !isEmpty(newValue.getValue()))
                .forEach(newValue -> {
                    PassportValueEntity oldValue = versionEntity.getPassportValues().stream()
                            .filter(pv -> newValue.getKey().equals(pv.getAttribute().getCode()))
                            .findFirst().orElse(null);

                    if (oldValue != null) {
                        editedValues.add(
                            Map.entry(
                                newValue.getKey(),
                                Map.entry(oldValue.getValue(), newValue.getValue())
                            )
                        );
                        oldValue.setValue(newValue.getValue());
                    } else {
                        addedValues.add(Map.entry(newValue.getKey(), newValue.getValue()));
                        PassportAttributeEntity entity = new PassportAttributeEntity(newValue.getKey());
                        versionEntity.getPassportValues()
                                .add(new PassportValueEntity(entity, newValue.getValue(), versionEntity));
                    }
                });
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"deleted\": [");
        sb.append(valuesToRemove.stream().map(p -> "\"" + p.getAttribute().getCode() + "\"").collect(joining(", ")));
        sb.append("], \"added\": [");
        sb.append(addedValues.stream().map(e -> "{\"" + e.getKey() + "\": \"" + e.getValue() + "\"}").collect(joining(", ")));
        sb.append("], \"edited\": [");
        sb.append(editedValues.stream().map(e -> "{\"" + e.getKey() + "\": {\"old\": \"" + e.getValue().getKey() + "\", \"new\": \"" + e.getValue().getValue() + "\"}}").collect(joining(", ")));
        sb.append("]}");
        passportValueRepository.deleteAll(valuesToRemove);

        versionEntity.getPassportValues()
                .removeAll(valuesToRemove);
        auditLogService.addAction(
            AuditAction.EDIT_PASSPORT,
            versionEntity,
            Map.of("operations", new JsonPayload(sb.toString()))
        );
    }
}