package ru.inovus.ms.rdm.sync.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * @author lgalimova
 * @since 20.02.2019
 */
@Path("rdm")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api("Синхронизация данных справочников НСИ")
public interface RdmSyncRest {

    @POST
    @Path("/update")
    @ApiOperation(value = "Синхронизация всех справочников")
    void update();

    @POST
    @Path("/update/{refbookCode}")
    @ApiOperation(value = "Синхронизация отдельного справочника")
    void update(@PathParam("refbookCode") String refbookCode);
}
