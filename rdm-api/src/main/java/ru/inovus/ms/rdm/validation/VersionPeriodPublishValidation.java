package ru.inovus.ms.rdm.validation;

import java.time.LocalDateTime;

public interface VersionPeriodPublishValidation {
    void validate(LocalDateTime fromDate, LocalDateTime toDate, Integer refBookId);
}