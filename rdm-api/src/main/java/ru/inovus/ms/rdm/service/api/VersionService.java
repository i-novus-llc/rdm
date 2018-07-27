package ru.inovus.ms.rdm.service.api;

import io.swagger.annotations.*;
import org.springframework.data.domain.Page;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.inovus.ms.rdm.model.SearchDataCriteria;
import ru.inovus.ms.rdm.model.Structure;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.OffsetDateTime;

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
    @Path("/{versionId}")
    Page<RowValue> search(@PathParam("versionId")Integer versionId, SearchDataCriteria criteria);

    @GET
    @Path(("/refbook/{refbookId}/{date}"))
    @ApiOperation("Получения записей версии актуальных на дату, с фильтрацией ")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет версии")
    })
    Page<RowValue> search(@ApiParam("Идентификатор справочника") @PathParam("refbookId")Integer refbookId,
                          @ApiParam("Дата получения данных") @PathParam("date") OffsetDateTime date,
                          @BeanParam SearchDataCriteria criteria);

    @GET
    @Path("/structure")
    @ApiOperation("Получение структуры версии справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Структура версии справочника"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    Structure getStructure(@QueryParam("versionId") @ApiParam("Идентификатор версии") Integer versionId);
}
