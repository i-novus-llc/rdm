package ru.inovus.ms.rdm.file;

import ru.inovus.ms.rdm.model.Result;

import java.util.Map;
import java.util.function.Consumer;

import static org.apache.commons.collections4.MapUtils.isEmpty;

public class CreateDraftPassportPersister implements PassportProcessor {

    private Consumer draftCreator;

    public CreateDraftPassportPersister(Consumer<Map<String, String>> draftCreator) {
        this.draftCreator = draftCreator;
    }

    @Override
    public Result append(Map<String, String> passport) {
        if (isEmpty(passport))
            return new Result(0, 0, null);

        draftCreator.accept(passport);

        return new Result(passport.size(), passport.size(), null);
    }

}
