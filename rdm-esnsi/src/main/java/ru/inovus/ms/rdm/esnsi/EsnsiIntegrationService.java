package ru.inovus.ms.rdm.esnsi;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("esnsi")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api("Синхронизация с ЕСНСИ")
public interface EsnsiIntegrationService {

    @POST
    @Path("/update")
    @ApiOperation(value = "Синхронизация всех справочников")
    void update();

    @POST
    @Path("/update/{classifierCode}")
    @ApiOperation(value = "Синхронизация отдельного справочника")
    void update(String classifierCode);

    @POST
    @Path("/shutdown")
    @ApiOperation(value = "Остановить синхронизацию всех справочников")
    void shutdown();

    @POST
    @Path("/shutdown/{classifierCode}")
    @ApiOperation(value = "Остановить синхронизацию отдельного справочника")
    void shutdown(String classifierCode);

}