package ru.inovus.ms.rdm.api.service;

import io.swagger.annotations.*;
import ru.inovus.ms.rdm.api.model.draft.PublishRequest;

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
            @ApiResponse(code = 200, message = "Справочник опубликован"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void publish(PublishRequest request);

    @POST
    @Path("/async/{draftId}")
    @ApiOperation("Запрос на публикацию справочника")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Запрос поставлен в очередь")
    })
    UUID publishAsync(PublishRequest request);
}
