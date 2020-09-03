package ru.i_novus.ms.rdm.l10n.api.service;

import io.swagger.annotations.*;
import ru.i_novus.ms.rdm.l10n.api.model.LocalizeDataRequest;
import ru.i_novus.ms.rdm.l10n.api.model.LocalizeTableRequest;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/l10n")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "Методы локализации версии", hidden = true)
public interface L10nVersionStorageService {

    @POST
    @ApiOperation("Создание копии таблицы версии для локализации записей")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Код локализованной таблицы"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/{versionId}/table")
    String localizeTable(@ApiParam("Идентификатор версии") @PathParam("versionId") Integer versionId,
                         @ApiParam("Модель локализации таблицы") LocalizeTableRequest request);

    @POST
    @ApiOperation("Локализация записей версии")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/{versionId}/data")
    void localizeData(@ApiParam("Идентификатор версии") @PathParam("versionId") Integer versionId,
                      @ApiParam("Модель локализации данных") LocalizeDataRequest request);

    @GET
    @ApiOperation("Получение кода хранилища с учётом локали")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Код хранилища с учётом локали"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/storage/code/{storageCode}/{localeCode}")
    String getLocaleStorageCode(String storageCode, String localeCode);
}
