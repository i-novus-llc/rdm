package ru.inovus.ms.rdm.sync.rest;

import ru.inovus.ms.rdm.api.model.refbook.RefBook;
import ru.inovus.ms.rdm.sync.criteria.LogCriteria;
import ru.inovus.ms.rdm.sync.model.Log;
import ru.inovus.ms.rdm.sync.model.VersionMapping;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author lgalimova
 * @since 20.02.2019
 */

/**
 * Интерфейс API для синхронизации данных справочников НСИ
 */
@Path("rdm")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface RdmSyncRest {

    @POST
    @Path("/update")
    void update();

    @POST
    @Path("/update/{refbookCode}")
    void update(@PathParam("refbookCode") String refbookCode);

    void update(RefBook refBook, VersionMapping versionMapping);

    @GET
    @Path("/log")
    List<Log> getLog(@BeanParam LogCriteria criteria);
}
