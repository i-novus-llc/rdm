package ru.inovus.ms.rdm.model;

import java.util.Map;

public class UiRefBookPublish extends RefBook {

    private Map<String, String> conflictReferrerNames;

    public UiRefBookPublish(RefBook refBook) {
        super(refBook);
    }

    public Map<String, String> getConflictReferrerNames() {
        return conflictReferrerNames;
    }

    public void setConflictReferrerNames(Map<String, String> conflictReferrerNames) {
        this.conflictReferrerNames = conflictReferrerNames;
    }
}
