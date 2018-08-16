package ru.inovus.ms.rdm.file;

import net.n2oapp.platform.i18n.Message;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.inovus.ms.rdm.model.Result;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.service.api.VersionService;
import ru.inovus.ms.rdm.validation.*;

import java.util.*;

import static org.apache.cxf.common.util.CollectionUtils.isEmpty;

public class RowsValidatorImpl implements RowsValidator {

    private Result result = new Result(0, 0, null);

    private Structure structure;

    private VersionService versionService;

    private SearchDataService searchDataService;

    String storageCode;


    public RowsValidatorImpl(VersionService versionService, SearchDataService searchDataService, Structure structure, String storageCode) {
        this.versionService = versionService;
        this.structure = structure;
        this.searchDataService = searchDataService;
        this.storageCode = storageCode;
    }

    @Override
    public Result append(Row row) {
        List<Message> errors = new ArrayList<>();
        Set<String> errorAttributes = new HashSet<>();

        List<ErrorAttributeHolderValidation> validators = Arrays.asList(
                new RequiredValidation(row, structure),
                new TypeValidation(row.getData(), structure),
                new ReferenceValueValidation(versionService, row, structure),
                new DBPrimaryKeyValidation(searchDataService, structure, row, storageCode)
        );

        for (ErrorAttributeHolderValidation validator : validators){
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
