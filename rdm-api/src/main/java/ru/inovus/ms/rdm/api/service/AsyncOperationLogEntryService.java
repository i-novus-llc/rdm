package ru.inovus.ms.rdm.api.service;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.data.domain.Page;
import ru.inovus.ms.rdm.api.async.AsyncOperation;
import ru.inovus.ms.rdm.api.async.AsyncOperationLogEntry;
import ru.inovus.ms.rdm.api.async.AsyncOperationLogEntryCriteria;
import ru.inovus.ms.rdm.api.async.AsyncOperationStatus;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
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
    @Path("/entry/{logEntryId}")
    @ApiOperation("Получение текущего статуса асинхронной операции по ее идентификатору")
    AsyncOperationLogEntry get(@ApiParam("Идентификатор операции") @PathParam("logEntryId") UUID uuid);

    @GET
    @Path("/opTypes")
    @ApiOperation("Получение возможных типов асинхронных операций")
    Page<AsyncOperation> getOpTypes();

    @GET
    @Path("/statuses")
    @ApiOperation("Получение типов реализованных асинхронных операций")
    Page<AsyncOperationStatus> getStatuses();

}
