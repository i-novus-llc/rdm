package ru.i_novus.ms.rdm.n2o.api.constant;

import java.util.Arrays;
import java.util.List;

public class DataRecordConstants {

    public static final String DATA_ACTION_CREATE = "create";
    public static final String DATA_ACTION_UPDATE = "update";

    private static final List<String> DATA_ACTIONS = Arrays.asList(DATA_ACTION_CREATE, DATA_ACTION_UPDATE);

    // Поля.
    public static final String FIELD_SYSTEM_ID = "id";
    public static final String FIELD_ROW_TYPE = "rowType";

    public static final String FIELD_VERSION_ID = "versionId";
    public static final String FIELD_OPT_LOCK_VALUE = "optLockValue";
    public static final int DEFAULT_OPT_LOCK_VALUE = 0;
    public static final String FIELD_LOCALE_CODE = "localeCode";
    public static final String DEFAULT_LOCALE_CODE = "";
    public static final String FIELD_DATA_ACTION = "dataAction";

    public static final String REFERENCE_QUERY_ID = "reference";
    public static final String REFERENCE_VALUE = "value";
    public static final String REFERENCE_DISPLAY_VALUE = "displayValue";

    // Фильтрация.
    public static final String FILTER_PREFIX = "filter.";

    public static final String BOOL_FIELD_ID = "id";
    public static final String BOOL_FIELD_NAME = "name";

    private DataRecordConstants() {
        // Nothing to do.
    }

    public static boolean containsDataAction(String dataAction) {
        return DATA_ACTIONS.contains(dataAction);
    }
}
