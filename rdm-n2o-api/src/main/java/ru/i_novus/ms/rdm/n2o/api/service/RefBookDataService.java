package ru.i_novus.ms.rdm.n2o.api.service;

import io.swagger.annotations.*;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.n2o.api.criteria.DataCriteria;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/refBookData")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "Методы работы с данными справочника", hidden = true)
public interface RefBookDataService {

    @GET
    @Path("/{versionId}/structure")
    @ApiOperation("Получение структуры данных версии справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Структура данных версии справочника"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    Structure getDataStructure(Integer versionId, DataCriteria criteria);
}
