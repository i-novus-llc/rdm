package ru.inovus.ms.rdm.model;

import java.util.List;

public class Result {

    private int successCount;

    private int allCount;

    private List<String> errors;

    public Result(int successCount, int allCount, List<String> errors) {
        this.successCount = successCount;
        this.allCount = allCount;
        this.errors = errors;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getAllCount() {
        return allCount;
    }

    public List<String> getErrors() {
        return errors;
    }

    void append(Result anotherResult) {

    }
}
