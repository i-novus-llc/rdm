package ru.inovus.ms.rdm.service;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.data.domain.Page;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.inovus.ms.rdm.model.SearchDataCriteria;
import ru.inovus.ms.rdm.model.Structure;

import javax.ws.rs.GET;

public interface VersionService {

    @GET
    @ApiOperation("Получения записей версии, с фильтрацией")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет версии")
    })
    Page<RowValue> search(Integer versionId, SearchDataCriteria criteria);
    Structure getMetadata(Integer versionId);
}
