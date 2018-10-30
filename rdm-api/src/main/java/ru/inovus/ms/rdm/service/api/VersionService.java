package ru.inovus.ms.rdm.service.api;

import io.swagger.annotations.*;
import org.springframework.data.domain.Page;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.inovus.ms.rdm.enumeration.FileType;
import ru.inovus.ms.rdm.model.*;

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
    @Path("/{versionId}/data")
    Page<RowValue> search(@PathParam("versionId")Integer versionId, @BeanParam SearchDataCriteria criteria);

    @GET
    @ApiOperation("Получение версии по идентификатору")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет версии")
    })
    @Path("/{versionId}")
    RefBookVersion getById(@ApiParam("Идентификатор версии")
                           @PathParam("versionId")
                           Integer versionId);

    @GET
    @Path("/refBook/{refBookCode}/{date}")
    @ApiOperation("Получение актуальных на дату записей версии по коду справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет версии")
    })
    Page<RowValue> search(@ApiParam("Код справочника") @PathParam("refBookCode") String refBookCode,
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

    @GET
    @Path("/{versionId}/getFile")
    @Produces("application/zip")
    @ApiOperation("Выгрузка версии")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    ExportFile getVersionFile(@ApiParam("Идентификатор версии")
                            @PathParam("versionId")
                            Integer versionId,
                            @ApiParam(value = "Тип файла", required = true)
                            @QueryParam("type")
                            FileType fileType);

}
