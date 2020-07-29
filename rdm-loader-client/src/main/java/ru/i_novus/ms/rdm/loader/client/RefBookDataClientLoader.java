package ru.i_novus.ms.rdm.loader.client;

import net.n2oapp.platform.loader.client.ClientLoader;
import net.n2oapp.platform.loader.client.RestClientLoader;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;

@Component
public class RefBookDataClientLoader
        extends RestClientLoader<MultiValueMap<String, Object>> implements ClientLoader {

    public RefBookDataClientLoader(RestOperations restTemplate) {
        super(restTemplate);
    }

    public RefBookDataClientLoader(RestOperations restTemplate, String endpointPattern) {
        super(restTemplate, endpointPattern);
    }

    @Override
    protected MultiValueMap<String, String> getHeaders() {

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>(1);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE);
        return headers;
    }

    @Override
    protected MultiValueMap<String, Object> getData(Resource file) {

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file);
        return body;
    }
}
