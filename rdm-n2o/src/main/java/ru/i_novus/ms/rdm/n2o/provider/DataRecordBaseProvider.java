package ru.i_novus.ms.rdm.n2o.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.n2o.api.model.DataRecordRequest;
import ru.i_novus.ms.rdm.rest.client.impl.VersionRestServiceRestClient;

/**
 * Провайдер для формирования метаданных.
 */
@Service
public class DataRecordBaseProvider {

    private static final Logger logger = LoggerFactory.getLogger(DataRecordBaseProvider.class);

    private static final String CONTEXT_PARAM_SEPARATOR_REGEX = "_";

    protected VersionRestServiceRestClient versionService;

    @Autowired
    public void setVersionService(VersionRestServiceRestClient versionService) {
        this.versionService = versionService;
    }

    /**
     * Получение запроса из контекста провайдера.
     *
     * @param context параметры провайдера в формате versionId_pageType, где
     *                  versionId - идентификатор версии справочника,
     *                  pageType - тип действия (string)
     * @return Запрос
     */
    protected DataRecordRequest toRequest(String context) {

        final DataRecordRequest request = new DataRecordRequest();

        final String[] params = context.split(CONTEXT_PARAM_SEPARATOR_REGEX);

        final Integer versionId = Integer.parseInt(params[0]);
        final Structure structure = getStructureOrEmpty(versionId);

        request.setVersionId(versionId);
        request.setStructure(structure);
        request.setDataAction(params[1]);

        return request;
    }

    /**
     * Получение структуры по идентификатору версии без исключения.
     *
     * @param versionId идентификатор версии
     * @return Структура версии или пустая структура
     */
    protected Structure getStructureOrEmpty(Integer versionId) {
        try {
            return versionService.getStructure(versionId);

        } catch (Exception e) {
            logger.error("Structure is not received for metadata", e);

            return Structure.EMPTY;
        }
    }
}
