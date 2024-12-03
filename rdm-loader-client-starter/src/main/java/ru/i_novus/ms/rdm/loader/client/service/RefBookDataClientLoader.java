package ru.i_novus.ms.rdm.loader.client.service;

import net.n2oapp.platform.loader.client.ClientLoader;
import net.n2oapp.platform.loader.client.LoadingException;
import net.n2oapp.platform.loader.client.RestClientLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;
import ru.i_novus.ms.rdm.api.model.loader.RefBookDataUpdateTypeEnum;
import ru.i_novus.ms.rdm.loader.client.model.RefBookDataModel;
import ru.i_novus.ms.rdm.loader.client.util.RefBookDataUtil;

import java.net.URI;
import java.util.List;

import static ru.i_novus.ms.rdm.api.model.loader.RefBookDataUpdateTypeEnum.CREATE_ONLY;
import static ru.i_novus.ms.rdm.api.util.loader.RefBookDataConstants.*;
import static ru.i_novus.ms.rdm.loader.client.util.RefBookDataUtil.isEmpty;

public class RefBookDataClientLoader extends RestClientLoader<MultiValueMap<String, Object>> implements ClientLoader {

    private static final Logger logger = LoggerFactory.getLogger(RefBookDataClientLoader.class);

    public RefBookDataClientLoader(RestOperations restTemplate) {
        super(restTemplate);
    }

    public RefBookDataClientLoader(RestOperations restTemplate, String endpointPattern) {
        super(restTemplate, endpointPattern);
    }

    @Override
    public void load(URI server, String subject, String target, Resource file) {

        final List<RefBookDataModel> models = RefBookDataUtil.toRefBookDataModels(file);
        final String url = getUrl(server, subject, target);

        models.forEach(model -> load(url, model));
    }

    private void load(String url, RefBookDataModel model) {

        final MultiValueMap<String, Object> data = getData(model);
        final MultiValueMap<String, String> headers = getHeaders();

        logger.debug("POST: request to load {}", model.getCode());
        final HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(data, headers);
        final ResponseEntity<String> response = getRestTemplate().postForEntity(url, request, String.class);
        logger.debug("POST: response from load {}", model.getCode());

        final HttpStatusCode statusCode = response.getStatusCode();
        logger.debug("POST: response status: {}", statusCode.value());

        if (!statusCode.is2xxSuccessful())
            throw new LoadingException("Loading failed status " + statusCode.value() + " response " + response.getBody());
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

    /**
     * Формирование данных по модели справочника.
     *
     * @param model модель справочника
     * @return Данные
     */
    private MultiValueMap<String, Object> getData(RefBookDataModel model) {

        final MultiValueMap<String, Object> body = new LinkedMultiValueMap<>(7);

        final String changeSetId = model.getChangeSetId();
        body.add(FIELD_CHANGE_SET_ID, !isEmpty(changeSetId) ? changeSetId : "");

        final RefBookDataUpdateTypeEnum updateType = model.getUpdateType();
        body.add(FIELD_UPDATE_TYPE, (updateType != null ? updateType : CREATE_ONLY).name());

        if (!isEmpty(model.getCode())) {
            body.add(FIELD_REF_BOOK_CODE, model.getCode());
        }

        if (!isEmpty(model.getName())) {
            body.add(FIELD_REF_BOOK_NAME, model.getName());
        }

        if (!isEmpty(model.getStructure())) {
            body.add(FIELD_REF_BOOK_STRUCTURE, model.getStructure());
        }

        if (!isEmpty(model.getData())) {
            body.add(FIELD_REF_BOOK_DATA, model.getData());
        }

        if (model.getFile() != null) {
            body.add(FIELD_REF_BOOK_FILE, getFilePart(model));
        }

        return body;
    }

    private HttpEntity<Resource> getFilePart(RefBookDataModel model) {

        final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>(1);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML_VALUE);
        headers.put(HttpHeaders.CONTENT_DISPOSITION, List.of("filename=" + model.getFile().getFilename()));

        return new HttpEntity<>(model.getFile(), headers);
    }
}
