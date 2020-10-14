package ru.i_novus.ms.rdm.loader.client.loader;

import net.n2oapp.platform.loader.client.ClientLoader;
import net.n2oapp.platform.loader.client.LoadingException;
import net.n2oapp.platform.loader.client.RestClientLoader;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;

import java.net.URI;
import java.util.List;

import static org.springframework.util.StringUtils.isEmpty;

public class RefBookDataClientLoader extends RestClientLoader<MultiValueMap<String, Object>> implements ClientLoader {

    public RefBookDataClientLoader(RestOperations restTemplate) {
        super(restTemplate);
    }

    public RefBookDataClientLoader(RestOperations restTemplate, String endpointPattern) {
        super(restTemplate, endpointPattern);
    }

    @Override
    public void load(URI server, String subject, String target, Resource file) {

        List<RefBookDataModel> models = RefBookDataUtil.toRefBookDataModels(file);

        String url = getUrl(server, subject, target);
        MultiValueMap<String, String> headers = getHeaders();

        models.forEach(model -> load(url, getData(model), headers));
    }

    private MultiValueMap<String, Object> getData(RefBookDataModel model) {

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        if (!isEmpty(model.getCode())) {
            body.add("code", model.getCode());
        }

        if (!isEmpty(model.getName())) {
            body.add("name", model.getName());
        }

        if (!isEmpty(model.getStructure())) {
            body.add("structure", model.getStructure());
        }

        if (!isEmpty(model.getData())) {
            body.add("data", model.getData());
        }

        if (model.getFile() != null) {
            body.add("file", model.getFile());
        }

        return body;
    }

    @Override
    protected MultiValueMap<String, String> getHeaders() {

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>(1);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE);
        return headers;
    }

    @Override
    protected MultiValueMap<String, Object> getData(Resource file) {

        return null; // Nothing to do.
    }

    private void load(String url, MultiValueMap<String, Object> data, MultiValueMap<String, String> headers) {

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(data, headers);
        ResponseEntity<String> response = getRestTemplate().postForEntity(url, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful())
            throw new LoadingException("Loading failed status " + response.getStatusCodeValue() + " response " + response.getBody());
    }
}
