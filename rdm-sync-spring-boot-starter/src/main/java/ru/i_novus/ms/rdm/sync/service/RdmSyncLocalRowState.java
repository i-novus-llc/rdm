package ru.i_novus.ms.rdm.sync.service;

public enum RdmSyncLocalRowState {

    DIRTY,
    PENDING,
    SYNCED,
    ERROR;

    public static final String RDM_SYNC_INTERNAL_STATE_COLUMN = "rdm_sync_internal_local_row_state";

}
