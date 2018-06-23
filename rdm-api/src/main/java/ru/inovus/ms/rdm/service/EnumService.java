package ru.inovus.ms.rdm.service;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.data.domain.Page;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/enum")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api("Перечисления")
public interface EnumService {

    @GET
    @ApiOperation("Поиск значений перечисления")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Список значений перечисления"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    Page<Identifiable> search(@QueryParam("id") Integer id,
                              @QueryParam("enumClass") String enumClass,
                              @QueryParam("name") String name) throws ClassNotFoundException;

}
