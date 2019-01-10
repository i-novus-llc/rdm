package ru.inovus.ms.rdm.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.cxf.jaxrs.impl.ContainerRequestContextImpl;
import org.apache.cxf.message.Message;
import org.springframework.stereotype.Component;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.springframework.util.CollectionUtils.isEmpty;

@PreMatching
@Provider
@Component
public class PlainDataRequestFilter implements ContainerRequestFilter {

    public static final String FILTER_PREFIX = "filter.";
    public static final String PLAIN_DATA_URL_PREFIX = "plainData";
    public static final String PLAIN_FILTER_QUERY_PARAM = "plainAttributeFilter";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (requestContext.getUriInfo().getPath().startsWith(PLAIN_DATA_URL_PREFIX)) {
            Message message = ((ContainerRequestContextImpl) requestContext).getMessage();

            Map<String, String> plainAttributeFilter = requestContext.getUriInfo().getQueryParameters().entrySet().stream()
                    .filter(e -> e.getKey().startsWith(FILTER_PREFIX))
                    .filter(e -> !isEmpty(e.getValue()) && isNotBlank(e.getValue().get(0)))
                    .collect(Collectors.toMap(e -> e.getKey().replace(FILTER_PREFIX, ""), e -> e.getValue().get(0)));

            if (!isEmpty(plainAttributeFilter)) {
                String s = (String) message.get(Message.QUERY_STRING);
                s += "&" + PLAIN_FILTER_QUERY_PARAM + "=" + new ObjectMapper().writeValueAsString(plainAttributeFilter);
                message.put(Message.QUERY_STRING, s);
            }
        }
    }
}