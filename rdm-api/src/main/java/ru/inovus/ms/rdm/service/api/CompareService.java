package ru.inovus.ms.rdm.service.api;

import io.swagger.annotations.*;
import ru.inovus.ms.rdm.model.PassportDiff;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Path("/compare")
@Api("Методы сравнения версий")
public interface CompareService {

    @GET
    @Path("/passports")
    @ApiOperation("Сравнение метаданных (паспортов) версий")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    PassportDiff comparePassports(@ApiParam("Идентификатор первой версии")
                                  @QueryParam("firstVersionId")
                                          Integer firstVersionId,
                                  @ApiParam("Идентификатор второй версии")
                                  @QueryParam("secondVersionId")
                                          Integer secondVersionId);

}