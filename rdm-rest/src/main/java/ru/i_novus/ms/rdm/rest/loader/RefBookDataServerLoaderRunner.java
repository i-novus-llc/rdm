package ru.i_novus.ms.rdm.rest.loader;

import io.swagger.annotations.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import net.n2oapp.platform.loader.server.BaseLoaderRunner;
import net.n2oapp.platform.loader.server.LoaderDataInfo;
import net.n2oapp.platform.loader.server.ServerLoader;
import net.n2oapp.platform.loader.server.ServerLoaderRestService;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.model.loader.RefBookDataRequest;
import ru.i_novus.ms.rdm.api.model.loader.RefBookDataUpdateTypeEnum;
import ru.i_novus.ms.rdm.api.service.FileStorageService;
import ru.i_novus.ms.rdm.api.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

import static java.util.stream.Collectors.joining;
import static ru.i_novus.ms.rdm.api.model.loader.RefBookDataUpdateTypeEnum.CREATE_ONLY;
import static ru.i_novus.ms.rdm.api.util.loader.RefBookDataConstants.*;

/**
 * Запускатель загрузчиков справочников RDM.
 */
@Service
@Api("Загрузчики справочников")
@SuppressWarnings({"rawtypes", "java:S3740"})
public class RefBookDataServerLoaderRunner extends BaseLoaderRunner implements ServerLoaderRestService {

    private final FileStorageService fileStorageService;

    @Value("${rdm.loader.enabled:true}")
    private boolean loaderEnabled;

    @Autowired
    public RefBookDataServerLoaderRunner(List<ServerLoader> loaders,
                                         FileStorageService fileStorageService) {
        super(loaders);

        this.fileStorageService = fileStorageService;
    }

    @POST
    @Path("/json/{subject}/{target}")
    @ApiOperation("Загрузить json-данные")
    @ApiResponse(code = 200, message = "Данные загружены без ошибок")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public void run(@ApiParam("Владелец данных") @PathParam("subject") String subject,
                    @ApiParam("Вид данных") @PathParam("target") String target,
                    InputStream body) {
        super.run(subject, target, body);
    }

    @Override
    protected List<Object> read(InputStream body, LoaderDataInfo<?> info) {

        throw new IllegalArgumentException(String.format("Unsupported format for %s", info.getTarget()));
    }

    @POST
    @Path("/{subject}/{target}")
    @ApiOperation("Загрузить справочник")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Справочник загружен без ошибок"),
            @ApiResponse(code = 400, message = "Некорректный запрос"),
            @ApiResponse(code = 404, message = "Нет справочника")
    })
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public void run(@ApiParam("Владелец данных") @PathParam("subject") String subject,
                    @ApiParam("Вид данных") @PathParam("target") String target,
                    @ApiParam("Содержимое") MultipartBody body) {
        if (!loaderEnabled)
            return;

        final ServerLoader loader = find(target);
        final RefBookDataRequest request = toRequest(body, loader);
        if (request == null)
            return;

        execute(subject, List.of(request), loader);
    }

    /** Формирование запроса на загрузку справочника по полученному содержимому. */
    private RefBookDataRequest toRequest(MultipartBody body, LoaderDataInfo<?> info) {

        if (CollectionUtils.isEmpty(body.getAllAttachments()))
            return null;

        final RefBookDataRequest request = new RefBookDataRequest();
        request.setPassport(new HashMap<>());

        for (Attachment attachment : body.getAllAttachments()) {
            parseAttachment(attachment, info, request);
        }

        return request;
    }

    /** Разбор прикрепления и заполнение запроса. */
    private void parseAttachment(Attachment attachment, LoaderDataInfo<?> info, RefBookDataRequest request) {

        final String name = attachment.getDataHandler().getDataSource().getName();

        String value = attachment.getObject(String.class);
        if (value == null) {
            value = readString(attachment, name, info);
            if (value == null) value = "";
        }

        if (FIELD_CHANGE_SET_ID.equals(name)) {
            request.setChangeSetId(value);

        } else if (FIELD_UPDATE_TYPE.equals(name)) {
            request.setUpdateType(RefBookDataUpdateTypeEnum.fromValue(value, CREATE_ONLY));

        } else if (FIELD_REF_BOOK_CODE.equals(name)) {
            request.setCode(value);

        } else if (FIELD_REF_BOOK_NAME.equals(name)) {
            request.getPassport().put("name", value);

        } else if (FIELD_REF_BOOK_STRUCTURE.equals(name)) {
            request.setStructure(value);

        } else if (FIELD_REF_BOOK_DATA.equals(name)) {
            request.setData(value);

        } else {
            final String fileName = getFileName(attachment);
            if (!StringUtils.isEmpty(fileName)) {
                final FileModel fileModel = readFile(attachment, fileName, info);
                request.setFileModel(fileModel);
            }
        }
    }

    private String getFileName(Attachment attachment) {

        final List<String> contentDispositionHeaders = attachment.getHeaderAsList(HttpHeaders.CONTENT_DISPOSITION);
        if (CollectionUtils.isEmpty(contentDispositionHeaders)
                || StringUtils.isEmpty(contentDispositionHeaders.get(0))) {
            return null;
        }

        final ContentDisposition contentDisposition = new ContentDisposition(contentDispositionHeaders.get(0));
        return contentDisposition.getFilename();
    }
    
    private FileModel readFile(Attachment attachment, String fileName, LoaderDataInfo<?> info) {
        try {
            return fileStorageService.save(attachment.getDataHandler().getDataSource().getInputStream(), fileName);

        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Cannot read attachment '%s' for %s", fileName, info.getTarget()), e);
        }
    }

    private String readString(Attachment attachment, String name, LoaderDataInfo<?> info) {
        try {
            final InputStream inputStream = attachment.getDataHandler().getDataSource().getInputStream();
            if (inputStream == null)
                return null;

            return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines().collect(joining("\n"));

        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Cannot read attachment '%s' for %s", name, info.getTarget()), e);
        }
    }
}
