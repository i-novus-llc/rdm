package ru.inovus.ms.rdm.util;

import java.time.LocalDateTime;

public interface VersionPeriodPublishValidation {
    void validate(LocalDateTime fromDate, LocalDateTime toDate, Integer refBookId);
}