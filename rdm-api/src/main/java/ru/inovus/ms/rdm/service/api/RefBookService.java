package ru.inovus.ms.rdm.service.api;

import io.swagger.annotations.*;
import org.springframework.data.domain.Page;
import ru.inovus.ms.rdm.model.refbook.*;
import ru.inovus.ms.rdm.model.version.RefBookVersion;
import ru.inovus.ms.rdm.model.version.ReferrerVersionCriteria;
import ru.inovus.ms.rdm.model.version.VersionCriteria;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/refBook")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api("Методы работы со справочниками")
public interface RefBookService {

    @GET
    @ApiOperation(value = "Поиск справочников по параметрам критерия", hidden = true)
    @ApiImplicitParams(@ApiImplicitParam(name = "sort", value = "Параметры сортировки",
            required = false, allowMultiple = true, paramType = "query", dataType = "string"))
    @ApiResponses({
            @ApiResponse(code = 200, message = "Список справочников"),
            @ApiResponse(code = 400, message = "Некорректный запрос")
    })
    Page<RefBook> search(@ApiParam("Критерий поиска") @BeanParam RefBookCriteria criteria);

    @GET
    @Path("/version/{id}")
    @ApiOperation(value = "Поиск по идентификатору версии", hidden = true)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Справочник"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    RefBook getByVersionId(@ApiParam("Идентификатор версии") @PathParam("id") Integer versionId);

    @GET
    @Path("/{id}")
    @ApiOperation(value = "Код справочника по идентификатору", hidden = true)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Справочник"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    String getCode(@ApiParam("Идентификатор справочника") @PathParam("id") Integer refBookId);

    @GET
    @Path("/code/{refBookCode}")
    @ApiOperation(value = "Идентификатор справочника по коду", hidden = true)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Справочник"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    Integer getId(@ApiParam("Код справочника") @PathParam("refBookCode") String refBookCode);

    @POST
    @ApiOperation(value = "Создание нового справочника", hidden = true)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Справочник"),
            @ApiResponse(code = 400, message = "Некорректный запрос")
    })
    RefBook create(RefBookCreateRequest refBookCreateRequest);

    @PUT
    @ApiOperation(value = "Изменение метаданных справочника", hidden = true)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Справочник"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    RefBook update(RefBookUpdateRequest refBookUpdateRequest);

    @DELETE
    @ApiOperation(value = "Удаление справочника", hidden = true)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Справочник удален"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void delete(@ApiParam("Идентификатор справочника") @QueryParam("refBookId") int refBookId);

    @POST
    @Path("/{refBookId}/toArchive")
    @ApiOperation(value = "В архив", hidden = true)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Справочник в архиве"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void toArchive(@ApiParam("Идентификатор справочника") @PathParam("refBookId") int refBookId);

    @POST
    @Path("/{refBookId}/fromArchive")
    @ApiOperation(value = "Вернуть из архива", hidden = true)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Справочник возвращен из архива"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void fromArchive(@ApiParam("Идентификатор справочника") @PathParam("refBookId") int refBookId);

    @GET
    @Path("/versions")
    @ApiOperation("Получение списка версий справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Версия справочника"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    Page<RefBookVersion> getVersions(@BeanParam VersionCriteria criteria);

    @GET
    @Path("/referrers")
    @ApiOperation(value = "Поиск версий ссылающихся справочников по параметрам критерия", hidden = true)
    @ApiImplicitParams(@ApiImplicitParam(name = "sort", value = "Параметры сортировки",
            required = false, allowMultiple = true, paramType = "query", dataType = "string"))
    @ApiResponses({
            @ApiResponse(code = 200, message = "Список справочников"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    Page<RefBookVersion> searchReferrerVersions(@ApiParam("Критерий поиска") @BeanParam ReferrerVersionCriteria criteria);
}
