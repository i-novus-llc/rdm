package ru.i_novus.ms.rdm.n2o.service;

import net.n2oapp.platform.i18n.Message;
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
import ru.i_novus.ms.rdm.api.exception.FileException;
import ru.i_novus.ms.rdm.api.model.ExportFile;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.api.service.FileStorageService;
import ru.i_novus.ms.rdm.api.util.FileUtils;
import ru.i_novus.ms.rdm.api.util.StringUtils;

import java.io.IOException;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static ru.i_novus.ms.rdm.api.exception.FileException.*;
import static ru.i_novus.ms.rdm.api.util.FileUtils.getRefBookFileExtension;

@RestController
@RequestMapping("/files")
@SuppressWarnings("unused")
public class FilesRestController {

    private static final String FILE_IS_TOO_BIG_EXCEPTION_CODE = "file.is.too.big";

    private static final long KILOBYTE = 1024;
    private static final List<String> REFBOOK_EXTENSIONS = singletonList("XML");
    private static final List<String> DRAFT_EXTENSIONS = List.of("XML", "XSLX");
    private static final List<String> REFDATA_EXTENSIONS = List.of("XML", "XSLX");

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
    @PostMapping(value = "/refbook")
    public FileModel uploadRefBook(@RequestParam("file") MultipartFile file) throws IOException {

        return uploadFile(file, REFBOOK_EXTENSIONS);
    }

    @CrossOrigin(origins = "*")
    @PostMapping(value = "/draft")
    public FileModel uploadRefDraft(@RequestParam("file") MultipartFile file) throws IOException {

        return uploadFile(file, DRAFT_EXTENSIONS);
    }

    @CrossOrigin(origins = "*")
    @PostMapping(value = "/refdata")
    public FileModel uploadRefData(@RequestParam("file") MultipartFile file) throws IOException {

        return uploadFile(file, REFDATA_EXTENSIONS);
    }

    private FileModel uploadFile(MultipartFile file, List<String> allowExtensions) throws IOException {

        validate(file);

        final String originalFilename = file.getOriginalFilename();
        final String fileExtension = getRefBookFileExtension(originalFilename);
        if (!allowExtensions.contains(fileExtension))
            throw newInvalidFileExtensionException(fileExtension);

        final String storageFileName = toStorageFileName(fileExtension);

        final FileModel save = fileStorageService.save(file.getInputStream(), storageFileName);
        save.setName(originalFilename);

        return save;
    }

    private void validate(MultipartFile file) {

        if (toMbSize(file.getSize()) > maxFileSizeMb)
            throw new FileException(new Message(FILE_IS_TOO_BIG_EXCEPTION_CODE, maxFileSizeMb));

        final String filename = file.getOriginalFilename();
        if (StringUtils.isEmpty(filename))
            throw newAbsentFileNameException();

        final List<String> extensions = FileUtils.getExtensions(filename);
        validateExtensions(filename, extensions);
    }

    private long toMbSize(long size) {

        return size / KILOBYTE / KILOBYTE;
    }

    private void validateExtensions(String originalFilename, List<String> extensions) {

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
