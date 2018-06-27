package ru.inovus.ms.rdm.service;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.data.domain.Page;
import ru.inovus.ms.rdm.model.RefBook;
import ru.inovus.ms.rdm.model.RefBookCreateRequest;
import ru.inovus.ms.rdm.model.RefBookCriteria;
import ru.inovus.ms.rdm.model.RefBookVersion;

import javax.ws.rs.*;

@Path("/refbook")
@Produces("application/json")
@Consumes("application/json")
@Api("Методы работы со справочниками")
public interface RefBookService {

    @GET
    @ApiOperation("Получения списка справочников, с фильтрацией")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    Page<RefBook> search(@BeanParam RefBookCriteria refBookCriteria);

    @POST
    @ApiOperation("Создание нового справочника")
    RefBook create(@BeanParam RefBookCreateRequest refBookCreateRequest);

    Page<RefBookVersion> getVersions(String refBookId);

}
