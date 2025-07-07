package ru.i_novus.ms.rdm.api.rest;

import io.swagger.annotations.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;

@Path("/publish")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "Методы публикации справочника", hidden = true)
public interface PublishRestService {

    @POST
    @Path("/{draftId}")
    @ApiOperation("Публикация справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Результат публикации"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void publish(@ApiParam("Идентификатор публикуемого черновика") @PathParam("draftId") Integer draftId,
                 PublishRequest request);

    @POST
    @Path("/async/{draftId}")
    @ApiOperation("Запрос на публикацию справочника")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Запрос поставлен в очередь")
    })
    void publishAsync(@ApiParam("Идентификатор публикуемого черновика") @PathParam("draftId") Integer draftId,
                      PublishRequest request);
}
