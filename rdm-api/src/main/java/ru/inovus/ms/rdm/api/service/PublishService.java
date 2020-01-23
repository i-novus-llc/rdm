package ru.inovus.ms.rdm.api.service;

import io.swagger.annotations.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.LocalDateTime;
import java.util.UUID;

@Path("/publish")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "Методы публикации", hidden = true)
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
                 @ApiParam("Дата окончания действия версии") @QueryParam("toDate") LocalDateTime toDate,
                 @ApiParam("Обработка разрешаемых конфликтов") @DefaultValue("false") @QueryParam("resolveConflicts") boolean resolveConflicts);

    @POST
    @Path("/async/{draftId}")
    @ApiOperation("Запрос на публикацию справочника")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Запрос поставлен в очередь")
    })
    UUID publishAsync(@ApiParam("Идентификатор черновика") @PathParam("draftId") Integer draftId,
                      @ApiParam("Версия") @QueryParam("version") String version,
                      @ApiParam("Дата начала действия версии") @QueryParam("fromDate") LocalDateTime fromDate,
                      @ApiParam("Дата окончания действия версии") @QueryParam("toDate") LocalDateTime toDate,
                      @ApiParam("Обработка разрешаемых конфликтов") @DefaultValue("false") @QueryParam("resolveConflicts") boolean resolveConflicts);
}
