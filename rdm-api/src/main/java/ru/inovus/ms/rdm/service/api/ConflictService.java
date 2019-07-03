package ru.inovus.ms.rdm.service.api;

import io.swagger.annotations.*;
import org.springframework.data.domain.Page;
import ru.inovus.ms.rdm.enumeration.ConflictType;
import ru.inovus.ms.rdm.model.conflict.Conflict;
import ru.inovus.ms.rdm.model.conflict.DeleteRefBookConflictCriteria;
import ru.inovus.ms.rdm.model.conflict.RefBookConflict;
import ru.inovus.ms.rdm.model.conflict.RefBookConflictCriteria;
import ru.inovus.ms.rdm.model.version.RefBookVersion;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/conflicts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api("Методы работы с конфликтами")
public interface ConflictService {

    @GET
    @Path("/calculate")
    @ApiOperation("Вычисление конфликтов для двух версий")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    List<Conflict> calculateConflicts(@ApiParam("Идентификатор версии, которая ссылается") @QueryParam("refFromId") Integer refFromId,
                                      @ApiParam("Идентификатор старой версии, на которую ссылаются") @QueryParam("oldRefToId") Integer oldRefToId,
                                      @ApiParam("Идентификатор новой версии, на которую будут ссылаться") @QueryParam("newRefToId") Integer newRefToId);

    @GET
    @Path("/check")
    @ApiOperation("Проверка на наличие конфликта для двух версий")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    Boolean checkConflicts(@ApiParam("Идентификатор версии, которая ссылается") @QueryParam("refFromId") Integer refFromId,
                           @ApiParam("Идентификатор старой версии, на которую ссылаются") @QueryParam("oldRefToId") Integer oldRefToId,
                           @ApiParam("Идентификатор новой версии, на которую будут ссылаться") @QueryParam("newRefToId") Integer newRefToId,
                           @ApiParam("Тип конфликта") @QueryParam("type") ConflictType conflictType);

    @GET
    @Path("/check/{versionId}/referrer")
    @ApiOperation("Получение конфликтующих версий справочников для неопубликованной версии")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    List<RefBookVersion> getCheckConflictReferrers(@ApiParam("Идентификатор проверяемой версии") @PathParam("versionId") Integer versionId,
                                                   @ApiParam("Тип конфликта") @QueryParam("type") ConflictType conflictType);

    @GET
    @ApiOperation("Поиск конфликтов по параметрам критерия")
    @ApiImplicitParams(@ApiImplicitParam(name = "sort", value = "Параметры сортировки", required = false, allowMultiple = true,
            paramType = "query", dataType = "string"))
    @ApiResponses({
            @ApiResponse(code = 200, message = "Список конфликтов"),
            @ApiResponse(code = 400, message = "Некорректный запрос")
    })
    Page<RefBookConflict> search(@ApiParam("Критерий поиска") @BeanParam RefBookConflictCriteria criteria);

    @POST
    @Path("/create/list")
    @ApiOperation("Сохранение информации о конфликтах")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void create(@ApiParam("Идентификатор версии, которая ссылается") @QueryParam("refFromId") Integer refFromId,
                @ApiParam("Идентификатор версии с конфликтами, на которую ссылаются") @QueryParam("refToId") Integer refToId,
                @ApiParam("Список конфликтов") List<Conflict> conflicts);

    @DELETE
    @Path("/{id}")
    @ApiOperation("Удаление записи о конфликте")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void delete(@ApiParam("Идентификатор записи о конфликте") @PathParam("id") Integer id);

    @DELETE
    @ApiOperation("Удаление конфликтов по параметрам критерия")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void delete(@ApiParam("Критерий удаления") @BeanParam DeleteRefBookConflictCriteria criteria);

    @GET
    @Path("/find")
    @ApiOperation("Поиск конфликта по основным параметрам")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    RefBookConflict find(@ApiParam("Идентификатор версии, которая ссылается") @QueryParam("refFromId") Integer refFromId,
                         @ApiParam("Идентификатор версии с конфликтами, на которую ссылаются") @QueryParam("refToId") Integer refToId,
                         @ApiParam("Строка-конфликт версии, которая ссылается") @QueryParam("rowSystemId") Long rowSystemId,
                         @ApiParam("Атрибут версии, которая ссылается") @QueryParam("refFieldCode") String refFieldCode);

    @GET
    @Path("/find/id")
    @ApiOperation("Поиск конфликта по основным параметрам")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    Integer findId(@ApiParam("Идентификатор версии, которая ссылается") @QueryParam("refFromId") Integer refFromId,
                   @ApiParam("Идентификатор версии с конфликтами, на которую ссылаются") @QueryParam("refToId") Integer refToId,
                   @ApiParam("Строка-конфликт версии, которая ссылается") @QueryParam("rowSystemId") Long rowSystemId,
                   @ApiParam("Атрибут версии, которая ссылается") @QueryParam("refFieldCode") String refFieldCode);

    @GET
    @Path("/find/all")
    @ApiOperation("Получение конфликтных идентификаторов из идентификаторов записей для версии, которая ссылается")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    List<Long> getReferrerConflictedIds(@ApiParam("Идентификатор версии, которая ссылается") @QueryParam("versionId") Integer referrerVersionId,
                                        @ApiParam("Список системных идентификаторов записей") @QueryParam("refRecordIds") List<Long> refRecordIds);

    @POST
    @Path("/{versionId}/refresh/byPrimary")
    @ApiOperation("Обновление ссылок в справочнике")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void refreshReferrerByPrimary(@ApiParam("Идентификатор версии") @PathParam("versionId") Integer referrerVersionId);

    @POST
    @Path("/refresh/byPrimary")
    @ApiOperation("Обновление ссылок в связанных справочниках")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void refreshLastReferrersByPrimary(@ApiParam("Код справочника, на который ссылаются") @QueryParam("refFieldCode") String refBookCode);

    @POST
    @Path("/discover")
    @ApiOperation("Обнаружение конфликтов")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void discoverConflicts(@ApiParam("Идентификатор старой версии, на которую ссылаются") @QueryParam("oldVersionId") Integer oldVersionId,
                           @ApiParam("Идентификатор новой версии, на которую будут ссылаться") @QueryParam("newVersionId") Integer newVersionId);

    @POST
    @Path("/copy")
    @ApiOperation("Копирование конфликтов")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void copyConflicts(@ApiParam("Идентификатор старой версии, на которую ссылаются") @QueryParam("oldVersionId") Integer oldVersionId,
                       @ApiParam("Идентификатор новой версии, на которую будут ссылаться") @QueryParam("newVersionId") Integer newVersionId);
}
