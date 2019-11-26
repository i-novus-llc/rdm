package ru.inovus.ms.rdm.impl.validation;

import org.springframework.stereotype.Component;
import ru.inovus.ms.rdm.api.validation.VersionPeriodPublishValidation;

import java.time.LocalDateTime;

@Component
public class VersionPeriodPublishValidationImpl implements VersionPeriodPublishValidation {

    public void validate(LocalDateTime fromDate, LocalDateTime toDate, Integer refBookId){
        //redefine if necessary
    }
}