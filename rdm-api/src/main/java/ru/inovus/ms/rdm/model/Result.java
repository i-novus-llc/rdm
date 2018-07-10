package ru.inovus.ms.rdm.model;

import java.util.ArrayList;
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

    public Result addResult(Result result) {
        List<String> errors = null;
        if(this.errors == null && result.getErrors() != null) {
            errors = result.getErrors();
        } else if (this.errors != null && result.getErrors() == null) {
            errors = this.errors;
        } else if(this.errors != null && result.getErrors() != null) {
            errors = new ArrayList<>();
            errors.addAll(this.errors);
            errors.addAll(result.getErrors());
        }
        return  new Result(this.successCount + result.getSuccessCount(), this.getAllCount() + result.getAllCount(), errors);
    }

}
