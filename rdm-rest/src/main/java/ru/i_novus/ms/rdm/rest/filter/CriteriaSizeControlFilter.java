package ru.i_novus.ms.rdm.rest.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Проверяет значение атрибута size в методах GET.
 * Значение не должно превышать SIZE_MAX_VALUE.
 * В случае превышения будет выброшена ошибка 400 (Bad Request).
 */
@PreMatching
@Provider
@Component
public class CriteriaSizeControlFilter implements ContainerRequestFilter {

    private static final String SIZE_QUERY_NAME = "size";
    private static final Integer SIZE_MAX_VALUE = 1000;

    private static final String SIZE_MAX_VALUE_EXCEEDED_EXCEPTION_CODE = "size must be no greater than " + SIZE_MAX_VALUE;

    @Override
    public void filter(ContainerRequestContext requestContext) {

        if ("GET".equalsIgnoreCase(requestContext.getMethod())) {
            String size = requestContext.getUriInfo().getQueryParameters().getFirst(SIZE_QUERY_NAME);
            if (size != null && Integer.parseInt(size) > SIZE_MAX_VALUE) {
                requestContext.abortWith(Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(SIZE_MAX_VALUE_EXCEEDED_EXCEPTION_CODE)
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .build());
            }
        }
    }
}
