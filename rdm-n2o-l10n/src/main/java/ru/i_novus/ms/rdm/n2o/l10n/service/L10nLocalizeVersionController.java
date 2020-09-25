package ru.i_novus.ms.rdm.n2o.l10n.service;

import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.RestTemplate;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.api.util.RowUtils;
import ru.i_novus.ms.rdm.n2o.api.model.UiDraft;

import java.nio.charset.StandardCharsets;

@Controller
@SuppressWarnings("unused") // used in: L10nLocalizeRecordObjectResolver
public class L10nLocalizeVersionController {

    private static final String DATA_ROW_IS_EMPTY_EXCEPTION_CODE = "data.row.is.empty";

    @SuppressWarnings("java:S1075")
    private static final String LOCALIZE_DATA_FORMAT = "/%d/data";

    @Value("${rdm.backend.path}")
    private String restUrl;

    public UiDraft localizeDataRecord(Integer versionId, Integer optLockValue, String localeCode, Row row) {

        validatePresent(row);

        //localizeData(versionId);

        return null;
    }

    //private void localizeData(Integer versionId, LocalizeDataRequest request) {
    //    try {
    //        RestTemplate restTemplate = createRestTemplate();
    //
    //        HttpHeaders headers = new HttpHeaders();
    //        headers.setContentType(MediaType.APPLICATION_JSON);
    //        HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);
    //
    //        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(restUrl)
    //                .path(String.format(LOCALIZE_DATA_FORMAT, versionId));
    //        String answer = restTemplate.postForObject(builder.toUriString(), entity, String.class);
    //
    //    } catch (RuntimeException e) {
    //    }
    //}

    /** Проверка на заполненность хотя бы одного поля в записи. */
    private void validatePresent(Row row) {
        if (RowUtils.isEmptyRow(row))
            throw new UserException(DATA_ROW_IS_EMPTY_EXCEPTION_CODE);
    }

    private RestTemplate createRestTemplate() {

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

        return restTemplate;
    }
}
