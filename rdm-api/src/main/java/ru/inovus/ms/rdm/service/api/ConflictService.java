package ru.inovus.ms.rdm.service.api;

import io.swagger.annotations.*;
import ru.inovus.ms.rdm.enumeration.ConflictType;
import ru.inovus.ms.rdm.model.Conflict;
import ru.inovus.ms.rdm.model.RefBookConflict;
import ru.inovus.ms.rdm.model.RefBookVersion;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/conflicts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api("Методы работы с конфликтами")
public interface ConflictService {

    @GET
    @ApiOperation("Получение конфликтов для версии")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    List<RefBookConflict> getConflicts(@ApiParam("Идентификатор версии, которая ссылается")
                                       @QueryParam("versionId")
                                               Integer versionId,
                                       @ApiParam("Список системных идентификаторов строк")
                                       @QueryParam("refRecordIds")
                                               List<Long> refRecordIds);

    @GET
    @Path("/calculate")
    @ApiOperation("Вычисление конфликтов для двух версий")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    List<Conflict> calculateConflicts(@ApiParam("Идентификатор версии, которая ссылается")
                                      @QueryParam("refFromId")
                                              Integer refFromId,
                                      @ApiParam("Идентификатор бесконфликтной версии, на которую ссылается")
                                      @QueryParam("refToId")
                                              Integer refToId);

    @GET
    @Path("/check")
    @ApiOperation("Проверка на наличие конфликта для двух версий")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    Boolean checkConflicts(@ApiParam("Идентификатор версии, которая ссылается")
                           @QueryParam("refFromId")
                                   Integer refFromId,
                           @ApiParam("Идентификатор бесконфликтной версии, на которую ссылается")
                           @QueryParam("refToId")
                                   Integer refToId,
                           @ApiParam("Тип конфликта")
                           @QueryParam("type")
                                   ConflictType conflictType);

    @GET
    @Path("/check/{versionId}/referrer")
    @ApiOperation("Получение конфликтующих версий справочников для неопубликованной версии")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    List<RefBookVersion> getCheckConflictReferrers(@ApiParam("Идентификатор проверяемой версии")
                                                   @PathParam("versionId")
                                                           Integer versionId,
                                                   @ApiParam("Тип конфликта")
                                                   @QueryParam("type")
                                                           ConflictType conflictType);

    @POST
    @Path("/create")
    @ApiOperation("Сохранение информации о конфликтах")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void create(@ApiParam("Идентификатор версии, которая ссылается")
                @QueryParam("refFromId")
                        Integer refFromId,
                @ApiParam("Идентификатор версии с конфликтами, на которую ссылаются")
                @QueryParam("refToId")
                        Integer refToId,
                @ApiParam("Список конфликтов")
                        List<Conflict> conflicts);

    @GET
    @Path("/find")
    @ApiOperation("Поиск конфликта по основным параметрам")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    Conflict find(@ApiParam("Идентификатор версии, которая ссылается")
                  @QueryParam("refFromId")
                          Integer refFromId,
                  @ApiParam("Идентификатор версии с конфликтами, на которую ссылаются")
                  @QueryParam("refToId")
                          Integer refToId,
                  @ApiParam("Строка-конфликт версии, которая ссылается")
                  @QueryParam("rowSystemId")
                          Long rowSystemId,
                  @ApiParam("Атрибут версии, которая ссылается")
                  @QueryParam("refFieldCode")
                          String refFieldCode);

    @GET
    @Path("/findId")
    @ApiOperation("Поиск конфликта по основным параметрам")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    Integer findId(@ApiParam("Идентификатор версии, которая ссылается")
                   @QueryParam("refFromId")
                           Integer refFromId,
                   @ApiParam("Идентификатор версии с конфликтами, на которую ссылаются")
                   @QueryParam("refToId")
                           Integer refToId,
                   @ApiParam("Строка-конфликт версии, которая ссылается")
                   @QueryParam("rowSystemId")
                           Long rowSystemId,
                   @ApiParam("Атрибут версии, которая ссылается")
                   @QueryParam("refFieldCode")
                           String refFieldCode);

    @POST
    @Path("/delete/{id}")
    @ApiOperation("Удаление записи о конфликте")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void delete(@ApiParam("Идентификатор записи о конфликте")
                @PathParam("id")
                        Integer id);

    @GET
    @Path("/{versionId}/hasConflict")
    @ApiOperation("Проверка версии на наличие конфликта с любым справочником")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    boolean hasConflict(@ApiParam("Идентификатор проверяемой версии")
                        @PathParam("versionId")
                                Integer referrerVersionId);

    @GET
    @Path("/{versionId}/hasConflict/typed")
    @ApiOperation("Проверка версии на наличие конфликта обновления записи с любым справочником")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    boolean hasTypedConflict(@ApiParam("Идентификатор проверяемой версии")
                             @PathParam("versionId")
                                     Integer referrerVersionId,
                             @ApiParam("Тип конфликта")
                             @QueryParam("type")
                                     ConflictType conflictType);

    @GET
    @Path("/{versionId}/isConflicted")
    @ApiOperation("Проверка версии на наличие конфликта в любом справочнике")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    boolean isConflicted(@ApiParam("Идентификатор проверяемой версии")
                         @PathParam("versionId")
                                 Integer publishedVersionId);

    @POST
    @Path("/refresh/all/byPrimary")
    @ApiOperation("Обновление отображаемых значений ссылок")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void refreshReferencesByPrimary(@ApiParam("Идентификатор старой версии, на которую ссылаются")
                                    @QueryParam("oldVersionId")
                                            Integer oldVersionId,
                                    @ApiParam("Идентификатор новой версии, на которую будут ссылаться")
                                    @QueryParam("newVersionId")
                                            Integer newVersionId);

    @POST
    @Path("/refresh/byPrimary")
    @ApiOperation("Обновление отображаемых значений ссылок версии")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void refreshReferencesByPrimary(@ApiParam("Идентификатор версии, которая ссылается")
                                    @QueryParam("refFromId")
                                            Integer refFromId,
                                    @ApiParam("Идентификатор версии с конфликтами, на которую ссылаются")
                                    @QueryParam("refToId")
                                            Integer refToId,
                                    @ApiParam("Список конфликтов")
                                            List<Conflict> conflicts);

    @POST
    @Path("/discover")
    @ApiOperation("Обнаружение конфликтов")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    void discoverConflicts(@ApiParam("Идентификатор старой версии, на которую ссылаются")
                           @QueryParam("oldVersionId")
                                   Integer oldVersionId,
                           @ApiParam("Идентификатор новой версии, на которую будут ссылаться")
                           @QueryParam("newVersionId")
                                   Integer newVersionId,
                           @ApiParam("Признак обработки разрешимых конфликтов")
                           @QueryParam("processResolvables")
                                   boolean processResolvables);
}
