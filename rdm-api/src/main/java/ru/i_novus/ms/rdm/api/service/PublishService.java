package ru.i_novus.ms.rdm.api.service;

import io.swagger.annotations.*;
import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.model.draft.PublishResponse;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

@Path("/publish")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "Методы публикации справочника", hidden = true)
public interface PublishService {

    @POST
    @Path("/{draftId}")
    @ApiOperation("Публикация справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Результат публикации"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    PublishResponse publish(@ApiParam("Идентификатор публикуемого черновика") @PathParam("draftId") Integer draftId,
                            PublishRequest request);

    @POST
    @Path("/async/{draftId}")
    @ApiOperation("Запрос на публикацию справочника")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Запрос поставлен в очередь")
    })
    UUID publishAsync(@ApiParam("Идентификатор публикуемого черновика") @PathParam("draftId") Integer draftId,
                      PublishRequest request);
}
