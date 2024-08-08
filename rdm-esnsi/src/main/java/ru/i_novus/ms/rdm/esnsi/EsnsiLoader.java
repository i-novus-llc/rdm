package ru.i_novus.ms.rdm.esnsi;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("esnsi")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api("Синхронизация с ЕСНСИ")
public interface EsnsiLoader {

    @POST
    @Path("/update")
    @ApiOperation(value = "Синхронизация всех справочников")
    void update();

    @POST
    @Path("/update/{classifierCode}")
    @ApiOperation(value = "Синхронизация отдельного справочника")
    String update(String classifierCode);

    @POST
    @Path("/shutdown")
    @ApiOperation(value = "Остановить синхронизацию всех справочников")
    void shutdown();

    @POST
    @Path("/shutdown/{classifierCode}")
    @ApiOperation(value = "Остановить синхронизацию отдельного справочника")
    void shutdown(String classifierCode);

    @POST
    @Path("/cleanHistory")
    @ApiOperation(value = "Очистить историю синхронизации со всеми справочниками")
    void cleanHistory();

    @POST
    @Path("/cleanHistory/{classifierCode}")
    @ApiOperation(value = "Очистить историю синхронизации с конкретным справочником")
    void cleanHistory(String classifierCode);


}
