package ru.i_novus.ms.rdm.l10n.api.service;

import io.swagger.annotations.*;
import ru.i_novus.ms.rdm.l10n.api.model.LocalizeDataRequest;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/l10n")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "Методы локализации версии", hidden = true)
public interface L10nVersionService {

    @POST
    @ApiOperation("Локализация записей версии")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Версия обновлена"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/{versionId}/data")
    void localizeData(Integer versionId, LocalizeDataRequest request);
}
