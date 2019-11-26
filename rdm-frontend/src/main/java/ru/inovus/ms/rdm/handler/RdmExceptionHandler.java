package ru.inovus.ms.rdm.handler;
import net.n2oapp.criteria.dataset.DataSet;
import net.n2oapp.framework.api.criteria.N2oPreparedCriteria;
import net.n2oapp.framework.api.data.QueryExceptionHandler;
import net.n2oapp.framework.api.exception.N2oException;
import net.n2oapp.framework.api.exception.N2oUserException;
import net.n2oapp.framework.api.metadata.local.CompiledObject;
import net.n2oapp.framework.api.metadata.local.CompiledQuery;
import net.n2oapp.framework.engine.data.N2oOperationExceptionHandler;
import net.n2oapp.platform.jaxrs.RestException;
import net.n2oapp.platform.web.autoconfigure.PlatformExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;

/**
 * Декоратор для PlatformExceptionHandler.
 * Если ошибка содержит список ошибок, то используется текущая реализация, конкатенирующая список ошибок с нумерацией
 * Если стандартная ошибка - обработка делегируется PlatformExceptionHandler
 */
public class RdmExceptionHandler extends N2oOperationExceptionHandler implements QueryExceptionHandler {

    @Autowired
    private PlatformExceptionHandler platformExceptionHandler;

    @Override
    public N2oException handle(CompiledObject.Operation operation, DataSet dataSet, Exception e) {
        if (isMultipleErrorsException(e)) {
            return handleMultipleErrorsException(e);
        }
        return platformExceptionHandler.handle(operation, dataSet, e);
    }

    @Override
    public N2oException handle(CompiledQuery compiledQuery, N2oPreparedCriteria n2oPreparedCriteria, Exception e) {
        if (isMultipleErrorsException(e)) {
            return handleMultipleErrorsException(e);
        }
        return platformExceptionHandler.handle(compiledQuery, n2oPreparedCriteria, e);
    }

    private boolean isMultipleErrorsException(Exception e) {
        if (e instanceof N2oException && e.getCause() instanceof RestException) {
            RestException restException = (RestException) e.getCause();
            return restException.getErrors() != null;
        }
        return false;
    }

    private N2oException handleMultipleErrorsException(Exception e) {
        RestException restException = (RestException) e.getCause();
        String message = IntStream
                .rangeClosed(1, restException.getErrors().size())
                .mapToObj(i -> i + ") " + restException.getErrors().get(i - 1).getMessage())
                .collect(joining("; "));
        return new N2oUserException(message);
    }

}
