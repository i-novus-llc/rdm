package ru.i_novus.ms.rdm.rest.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.cxf.jaxrs.impl.ContainerRequestContextImpl;
import org.apache.cxf.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * Преобразует запросы для VersionPlainDataService, позволяя писать их в более удобном виде
 * Принимает запросы с параметрами вида filter.[key]=[value]
 * и преобразует их в формат json для десериализации в Map
 */
@PreMatching
@Provider
@Component
public class PlainDataRequestFilter implements ContainerRequestFilter {

    public static final String FILTER_PREFIX = "filter.";
    public static final String PLAIN_DATA_URL_PREFIX = "plainData";
    public static final String PLAIN_FILTER_QUERY_PARAM = "plainAttributeFilter";

    @Autowired
    @Qualifier("cxfObjectMapper")
    private ObjectMapper objectMapper;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {

        if (!requestContext.getUriInfo().getPath().startsWith(PLAIN_DATA_URL_PREFIX))
            return;

        Map<String, String> attributeFilters = requestContext.getUriInfo().getQueryParameters().entrySet().stream()
                .filter(e -> e.getKey().startsWith(FILTER_PREFIX))
                .filter(e -> !isEmpty(e.getValue()) && isNotBlank(e.getValue().get(0)))
                .collect(toMap(e -> e.getKey().replace(FILTER_PREFIX, ""), e -> e.getValue().get(0)));

        if (isEmpty(attributeFilters))
            return;

        Message message = ((ContainerRequestContextImpl) requestContext).getMessage();
        String plainAttributeFilter = objectMapper.writeValueAsString(attributeFilters);

        String query = (String) message.get(Message.QUERY_STRING);
        query += "&" + PLAIN_FILTER_QUERY_PARAM + "=" +
                URLEncoder.encode(plainAttributeFilter, StandardCharsets.UTF_8);
        message.put(Message.QUERY_STRING, query);
    }
}