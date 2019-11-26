package ru.inovus.ms.rdm.api.service;

import io.swagger.annotations.*;
import ru.inovus.ms.rdm.api.model.refbook.RefBookUpdateRequest;
import ru.inovus.ms.rdm.api.model.version.RefBookVersion;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/passport")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "Методы паспорта справочника", hidden = true)
public interface PassportService {

    @PUT
    @ApiOperation("Изменение метаданных версии")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    RefBookVersion updatePassport(RefBookUpdateRequest refBookUpdateRequest);

}
