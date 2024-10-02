package ru.i_novus.ms.rdm.api.service;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.api.async.AsyncOperationLogEntry;
import ru.i_novus.ms.rdm.api.async.AsyncOperationLogEntryCriteria;
import ru.i_novus.ms.rdm.api.async.AsyncOperationStatusEnum;
import ru.i_novus.ms.rdm.api.async.AsyncOperationTypeEnum;

import java.util.UUID;

@Path("/asynclog")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "Журнал асинхронных операций", hidden = true)
public interface AsyncOperationLogEntryService {

    @GET
    @ApiOperation("Получение записей журнала операций")
    Page<AsyncOperationLogEntry> search(@BeanParam AsyncOperationLogEntryCriteria criteria);

    @GET
    @Path("/entry/{id}")
    @ApiOperation("Получение записи асинхронной операции по её идентификатору")
    AsyncOperationLogEntry get(@ApiParam("Идентификатор операции") @PathParam("id") UUID id);

    @GET
    @Path("/types")
    @ApiOperation("Получение возможных типов асинхронных операций")
    Page<AsyncOperationTypeEnum> getOperationTypes();

    @GET
    @Path("/statuses")
    @ApiOperation("Получение возможных статусов асинхронных операций")
    Page<AsyncOperationStatusEnum> getOperationStatuses();
}
