package ru.inovus.ms.rdm.service.api;

import io.swagger.annotations.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.LocalDateTime;

@Path("/publish")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api("Методы публикации")
public interface PublishService {

    @POST
    @Path("/{draftId}")
    @ApiOperation("Публикация черновика")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Черновик опубликован"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void publish(@ApiParam("Идентификатор черновика") @PathParam("draftId") Integer draftId,
                 @ApiParam("Версия") @QueryParam("version") String version,
                 @ApiParam("Дата начала действия версии") @QueryParam("fromDate") LocalDateTime fromDate,
                 @ApiParam("Дата окончания действия версии") @QueryParam("toDate") LocalDateTime toDate);
}
