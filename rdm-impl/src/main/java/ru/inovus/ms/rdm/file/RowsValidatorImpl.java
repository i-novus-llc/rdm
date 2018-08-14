package ru.inovus.ms.rdm.file;

import net.n2oapp.platform.i18n.Message;
import ru.inovus.ms.rdm.model.Result;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.service.api.VersionService;
import ru.inovus.ms.rdm.validation.ErrorAttributeHolderValidator;
import ru.inovus.ms.rdm.validation.ReferenceValueValidation;
import ru.inovus.ms.rdm.validation.RequiredValidation;
import ru.inovus.ms.rdm.validation.TypeValidation;

import java.util.*;

import static org.apache.cxf.common.util.CollectionUtils.isEmpty;

public class RowsValidatorImpl implements RowsValidator {

    private Result result = new Result(0, 0, null);

    private Structure structure;

    private VersionService versionService;

    public RowsValidatorImpl(VersionService versionService, Structure structure) {
        this.versionService = versionService;
        this.structure = structure;
    }

    @Override
    public Result append(Row row) {
        List<Message> errors = new ArrayList<>();
        Set<String> errorAttributes = new HashSet<>();

        List<ErrorAttributeHolderValidator> validators = Arrays.asList(
                new RequiredValidation(row, structure),
                new TypeValidation(row.getData(), structure),
                new ReferenceValueValidation(versionService, row, structure)
        );

        for (ErrorAttributeHolderValidator validator : validators){
            validator.setErrorAttributes(errorAttributes);
            errors.addAll(validator.validate());
            errorAttributes.addAll(validator.getErrorAttributes());
        }



        if(isEmpty(errors)){
            this.result = this.result.addResult(new Result(1, 1, null));
        } else {
            this.result = this.result.addResult(new Result(0, 1, errors));
        }
        return this.result;
    }


    @Override
    public Result process() {
        return this.result;
    }
}
