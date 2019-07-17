package ru.inovus.ms.rdm.service.api;

import io.swagger.annotations.*;
import org.springframework.data.domain.Page;
import ru.inovus.ms.rdm.enumeration.ConflictType;
import ru.inovus.ms.rdm.model.conflict.*;
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
    @ApiOperation("Вычисление конфликтов по параметрам критерия")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    List<Conflict> calculateConflicts(@ApiParam("Критерий вычисления") @BeanParam CalculateConflictCriteria criteria);


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

    @GET
    @Path("/rows/count")
    @ApiOperation("Получение количества конфликтных строк по параметрам критерия")
    @ApiImplicitParams(@ApiImplicitParam(name = "sort", value = "Параметры сортировки", required = false, allowMultiple = true,
            paramType = "query", dataType = "string"))
    @ApiResponses({
            @ApiResponse(code = 200, message = "Количество конфликтов"),
            @ApiResponse(code = 400, message = "Некорректный запрос")
    })
    Long countConflictedRowIds(@ApiParam("Критерий поиска") @BeanParam RefBookConflictCriteria criteria);

    @GET
    @Path("/rows")
    @ApiOperation("Поиск идентификаторов строк с конфликтами по параметрам критерия")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Список идентификаторов конфликтующих строк"),
            @ApiResponse(code = 400, message = "Некорректный запрос")
    })
    Page<Long> searchConflictedRowIds(@ApiParam("Критерий поиска") @BeanParam RefBookConflictCriteria criteria);

    @POST
    @Path("/create")
    @ApiOperation("Сохранение информации о конфликтах")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void create(CreateConflictsRequest request);

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
    @Path("/id/{refFromId}-{refToId}-{refFieldCode}")
    @ApiOperation("Поиск конфликта по основным параметрам")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    Integer findId(@ApiParam("Идентификатор версии, которая ссылается") @PathParam("refFromId") Integer refFromId,
                   @ApiParam("Идентификатор версии с конфликтами, на которую ссылаются") @PathParam("refToId") Integer refToId,
                   @ApiParam("Атрибут версии, которая ссылается") @PathParam("refFieldCode") String refFieldCode,
                   @ApiParam("Строка-конфликт версии, которая ссылается") @QueryParam("rowSystemId") Long rowSystemId);

    @GET
    @Path("/find/all/{versionId}")
    @ApiOperation("Получение конфликтных идентификаторов из идентификаторов записей для версии, которая ссылается")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    List<Long> getReferrerConflictedIds(@ApiParam("Идентификатор версии, которая ссылается") @PathParam("versionId") Integer referrerVersionId,
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
    @Path("/refresh/byPrimary/{refFieldCode}")
    @ApiOperation("Обновление ссылок в связанных справочниках")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void refreshLastReferrersByPrimary(@ApiParam("Код справочника, на который ссылаются") @PathParam("refFieldCode") String refBookCode);

    @POST
    @Path("/discover/{oldVersionId}-{newVersionId}")
    @ApiOperation("Обнаружение конфликтов")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void discoverConflicts(@ApiParam("Идентификатор старой версии, на которую ссылаются") @PathParam("oldVersionId") Integer oldVersionId,
                           @ApiParam("Идентификатор новой версии, на которую будут ссылаться") @PathParam("newVersionId") Integer newVersionId);

    @POST
    @Path("/copy/{oldVersionId}-{newVersionId}")
    @ApiOperation("Копирование конфликтов")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void copyConflicts(@ApiParam("Идентификатор старой версии, на которую ссылаются") @PathParam("oldVersionId") Integer oldVersionId,
                       @ApiParam("Идентификатор новой версии, на которую будут ссылаться") @PathParam("newVersionId") Integer newVersionId);
}
