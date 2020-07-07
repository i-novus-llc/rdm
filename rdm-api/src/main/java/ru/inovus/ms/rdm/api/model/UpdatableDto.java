package ru.inovus.ms.rdm.api.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Модель изменения данных для передачи. */
public class UpdatableDto implements Serializable {

    private LocalDateTime lastActionDate;

    public LocalDateTime getLastActionDate() {
        return lastActionDate;
    }

    public void setLastActionDate(LocalDateTime lastActionDate) {
        this.lastActionDate = lastActionDate;
    }
}
