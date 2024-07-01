package ru.i_novus.ms.rdm.api.service;

import io.swagger.annotations.*;
import org.apache.cxf.interceptor.OutInterceptors;
import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.model.draft.Draft;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCreateRequest;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCriteria;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookUpdateRequest;
import ru.i_novus.ms.rdm.api.model.refdata.RdmChangeDataRequest;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/refBook")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api("Методы работы со справочниками")
@OutInterceptors(interceptors = {"ru.i_novus.ms.rdm.config.UserInfoCxfInterceptor"})
public interface RefBookService {

    @GET
    @ApiOperation(value = "Поиск справочников по параметрам критерия")
    @ApiImplicitParams(@ApiImplicitParam(name = "sort", value = "Параметры сортировки",
            required = false, allowMultiple = true, paramType = "query", dataType = "string"))
    @ApiResponses({
            @ApiResponse(code = 200, message = "Список справочников"),
            @ApiResponse(code = 400, message = "Некорректный запрос")
    })
    Page<RefBook> search(@BeanParam RefBookCriteria criteria);

    @GET
    @Path("/versions")
    @ApiOperation(value = "Поиск версий справочников по параметрам критерия", hidden = true)
    @ApiImplicitParams(@ApiImplicitParam(name = "sort", value = "Параметры сортировки",
            required = false, allowMultiple = true, paramType = "query", dataType = "string"))
    @ApiResponses({
            @ApiResponse(code = 200, message = "Список версий справочников"),
            @ApiResponse(code = 400, message = "Некорректный запрос")
    })
    Page<RefBook> searchVersions(@BeanParam RefBookCriteria criteria);

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

    @POST
    @ApiOperation(value = "Создание нового справочника и черновика из файла", hidden = true)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Справочник и черновик созданы"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    @Path("/createFromFile")
    Draft create(@ApiParam("Файл") FileModel fileModel);

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

    @POST
    @Path("/changeData")
    @ApiOperation(value = "Добавить данные в черновик справочника и тут же опубликовать. Если черновика нет -- он будет создан, если есть -- данные отредактируются в существующем.", hidden = true)
    void changeData(RdmChangeDataRequest request);

}
