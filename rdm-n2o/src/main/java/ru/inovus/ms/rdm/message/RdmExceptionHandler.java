package ru.inovus.ms.rdm.message;

import net.n2oapp.criteria.dataset.DataSet;
import net.n2oapp.framework.api.criteria.N2oPreparedCriteria;
import net.n2oapp.framework.api.data.QueryExceptionHandler;
import net.n2oapp.framework.api.exception.N2oException;
import net.n2oapp.framework.api.exception.N2oUserException;
import net.n2oapp.framework.api.metadata.local.CompiledObject;
import net.n2oapp.framework.api.metadata.local.CompiledQuery;
import net.n2oapp.framework.api.util.RestClient;
import net.n2oapp.framework.engine.data.N2oOperationExceptionHandler;
import net.n2oapp.platform.jaxrs.RestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;

/**
 * Получение сообщений для пользователя из исключений от REST сервисов.
 */
public class RdmExceptionHandler extends N2oOperationExceptionHandler implements QueryExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(RdmExceptionHandler.class);
    @Override
    public N2oException handle(CompiledObject.Operation operation, DataSet dataSet, Exception e) {
        N2oException exception = handle(e);
        if (exception != null)
            return exception;
        return super.handle(operation, dataSet, e);
    }

    @Override
    public N2oException handle(CompiledQuery compiledQuery, N2oPreparedCriteria n2oPreparedCriteria, Exception e) {
        N2oException exception = handle(e);
        if (exception != null)
            return exception;
        if (e instanceof N2oException)
            return (N2oException) e;
        return new N2oException(e);
    }

    @SuppressWarnings("all")
    private N2oException handle(Exception e) {
        logger.error("handled error", e);
        if (e instanceof RestClient.RestException) {
            RestClient.RestException restException = (RestClient.RestException) e;
            if (restException.getHttpStatus() >= 400 && restException.getHttpStatus() < 500) {
                String message = restException.getBody().getString("message");
                return new N2oUserException(message);
            } else if (restException.getHttpStatus() >= 500 && restException.getHttpStatus() < 600) {
                String message = restException.getBody().getString("message");
                List<String> stackTrace = (List<String>) restException.getBody().getList("stackTrace");
                return new RdmRestException(message, stackTrace, e);
            }
        } else if (e instanceof N2oException) {
            N2oException n2oex = (N2oException) e;
            if (n2oex.getCause() instanceof RestException) {
                RestException restException = (RestException) n2oex.getCause();
                if (restException.getErrors() == null)
                    return null;
                String message = IntStream
                        .rangeClosed(1, restException.getErrors().size())
                        .mapToObj(i -> i + ") " + restException.getErrors().get(i - 1).getMessage())
                        .collect(joining("; "));
                return new N2oUserException(message, n2oex.getMessage());
            } else {
                return n2oex;
            }
        }
        return null;
    }
}
