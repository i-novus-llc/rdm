package ru.inovus.ms.rdm.service;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author lgalimova
 * @since 20.02.2019
 */
@Path("rdm")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api("Синхронизация справочников")
public interface RdmSyncRest {

    @POST
    @Path("/update")
    @ApiOperation(value = "Синхронизация справочников")
    Response update();
}
