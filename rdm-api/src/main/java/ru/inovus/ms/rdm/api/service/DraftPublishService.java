package ru.inovus.ms.rdm.api.service;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import ru.inovus.ms.rdm.api.model.draft.PublishRequest;
import ru.inovus.ms.rdm.api.model.draft.PublishResponse;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/publish-draft")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "Методы публикации черновика", hidden = true)
public interface DraftPublishService {

    @POST
    @Path("/{draftId}")
    @ApiOperation("Публикация черновика справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Черновик опубликован"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    PublishResponse publish(PublishRequest request);
}
