package ru.i_novus.ms.rdm.n2o.service;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.model.ExportFile;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.api.service.FileStorageService;
import ru.i_novus.ms.rdm.api.util.FileUtils;
import ru.i_novus.ms.rdm.api.util.StringUtils;

import java.io.IOException;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNumeric;
import static ru.i_novus.ms.rdm.api.exception.FileExtensionException.newAbsentFileExtensionException;
import static ru.i_novus.ms.rdm.api.exception.FileExtensionException.newInvalidFileExtensionException;

@RestController
@RequestMapping("/files")
@SuppressWarnings("unused")
public class FilesRestController {

    private static final String FILE_IS_TOO_BIG_EXCEPTION_CODE = "file.is.too.big";

    private static final long KILOBYTE = 1024;

    private final FileStorageService fileStorageService;

    private final VersionRestService versionService;

    private final int maxFileSizeMb;

    @Autowired
    public FilesRestController(FileStorageService fileStorageService,
                               VersionRestService versionService,
                               @Value("${rdm.max-file-size-mb:55}") int maxFileSizeMb) {

        this.fileStorageService = fileStorageService;

        this.versionService = versionService;

        this.maxFileSizeMb = maxFileSizeMb;
    }

    @CrossOrigin(origins = "*")
    @PostMapping(value = "")
    public FileModel uploadFile(@RequestParam("file") MultipartFile file) throws IOException {

        if (toMbSize(file.getSize()) > maxFileSizeMb)
            throw new UserException(new Message(FILE_IS_TOO_BIG_EXCEPTION_CODE, maxFileSizeMb));

        final String originalFilename = file.getOriginalFilename();
        final List<String> extensions = FileUtils.getExtensions(originalFilename);
        validateFileName(originalFilename, extensions);

        final String storageFileName = toStorageFileName(extensions.get(0));

        final FileModel save = fileStorageService.save(file.getInputStream(), storageFileName);
        save.setName(originalFilename);

        return save;
    }

    private long toMbSize(long size) {

        return size / KILOBYTE / KILOBYTE;
    }

    private void validateFileName(String originalFilename, List<String> extensions) {

        if (CollectionUtils.isEmpty(extensions))
            throw newAbsentFileExtensionException(originalFilename);

        extensions.stream()
                .filter(this::isInvalidExtension)
                .findAny()
                .ifPresent(extension -> { throw newInvalidFileExtensionException(extension); });

        final String fileExtension = extensions.get(0);
        if (StringUtils.isEmpty(fileExtension))
            throw newAbsentFileExtensionException(originalFilename);
    }

    private boolean isInvalidExtension(String extension) {

        return !StringUtils.isEmpty(extension) &&
                !isNumeric(extension); // Допустим только номер версии
    }

    private String toStorageFileName(String extension) {

        return System.currentTimeMillis() + "." + extension;
    }

    @CrossOrigin(origins = "*")
    @GetMapping(value = "/{versionId}/{type}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Integer versionId,
                                                 @PathVariable FileType type) {

        ExportFile versionFile = versionService.getVersionFile(versionId, type);

        return ResponseEntity.ok()
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + versionFile.getFileName() + "\"")
                .body(new InputStreamResource(versionFile.getInputStream()));
    }
}
