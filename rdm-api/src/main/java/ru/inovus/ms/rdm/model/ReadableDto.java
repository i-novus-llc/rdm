package ru.inovus.ms.rdm.model;

import java.time.LocalDateTime;

public class ReadableDto {

    private LocalDateTime creationDate;

    private LocalDateTime lastActionDate;

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDateTime getLastActionDate() {
        return lastActionDate;
    }

    public void setLastActionDate(LocalDateTime lastActionDate) {
        this.lastActionDate = lastActionDate;
    }
}
