package ru.inovus.ms.rdm.api.service;

import io.swagger.annotations.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/reference")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "Методы работы с ссылками", hidden = true)
public interface ReferenceService {

    @POST
    @Path("/{versionId}/refresh")
    @ApiOperation("Обновление ссылок в справочнике")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void refreshReferrer(@ApiParam("Идентификатор версии") @PathParam("versionId") Integer referrerVersionId);

    @POST
    @Path("/refresh/{refBookCode}")
    @ApiOperation("Обновление ссылок в связанных справочниках")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void refreshLastReferrers(@ApiParam("Код справочника, на который ссылаются") @PathParam("refBookCode") String refBookCode);
}
