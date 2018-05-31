package ru.inovus.ms.rdm.service;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import ru.inovus.ms.rdm.model.Echo;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * Created by tnurdinov on 30.05.2018.
 */
@Path("/echo")
@Produces("application/json")
@Consumes("application/json")
@Api("Эхо")
public interface EchoService {

    @GET
    @ApiOperation("Получения эхо")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    Echo getEcho();
}
