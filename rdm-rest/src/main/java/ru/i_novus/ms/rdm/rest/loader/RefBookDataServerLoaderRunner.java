package ru.i_novus.ms.rdm.rest.loader;

import io.swagger.annotations.*;
import net.n2oapp.platform.loader.server.BaseLoaderRunner;
import net.n2oapp.platform.loader.server.LoaderDataInfo;
import net.n2oapp.platform.loader.server.ServerLoader;
import net.n2oapp.platform.loader.server.ServerLoaderRestService;
import org.apache.cxf.attachment.AttachmentDataSource;
import org.apache.cxf.attachment.LazyDataSource;
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

import javax.activation.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
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

    @Autowired
    private FileStorageService fileStorageService;

    @Value("${rdm.loader.enabled:true}")
    private boolean loaderEnabled;

    @Autowired
    public RefBookDataServerLoaderRunner(List<ServerLoader> loaders) {
        super(loaders);
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

    /**
     * Формирование запроса на загрузку справочника по полученному содержимому.
     *
     * @param body тело запроса
     * @param info информация о загрузчике
     * @return Запрос на загрузку
     */
    private RefBookDataRequest toRequest(MultipartBody body, LoaderDataInfo<?> info) {

        final List<Attachment> attachments = body.getAllAttachments();
        if (CollectionUtils.isEmpty(attachments))
            return null;

        final RefBookDataRequest request = new RefBookDataRequest();
        request.setPassport(new HashMap<>(1));

        for (Attachment attachment : attachments) {
            parseAttachment(attachment, info, request);
        }

        return request;
    }

    /**
     * Разбор прикрепления и заполнение запроса.
     *
     * @param attachment прикрепление в содержимом
     * @param info       информация о загрузчике
     * @param request    запрос на загрузку
     */
    private void parseAttachment(Attachment attachment, LoaderDataInfo<?> info, RefBookDataRequest request) {

        final String name = attachment.getDataHandler().getName();
        if (name == null)
            return;

        switch (name) {
            case FIELD_CHANGE_SET_ID: {
                final String value = getAttachmentValue(attachment, name, info);
                request.setChangeSetId(value);

                break;
            }
            case FIELD_UPDATE_TYPE: {
                final String value = getAttachmentValue(attachment, name, info);
                request.setUpdateType(RefBookDataUpdateTypeEnum.fromValue(value, CREATE_ONLY));

                break;
            }
            case FIELD_REF_BOOK_CODE: {
                final String value = getAttachmentValue(attachment, name, info);
                request.setCode(value);

                break;
            }
            case FIELD_REF_BOOK_NAME: {
                final String value = getAttachmentValue(attachment, name, info);
                request.getPassport().put("name", value);

                break;
            }
            case FIELD_REF_BOOK_STRUCTURE: {
                final String value = getAttachmentValue(attachment, name, info);
                request.setStructure(value);

                break;
            }
            case FIELD_REF_BOOK_DATA: {
                final String value = getAttachmentValue(attachment, name, info);
                request.setData(value);

                break;
            }
            default: {
                final String fileName = getFileName(attachment);
                if (!StringUtils.isEmpty(fileName)) {
                    final FileModel fileModel = readFile(attachment, fileName, info);
                    request.setFileModel(fileModel);
                }
                break;
            }
        }
    }

    /**
     * Получение значения из обычного прикрепления.
     *
     * @param attachment прикрепление в содержимом
     * @param name       наименование прикрепления
     * @param info       информация о загрузчике
     * @return Значение
     */
    private String getAttachmentValue(Attachment attachment, String name, LoaderDataInfo<?> info) {

        final String objectValue = attachment.getObject(String.class);
        if (objectValue != null)
            return objectValue;

        final String stringValue = readString(attachment, name, info);
        return stringValue != null ? stringValue : "";
    }

    /**
     * Получение значения из потока данных обычного прикрепления.
     *
     * @param attachment прикрепление в содержимом
     * @param name       наименование прикрепления
     * @param info       информация о загрузчике
     * @return Значение
     */
    private String readString(Attachment attachment, String name, LoaderDataInfo<?> info) {
        try {
            final DataSource ds = attachment.getDataHandler().getDataSource();
            final InputStream is = ds.getInputStream();
            if (is == null)
                return null;

            return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines().collect(joining("\n"));

        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Cannot read attachment '%s' for %s", name, info.getTarget()), e);
        }
    }

    /**
     * Получение наименования файла из прикрепления.
     *
     * @param attachment прикрепление в содержимом
     * @return Наименование файла
     */
    private String getFileName(Attachment attachment) {

        final List<String> contentDispositionHeaders = attachment.getHeaderAsList(HttpHeaders.CONTENT_DISPOSITION);
        if (CollectionUtils.isEmpty(contentDispositionHeaders)
                || StringUtils.isEmpty(contentDispositionHeaders.get(0))) {
            return null;
        }

        final ContentDisposition contentDisposition = new ContentDisposition(contentDispositionHeaders.get(0));
        return contentDisposition.getFilename();
    }

    /**
     * Получение файла из прикрепления с сохранением в хранилище.
     *
     * @param attachment прикрепление в содержимом
     * @param fileName   наименование файла
     * @param info       информация о загрузчике
     * @return Модель файла
     */
    private FileModel readFile(Attachment attachment, String fileName, LoaderDataInfo<?> info) {
        try {
            final DataSource ds = attachment.getDataHandler().getDataSource();
            final InputStream is = getAttachmentDataSource(ds).getInputStream();
            return fileStorageService.save(is, fileName);

        } catch (InternalServerErrorException e) {
            throw new IllegalArgumentException(String.format("Cannot read attachment '%s' for %s", fileName, info.getTarget()), e);
        }
    }

    protected AttachmentDataSource getAttachmentDataSource(final DataSource dataSource) {

        if (dataSource == null)
            return null;

        final DataSource ds = (dataSource instanceof LazyDataSource)
                ? ((LazyDataSource) dataSource).getDataSource()
                : dataSource;

        return (ds instanceof AttachmentDataSource) ? (AttachmentDataSource) ds : null;
    }
}
