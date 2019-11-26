package ru.inovus.ms.rdm.impl.file.export;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import ru.inovus.ms.rdm.impl.entity.AttributeValidationEntity;
import ru.inovus.ms.rdm.api.enumeration.FileType;
import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.api.model.version.RefBookVersion;
import ru.inovus.ms.rdm.api.model.refdata.Row;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.validation.AttributeValidation;
import ru.inovus.ms.rdm.impl.repository.AttributeValidationRepository;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

/**
 * Created by znurgaliev on 08.08.2018.
 */
@Component
public class PerRowFileGeneratorFactory {

    private AttributeValidationRepository attributeValidationRepository;

    @Autowired
    public PerRowFileGeneratorFactory(AttributeValidationRepository attributeValidationRepository) {
        this.attributeValidationRepository = attributeValidationRepository;
    }

    @Transactional(readOnly = true)
    public PerRowFileGenerator getFileGenerator(Iterator<Row> rowIterator, RefBookVersion version, FileType fileType) {

        if (FileType.XLSX.equals(fileType))
            return new XlsFileGenerator(rowIterator, version.getStructure());

        if (FileType.XML.equals(fileType)) {
            Map<String, Structure.Reference> attributeToReferenceMap = null;
            if (!CollectionUtils.isEmpty(version.getStructure().getReferences())) {
                attributeToReferenceMap = version.getStructure().getReferences().stream()
                        .collect(Collectors.toMap(
                                        Structure.Reference::getAttribute,
                                        reference -> reference)
                        );
            }
            final List<AttributeValidation> attributeValidations = attributeValidationRepository
                    .findAllByVersionId(version.getId()).stream()
                    .map(AttributeValidationEntity::attributeValidationModel)
                    .collect(Collectors.toList());
            return new XmlFileGenerator(rowIterator, version, attributeToReferenceMap, attributeValidations);
        }

        throw new RdmException("no generator for " + fileType + " type");
    }

    public static List<FileType> getAvailableTypes() {
        return asList(FileType.XLSX, FileType.XML);
    }
}

