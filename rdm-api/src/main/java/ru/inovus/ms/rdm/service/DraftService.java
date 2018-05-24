package ru.inovus.ms.rdm.service;

import ru.inovus.ms.rdm.model.*;

import java.time.OffsetDateTime;

public interface DraftService {
    Draft create(Long dictionaryId, Metadata metadata);
    void updateMetadata(Long draftId, MetadataDiff metadataDiff);
    void updateData(Long draftId, DataDiff dataDiff);
    void updateData(Long draftId, FileData file);


    Data search(Long draftId, DraftCriteria criteria);

    void publish(Long draftId, String versionName, OffsetDateTime versionDate);
    void remove(Long draftId);

    Metadata getMetadata(Long draftId);


}
