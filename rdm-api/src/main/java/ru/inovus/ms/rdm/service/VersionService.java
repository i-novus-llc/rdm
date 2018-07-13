package ru.inovus.ms.rdm.service;

import io.swagger.annotations.*;
import org.springframework.data.domain.Page;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.inovus.ms.rdm.model.SearchDataCriteria;
import ru.inovus.ms.rdm.model.Structure;

import javax.ws.rs.*;
import java.time.OffsetDateTime;
import javax.ws.rs.core.MediaType;

@Path("/version")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api("Методы работы с версиями справочника")
public interface VersionService {

    @GET
    @ApiOperation("Получения записей версии, с фильтрацией")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет версии")
    })
    Page<RowValue> search(Integer versionId, SearchDataCriteria criteria);

    @GET
    @ApiOperation("Получения записей версии актуальных на дату, с фильтрацией ")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет версии")
    })
    Page<RowValue> search(Integer refbookId, OffsetDateTime date, SearchDataCriteria criteria);

    @GET
    @Path("/structure")
    @ApiOperation("Получение структуры версии справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Структура версии справочника"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    Structure getStructure(@QueryParam("versionId") @ApiParam("Идентификатор версии") Integer versionId);
}
