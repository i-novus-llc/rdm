package ru.inovus.ms.rdm.esnsi;

public enum ClassifierProcessingStage {
    NONE,
    GET_REVISIONS_COUNT,
    GET_LAST_REVISION,
    GET_STRUCTURE,
    GET_RECORDS_COUNT,
    GET_DATA,
    SENDING_TO_RDM
}
