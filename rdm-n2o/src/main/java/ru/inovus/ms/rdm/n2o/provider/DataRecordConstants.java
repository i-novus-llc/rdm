package ru.inovus.ms.rdm.n2o.provider;

public class DataRecordConstants {

    public static final String DATA_ACTION_CREATE = "create";
    public static final String DATA_ACTION_EDIT = "edit";

    public static final String FIELD_SYSTEM_ID = "id";
    public static final String FIELD_SYS_RECORD_ID = "sysRecordId";
    public static final String FIELD_VERSION_ID = "versionId";
    public static final String FIELD_OPT_LOCK_VALUE = "optLockValue";
    public static final int DEFAULT_OPT_LOCK_VALUE = 0;
    public static final String FIELD_DATA_ACTION = "dataAction";
    public static final String FIELD_FILTERS = String.format("%s,%s,%s,%s",
            FIELD_VERSION_ID, FIELD_SYS_RECORD_ID, FIELD_OPT_LOCK_VALUE, FIELD_DATA_ACTION);

    public static final String REFERENCE_QUERY_ID = "reference";
    public static final String REFERENCE_VALUE = "value";
    public static final String REFERENCE_DISPLAY_VALUE = "displayValue";

    private DataRecordConstants() {
    }
}
