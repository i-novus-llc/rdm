package ru.inovus.ms.rdm.api.model.diff;

import java.util.List;

public class PassportDiff {

    private List<PassportAttributeDiff> passportAttributeDiffs;

    public PassportDiff() {
    }

    public PassportDiff(List<PassportAttributeDiff> passportAttributeDiffs) {
        this.passportAttributeDiffs = passportAttributeDiffs;
    }

    public List<PassportAttributeDiff> getPassportAttributeDiffs() {
        return passportAttributeDiffs;
    }

    public void setPassportAttributeDiffs(List<PassportAttributeDiff> passportAttributeDiffs) {
        this.passportAttributeDiffs = passportAttributeDiffs;
    }

}