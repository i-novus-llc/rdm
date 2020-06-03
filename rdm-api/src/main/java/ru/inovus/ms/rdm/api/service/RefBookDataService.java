package ru.inovus.ms.rdm.api.service;

import io.swagger.annotations.*;
import ru.inovus.ms.rdm.api.model.FileModel;
import ru.inovus.ms.rdm.api.model.draft.Draft;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/refBookData")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api("Методы загрузки справочников из файла")
public interface RefBookDataService {

    @POST
    @ApiOperation(value = "Создание нового справочника и черновика из файла", hidden = true)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Справочник и черновик созданы"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/createByFile")
    Draft create(@ApiParam("Файл") FileModel fileModel);
}
