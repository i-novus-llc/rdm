package ru.i_novus.ms.rdm.sync.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.sync.criteria.LogCriteria;
import ru.i_novus.ms.rdm.sync.model.Log;
import ru.i_novus.ms.rdm.sync.model.VersionMapping;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author lgalimova
 * @since 20.02.2019
 */

@Path("rdm")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Синхронизация данных справочников НСИ", hidden = true)
public interface RdmSyncRest {

    @POST
    @Path("/update")
    @ApiOperation(value = "Синхронизация всех справочников")
    void update();

    @POST
    @Path("/update/{refbookCode}")
    @ApiOperation(value = "Синхронизация отдельного справочника")
    void update(@PathParam("refbookCode") String refbookCode);

    void update(RefBook refBook, VersionMapping versionMapping);

    @GET
    @Path("/log")
    @ApiOperation(value = "Получение журнала за дату")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 400, message = "Укажите пожалуйста дату в формате ISO_LOCAL_DATE [yyyy-MM-dd].")
    })
    List<Log> getLog(@BeanParam LogCriteria criteria);

    @GET
    @Path("/xml-fm")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    Response downloadXmlFieldMapping(@QueryParam("code") List<String> forRefBooks);

    RefBook getLastPublishedVersionFromRdm(String code);

}
