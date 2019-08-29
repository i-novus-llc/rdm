package ru.inovus.ms.rdm.model;

import ru.inovus.ms.rdm.model.refbook.RefBook;

import java.util.Map;

public class UiRefBookPublish extends RefBook {

    private Map<String, String> conflictingReferrerNames;

    public UiRefBookPublish(RefBook refBook) {
        super(refBook);
    }

    public Map<String, String> getConflictingReferrerNames() {
        return conflictingReferrerNames;
    }

    public void setConflictingReferrerNames(Map<String, String> conflictingReferrerNames) {
        this.conflictingReferrerNames = conflictingReferrerNames;
    }
}
