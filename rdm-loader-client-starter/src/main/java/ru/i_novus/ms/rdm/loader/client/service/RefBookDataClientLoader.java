package ru.i_novus.ms.rdm.loader.client.service;

import net.n2oapp.platform.loader.client.ClientLoader;
import net.n2oapp.platform.loader.client.LoadingException;
import net.n2oapp.platform.loader.client.RestClientLoader;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;
import ru.i_novus.ms.rdm.loader.client.model.RefBookDataModel;
import ru.i_novus.ms.rdm.loader.client.util.RefBookDataUtil;

import java.net.URI;
import java.util.List;

import static ru.i_novus.ms.rdm.loader.client.model.RefBookDataUpdateTypeEnum.CREATE_ONLY;
import static ru.i_novus.ms.rdm.loader.client.util.RefBookDataUtil.isEmpty;

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

        final MultiValueMap<String, Object> body = new LinkedMultiValueMap<>(7);

        body.add("change_set_id", !isEmpty(model.getChangeSetId()) ? model.getChangeSetId() : "");
        body.add("update_type", model.getUpdateType() != null ? model.getUpdateType() : CREATE_ONLY);

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

        final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>(1);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE);

        return headers;
    }

    @Override
    protected MultiValueMap<String, Object> getData(Resource file) {

        return null; // Nothing to do.
    }

    private void load(String url, MultiValueMap<String, Object> data, MultiValueMap<String, String> headers) {

        final HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(data, headers);
        final ResponseEntity<String> response = getRestTemplate().postForEntity(url, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful())
            throw new LoadingException("Loading failed status " + response.getStatusCodeValue() + " response " + response.getBody());
    }
}
