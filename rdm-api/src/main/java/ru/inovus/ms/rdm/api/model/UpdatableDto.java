package ru.inovus.ms.rdm.api.model;

import java.time.LocalDateTime;

public class UpdatableDto {

    private LocalDateTime lastActionDate;

    public LocalDateTime getLastActionDate() {
        return lastActionDate;
    }

    public void setLastActionDate(LocalDateTime lastActionDate) {
        this.lastActionDate = lastActionDate;
    }
}
