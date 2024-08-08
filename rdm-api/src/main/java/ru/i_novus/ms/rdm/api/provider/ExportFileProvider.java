package ru.i_novus.ms.rdm.api.provider;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import org.apache.commons.io.IOUtils;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.api.model.ExportFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

@Provider
@Produces("application/zip")
public class ExportFileProvider implements MessageBodyWriter<ExportFile>, MessageBodyReader<ExportFile> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public long getSize(ExportFile exportFile, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return 0;
    }

    @Override
    public void writeTo(ExportFile exportFile, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException {
        try (InputStream inputStream = exportFile.getInputStream();
             OutputStream outputStream = entityStream) {
            httpHeaders.put("Content-Disposition", Collections.singletonList("attachment; filename=" + exportFile.getFileName()));
            IOUtils.copy(inputStream, outputStream);
        }
    }


    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public ExportFile readFrom(Class<ExportFile> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException {

        String name = null;
        try {
            name = null;
            List<String> contentDisposition = httpHeaders.get("Content-Disposition");
            String[] parts = new String[0];
            if (contentDisposition != null && !contentDisposition.isEmpty())
                parts = contentDisposition.get(0).split("filename[\\s]*=[\\s]*");
            if (parts.length > 1)
                name = parts[1].split(";")[0];
            return new ExportFile(entityStream, name);
        } catch (Exception e) {
            entityStream.close();
            throw new RdmException(e);
        }
    }
}
