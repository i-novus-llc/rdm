package ru.inovus.ms.rdm.api.service;

import io.swagger.annotations.*;
import ru.inovus.ms.rdm.api.async.AsyncOperationLogEntry;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

@Path("/asynclog")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "Журнал асинхронных операций", hidden = true)
public interface AsyncOperationLogEntryService {

    @GET
    @Path("/entry/{logEntryId}")
    @ApiOperation("Получение текущего статуса асинхронной операции по ее идентификатору")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    AsyncOperationLogEntry get(@ApiParam("Идентификатор операции") @PathParam("logEntryId") UUID uuid);

}
