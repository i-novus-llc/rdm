package ru.i_novus.ms.rdm.api.service.l10n;

import io.swagger.annotations.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.l10n.api.model.L10nVersionLocale;

import java.util.List;

@Path("/locale")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api("Методы локализации версии")
public interface VersionLocaleService {

    @GET
    @ApiOperation("Получение списка кодов локалей с переводами справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Список код локалей справочника"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/refBook/{refBookCode}")
    List<String> findRefBookLocales(@ApiParam("Код справочника") @PathParam("refBookCode") String refBookCode);

    @GET
    @ApiOperation(value = "Получение списка локалей версии", hidden = true)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Список локалей версии"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/version/{versionId}")
    Page<L10nVersionLocale> searchVersionLocales(@ApiParam("Идентификатор версии") @PathParam("versionId") Integer versionId);

    @GET
    @ApiOperation(value = "Получение локали версии по коду", hidden = true)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Локаль версии"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/version/{versionId}/{localeCode}")
    L10nVersionLocale getVersionLocale(@ApiParam("Идентификатор версии") @PathParam("versionId") Integer versionId,
                                       @ApiParam("Код локали") @PathParam("localeCode") String localeCode);

    @GET
    @ApiOperation("Получение наименования локали по коду")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Наименование локали"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/name/{localeCode}")
    String getLocaleName(@ApiParam("Код локали") @PathParam("localeCode") String localeCode);
}
