package ru.inovus.ms.rdm.sync.rest;

import org.springframework.data.domain.Page;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.Map;

@Path("rdm/data")
@Produces(MediaType.APPLICATION_JSON)
public interface LocalRdmDataService {

    @GET
    @Path("/{refBookCode}")
    @SuppressWarnings("squid:S1452")
    Page<Map<String, Object>> getData(
            @PathParam("refBookCode") String refBookCode,
            @QueryParam("getDeleted") Boolean getDeleted,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size,
            @Context UriInfo uriInfo);

    @GET
    @Path("/{refBookCode}/{primaryKey}")
    Map<String, Object> getSingle(@PathParam("refBookCode") String refBookCode, @PathParam("primaryKey") String pk);

}
