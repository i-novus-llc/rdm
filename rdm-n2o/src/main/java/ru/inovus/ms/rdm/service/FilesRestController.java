package ru.inovus.ms.rdm.service;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.inovus.ms.rdm.enumeration.FileType;
import ru.inovus.ms.rdm.model.ExportFile;
import ru.inovus.ms.rdm.model.FileModel;
import ru.inovus.ms.rdm.model.audit.AuditAction;
import ru.inovus.ms.rdm.model.audit.AuditLog;
import ru.inovus.ms.rdm.service.api.AuditLogService;
import ru.inovus.ms.rdm.service.api.FileStorageService;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.io.IOException;
import java.time.LocalDateTime;


@RestController
@RequestMapping("/files")
public class FilesRestController {

    private final FileStorageService fileStorageService;
    private final VersionService versionService;
    private final AuditLogService auditLogService;

    @Autowired
    public FilesRestController(FileStorageService fileStorageService, VersionService versionService,
                               AuditLogService auditLogService) {
        this.fileStorageService = fileStorageService;
        this.versionService = versionService;
        this.auditLogService = auditLogService;
    }

    @CrossOrigin(origins = "*")
    @PostMapping(value = "")
    public FileModel uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        String storageFileName = toStorageFileName(file.getOriginalFilename());
        FileModel save = fileStorageService.save(file.getInputStream(), storageFileName);
        save.setName(file.getOriginalFilename());
        return save;
    }

    private String toStorageFileName(String originalFilename) {
        String extension = FilenameUtils.getExtension(originalFilename);
        return System.currentTimeMillis() + "." + extension;
    }

    @CrossOrigin(origins = "*")
    @GetMapping(value = "/{versionId}/{type}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Integer versionId, @PathVariable FileType type) {

        ExportFile versionFile = versionService.getVersionFile(versionId, type);

        saveVersionDownloadAuditLog(versionId);

        return ResponseEntity.ok()
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + versionFile.getFileName() + "\"")
                .body(new InputStreamResource(versionFile.getInputStream()));
    }

    private void saveVersionDownloadAuditLog(Integer versionId) {
        auditLogService.addAction(new AuditLog(null,
                SecurityContextHolder.getContext().getAuthentication().getName(),
                LocalDateTime.now(),
                AuditAction.DOWNLOAD,
                versionService.getById(versionId).getCode()));
    }

}
