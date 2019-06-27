package ru.inovus.ms.rdm.service.api;

import io.swagger.annotations.*;
import org.springframework.data.domain.Page;
import ru.inovus.ms.rdm.model.refdata.SearchDataCriteria;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.LocalDateTime;
import java.util.Map;

@Path("/plainData/version")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api("Методы получения данных версий справочника")
public interface VersionPlainDataService {

    @GET
    @ApiOperation("Получение записей версии по параметрам критерия")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет версии")
    })
    @Path("/{versionId}/data")
    Page<Map<String, Object>> search(@PathParam("versionId")
                                             Integer versionId,
                                     @BeanParam
                                             SearchDataCriteria criteria);

    @GET
    @Path("/refBook/{refBookCode}/{date}")
    @ApiOperation("Получение актуальных на дату записей версии по коду справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет версии")
    })
    Page<Map<String, Object>> search(@ApiParam("Код справочника")
                                     @PathParam("refBookCode")
                                             String refBookCode,
                                     @ApiParam("Дата получения данных")
                                     @PathParam("date")
                                             LocalDateTime date,
                                     @BeanParam
                                             SearchDataCriteria criteria);

    @GET
    @Path("/refBook/{refBookCode}")
    @ApiOperation("Получение актуальных на текущую дату записей версии по коду справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет версии")
    })
    Page<Map<String, Object>> search(@ApiParam("Код справочника")
                                     @PathParam("refBookCode")
                                             String refBookCode,
                                     @BeanParam
                                             SearchDataCriteria criteria);

    @GET
    @ApiOperation("Получение записи по идентификатору")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/row/{rowId}")
    Map<String, Object> getRow(@ApiParam("Идентификатор записи")
                               @PathParam("rowId")
                                       String rowId);
}
