package ru.inovus.ms.rdm.validation;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class VersionPeriodPublishValidationImpl implements VersionPeriodPublishValidation {

    public void validate(LocalDateTime fromDate, LocalDateTime toDate, Integer refBookId){
        //redefine if necessary
    }
}