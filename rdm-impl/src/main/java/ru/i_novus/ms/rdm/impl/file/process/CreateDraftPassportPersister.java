package ru.i_novus.ms.rdm.impl.file.process;

import java.util.Map;
import java.util.function.Consumer;

import static org.apache.commons.collections4.MapUtils.isEmpty;

public class CreateDraftPassportPersister implements PassportProcessor {

    private Consumer draftCreator;

    public CreateDraftPassportPersister(Consumer<Map<String, String>> draftCreator) {
        this.draftCreator = draftCreator;
    }

    @Override
    public void process(Map<String, String> passport) {
        if (isEmpty(passport))
            return;

        draftCreator.accept(passport);
    }

}
