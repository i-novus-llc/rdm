package ru.i_novus.ms.rdm.api.validation;

import java.time.LocalDateTime;

public interface VersionPeriodPublishValidation {
    void validate(LocalDateTime fromDate, LocalDateTime toDate, Integer refBookId);
}