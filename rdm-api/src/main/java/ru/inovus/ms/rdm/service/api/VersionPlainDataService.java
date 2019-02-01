package ru.inovus.ms.rdm.service.api;

import io.swagger.annotations.*;
import org.springframework.data.domain.Page;
import ru.inovus.ms.rdm.model.SearchDataCriteria;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.OffsetDateTime;
import java.util.Map;
@Path("/plainData/version")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api("Методы получения данных версий справочника")
public interface VersionPlainDataService {
    @GET
    @ApiOperation("Получения записей версии, с фильтрацией")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет версии")
    })
    @Path("/{versionId}/data")
    Page<Map<String, Object>> search(@PathParam("versionId")Integer versionId, @BeanParam SearchDataCriteria criteria);


    @GET
    @Path("/refBook/{refBookCode}/{date}")
    @ApiOperation("Получение актуальных на дату записей версии по коду справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет версии")
    })
    Page<Map<String, Object>> search(@ApiParam("Код справочника") @PathParam("refBookCode") String refBookCode,
                                 @ApiParam("Дата получения данных") @PathParam("date") OffsetDateTime date,
                                 @BeanParam SearchDataCriteria criteria);


    @GET
    @Path("/refBook/{refBookCode}")
    @ApiOperation("Получение актуальных на текущую дату записей версии по коду справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет версии")
    })
    Page<Map<String, Object>> search(@ApiParam("Код справочника") @PathParam("refBookCode") String refBookCode,
                                 @BeanParam SearchDataCriteria criteria);

    @GET
    @ApiOperation("Получение строки по идентификатору")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/row/{rowId}")
    Map<String, Object> getRow(@ApiParam("Идентификатор строки")@PathParam("rowId") String rowId);
}
