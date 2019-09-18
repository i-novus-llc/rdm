package ru.inovus.ms.rdm.n2o.service.api;

import io.swagger.annotations.*;
import org.springframework.data.domain.Page;
import ru.inovus.ms.rdm.n2o.model.refdata.SearchDataCriteria;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.LocalDateTime;
import java.util.List;
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
    @ApiImplicitParams({
            @ApiImplicitParam(name = "filter.<код атрибута>", value = "Фильтр по атрибуту", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "commonFilter", value = "Полнотекстовый поиск по всем атрибутам", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "rowSystemIds", value = "Фильтр по системным идентификаторам строк", dataTypeClass = List.class, paramType = "query"),
            @ApiImplicitParam(name = "page", value = "Номер страницы", defaultValue = "0", dataType = "integer", paramType = "query"),
            @ApiImplicitParam(name = "size", value = "Кол-во записей в странице", defaultValue = "10", dataType = "integer", paramType = "query"),
            @ApiImplicitParam(name = "sort", value = "Сортировка, sort=<код атрибута>, <ask|desc>", dataType = "string", paramType = "query")
    })
    @Path("/{versionId}/data")
    Page<Map<String, Object>> search(@ApiParam("Идентификатор версии") @PathParam("versionId") Integer versionId,
                                     @ApiParam(hidden = true) @BeanParam SearchDataCriteria criteria);

    @GET
    @Path("/refBook/{refBookCode}/{date}")
    @ApiOperation("Получение актуальных на дату записей версии по коду справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет опубликованных данных")
    })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "filter.<код атрибута>", value = "Фильтр по атрибуту", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "commonFilter", value = "Полнотекстовый поиск по всем атрибутам", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "rowSystemIds", value = "Фильтр по системным идентификаторам строк", dataTypeClass = List.class, paramType = "query"),
            @ApiImplicitParam(name = "page", value = "Номер страницы", defaultValue = "0", dataType = "integer", paramType = "query"),
            @ApiImplicitParam(name = "size", value = "Кол-во записей в странице", defaultValue = "10", dataType = "integer", paramType = "query"),
            @ApiImplicitParam(name = "sort", value = "Сортировка, sort=<код атрибута>, <ask|desc>", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "sort", value = "Сортировка, sort=<код атрибута>, <ask|desc>", dataType = "string", paramType = "query")

    })
    Page<Map<String, Object>> search(@ApiParam("Код справочника") @PathParam("refBookCode") String refBookCode,
                                     @ApiParam("Дата(в UTC) получения данных в формате yyyy-MM-ddTHH:mm:ss") @PathParam("date") LocalDateTime date,
                                     @ApiParam(hidden = true) @BeanParam SearchDataCriteria criteria);

    @GET
    @Path("/refBook/{refBookCode}")
    @ApiOperation("Получение актуальных на текущую дату записей версии по коду справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет версии")
    })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "filter.<код атрибута>", value = "Фильтр по атрибуту", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "commonFilter", value = "Полнотекстовый поиск по всем атрибутам", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "rowSystemIds", value = "Фильтр по системным идентификаторам строк", dataTypeClass = List.class, paramType = "query"),
            @ApiImplicitParam(name = "page", value = "Номер страницы", defaultValue = "0", dataType = "integer", paramType = "query"),
            @ApiImplicitParam(name = "size", value = "Кол-во записей в странице", defaultValue = "10", dataType = "integer", paramType = "query"),
            @ApiImplicitParam(name = "sort", value = "Сортировка, sort=<код атрибута>, <ask|desc>", dataType = "string", paramType = "query")
    })
    Page<Map<String, Object>> search(@ApiParam("Код справочника") @PathParam("refBookCode") String refBookCode,
                                     @ApiParam(hidden = true) @BeanParam SearchDataCriteria criteria);

    @GET
    @ApiOperation("Получение записи по идентификатору")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/row/{rowId}")
    Map<String, Object> getRow(@ApiParam("Идентификатор записи") @PathParam("rowId") String rowId);
}
