package ru.i_novus.ms.rdm.api.util;

import ru.i_novus.ms.rdm.api.async.AsyncOperationMessage;

public class AsyncOperationUtils {

    private static final String OPERATION_LOG_FORMAT = "id: %s, type: %s";

    private AsyncOperationUtils() {
        // Nothing to do.
    }

    public static String toOperationLogText(AsyncOperationMessage message) {

        return String.format(OPERATION_LOG_FORMAT, message.getOperationId(), message.getOperationType());
    }
}
