package ru.inovus.ms.rdm.service.api;

import io.swagger.annotations.*;
import org.springframework.data.domain.Page;
import ru.inovus.ms.rdm.model.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/refBook")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api("Методы работы со справочниками")
public interface RefBookService {

    @GET
    @ApiOperation("Поиск справочников по критериям")
    @ApiImplicitParams(@ApiImplicitParam(name = "sort", value = "Параметры сортировки", required = false, allowMultiple = true,
            paramType = "query", dataType = "string"))

    @ApiResponses({
            @ApiResponse(code = 200, message = "Список справочников"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    Page<RefBook> search(@BeanParam RefBookCriteria criteria);

    @GET
    @Path("/version/{id}")
    @ApiOperation("Поиск по идентификатору версии")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Справочник"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    RefBook getByVersionId(@PathParam("id") @ApiParam("Идентификатор версии") Integer versionId);

    @POST
    @ApiOperation("Создание нового справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Справочник"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    RefBook create(RefBookCreateRequest refBookCreateRequest);

    @PUT
    @ApiOperation("Изменение метаданных справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Справочник"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    RefBook update(RefBookUpdateRequest refBookCreateRequest);

    @DELETE
    @ApiOperation("Удаление справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Справочник удален"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void delete(@QueryParam("refBookId") int refBookId);

    @POST
    @Path("/{refBookId}/toArchive")
    @ApiOperation("В архив")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Справочник в архиве"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void toArchive(@ApiParam("Идентификатор справочника") @PathParam("refBookId") int refBookId);

    @POST
    @Path("/{refBookId}/fromArchive")
    @ApiOperation("Вернуть из архива")
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
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    Page<RefBookVersion> getVersions(@BeanParam VersionCriteria criteria);
}
