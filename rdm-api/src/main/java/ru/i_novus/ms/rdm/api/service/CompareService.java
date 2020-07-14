package ru.i_novus.ms.rdm.api.service;

import io.swagger.annotations.*;
import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.compare.ComparableRow;
import ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria;
import ru.i_novus.ms.rdm.api.model.diff.RefBookDataDiff;
import ru.i_novus.ms.rdm.api.model.diff.StructureDiff;
import ru.i_novus.ms.rdm.api.model.diff.PassportDiff;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/compare")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "Методы сравнения версий", hidden = true)
public interface CompareService {

    @GET
    @Path("/passports/{oldVersionId}-{newVersionId}")
    @ApiOperation("Сравнение метаданных (паспортов) версий")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    PassportDiff comparePassports(@ApiParam("Идентификатор старой версии") @PathParam("oldVersionId") Integer oldVersionId,
                                  @ApiParam("Идентификатор новой версии") @PathParam("newVersionId") Integer newVersionId);

    @GET
    @Path("/structures")
    @ApiOperation("Сравнение структур")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    StructureDiff compareStructures(@ApiParam("Старая структура") @QueryParam("oldStructure") Structure oldStructure,
                                    @ApiParam("Новая структура") @QueryParam("newStructure") Structure newStructure);

    @GET
    @Path("/structures/{oldVersionId}-{newVersionId}")
    @ApiOperation("Сравнение структур версий")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    StructureDiff compareStructures(@ApiParam("Идентификатор старой версии") @PathParam("oldVersionId") Integer oldVersionId,
                                    @ApiParam("Идентификатор новой версии") @PathParam("newVersionId") Integer newVersionId);

    @GET
    @Path("/data")
    @ApiOperation("Сравнение данных версий")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    RefBookDataDiff compareData(@ApiParam("Критерий сравнения") @BeanParam CompareDataCriteria compareDataCriteria);

    @GET
    @Path("/getCommonComparableRows")
    @ApiOperation("Объединенный результат сравнения данных версий")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    Page<ComparableRow> getCommonComparableRows(@ApiParam("Критерий сравнения") @BeanParam CompareDataCriteria criteria);

}