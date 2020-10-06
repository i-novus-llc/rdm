package ru.i_novus.ms.rdm.api.service.l10n;

import io.swagger.annotations.*;
import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.l10n.api.model.L10nVersionLocale;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/locale")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "Методы локализации версии", hidden = true)
public interface VersionLocaleService {

    @GET
    @ApiOperation("Получение списка локалей версии")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Список локалей версии"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/{versionId}")
    Page<L10nVersionLocale> searchVersionLocales(@ApiParam("Идентификатор версии") @PathParam("versionId") Integer versionId);

    @GET
    @ApiOperation("Получение локали версии по коду")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Локаль версии"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/{versionId}/{localeCode}")
    L10nVersionLocale getVersionLocale(@ApiParam("Идентификатор версии") @PathParam("versionId") Integer versionId,
                                       @ApiParam("Код локали") @PathParam("localeCode") String localeCode);

    @GET
    @ApiOperation("Получение наименования локали версии по коду")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Локаль версии"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/name/{localeCode}")
    String getLocaleName(@ApiParam("Код локали") @PathParam("localeCode") String localeCode);
}
