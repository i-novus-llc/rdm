package ru.inovus.ms.rdm.util;

import org.springframework.stereotype.Component;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;

/**
 * Проверяет значение атрибута size в методах GET
 * Значение не должно превышать 100
 * В случае превышения будет выброшена ошибка 400 (Bad Request)
 */
@PreMatching
@Provider
@Component
public class CriteriaSizeControlFilter implements ContainerRequestFilter {

    private static final String SIZE_QUERY_NAME = "size";
    private static final Integer SIZE_MAX_VALUE = 100;

    private static final String SIZE_MAX_VALUE_EXCEEDED_EXCEPTION_CODE = "size must be less than 100";

    @Override
    public void filter(ContainerRequestContext requestContext) {

        if ("GET".equalsIgnoreCase(requestContext.getMethod())) {
            String size = requestContext.getUriInfo().getQueryParameters().getFirst(SIZE_QUERY_NAME);
            if (size != null && Integer.valueOf(size) > SIZE_MAX_VALUE) {
                throw new BadRequestException(SIZE_MAX_VALUE_EXCEEDED_EXCEPTION_CODE);
            }
        }
    }

}
