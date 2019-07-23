package ru.inovus.ms.rdm.service.api;

import io.swagger.annotations.*;
import org.springframework.data.domain.Page;
import ru.inovus.ms.rdm.enumeration.FileType;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.model.refdata.RefBookRowValue;
import ru.inovus.ms.rdm.model.refdata.SearchDataCriteria;
import ru.inovus.ms.rdm.model.version.RefBookVersion;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.LocalDateTime;
import java.util.List;

@Path("/version")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api("Методы работы с версиями справочника")
public interface VersionService {

    @GET
    @ApiOperation("Получение записей версии по параметрам критерия")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет версии")
    })
    @Path("/{versionId}/data")
    Page<RefBookRowValue> search(@ApiParam("Идентификатор версии") @PathParam("versionId") Integer versionId,
                                 @ApiParam("Критерий поиска") @BeanParam SearchDataCriteria criteria);

    @GET
    @ApiOperation("Получение версии по идентификатору")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет версии")
    })
    @Path("/{versionId}")
    RefBookVersion getById(@ApiParam("Идентификатор версии") @PathParam("versionId") Integer versionId);

    @GET
    @ApiOperation("Получение версии по коду справочника и номеру")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет версии")
    })
    @Path("/{version}/refbook/{refBookCode}")
    RefBookVersion getVersion(@ApiParam("Номер версии") @PathParam("version") String version,
                              @ApiParam("Код справочника") @PathParam("refBookCode") String refBookCode);

    @GET
    @ApiOperation("Получение последней опубликованной версии по коду справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет версии")
    })
    @Path("/refBook/{refBookCode}/last")
    RefBookVersion getLastPublishedVersion(@ApiParam("Код справочника") @PathParam("refBookCode") String refBookCode);

    @GET
    @Path("/refBook/{refBookCode}/{date}")
    @ApiOperation("Получение актуальных на дату записей версии по коду справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет версии")
    })
    Page<RefBookRowValue> search(@ApiParam("Код справочника") @PathParam("refBookCode") String refBookCode,
                                 @ApiParam("Дата получения данных") @PathParam("date") LocalDateTime date,
                                 @ApiParam("Критерий поиска") @BeanParam SearchDataCriteria criteria);

    @GET
    @Path("/refBook/{refBookCode}")
    @ApiOperation("Получение актуальных на текущую дату записей версии по коду справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет версии")
    })
    Page<RefBookRowValue> search(@ApiParam("Код справочника") @PathParam("refBookCode") String refBookCode,
                                 @ApiParam("Критерий поиска") @BeanParam SearchDataCriteria criteria);

    @GET
    @Path("/structure")
    @ApiOperation("Получение структуры версии справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Структура версии справочника"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    Structure getStructure(@ApiParam("Идентификатор версии") @QueryParam("versionId") Integer versionId);

    @GET
    @Path("/{versionId}/getFile")
    @Produces("application/zip")
    @ApiOperation("Выгрузка версии")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    ExportFile getVersionFile(@ApiParam("Идентификатор версии") @PathParam("versionId") Integer versionId,
                              @ApiParam(value = "Тип файла", required = true, allowableValues = "XLSX, XML") @QueryParam("type") FileType fileType);

    @GET
    @ApiOperation("Информация о существовании записей в системе")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/row/exists")
    ExistsData existsData(@ApiParam("Идентификаторы записи") @QueryParam("rowId") List<String> rowIds);

    @GET
    @ApiOperation("Получение записи по идентификатору")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/row/{rowId}")
    RefBookRowValue getRow(@ApiParam("Идентификатор записи") @PathParam("rowId") String rowId);
}
