package ru.inovus.ms.rdm.service.api;

import io.swagger.annotations.*;
import ru.inovus.ms.rdm.model.CompareDataCriteria;
import ru.inovus.ms.rdm.model.PassportDiff;
import ru.inovus.ms.rdm.model.RefBookDataDiff;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/compare")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api("Методы сравнения версий")
public interface CompareService {

    @GET
    @Path("/passports")
    @ApiOperation("Сравнение метаданных (паспортов) версий")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    PassportDiff comparePassports(@ApiParam("Идентификатор старой версии")
                                  @QueryParam("oldVersionId")
                                          Integer oldVersionId,
                                  @ApiParam("Идентификатор новой версии")
                                  @QueryParam("newVersionId")
                                          Integer newVersionId);

    @GET
    @Path("/data")
    @ApiOperation("Сравнение данных версий")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    RefBookDataDiff compareData(@BeanParam CompareDataCriteria compareDataCriteria);

}