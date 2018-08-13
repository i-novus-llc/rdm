package ru.inovus.ms.rdm.model;


import net.n2oapp.platform.i18n.Message;

import java.util.ArrayList;
import java.util.List;

public class Result {

    private int successCount;

    private int allCount;

    private List<Message> errors;

    public Result(int successCount, int allCount, List<Message> errors) {
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

    public List<Message> getErrors() {
        return errors;
    }

    public Result addResult(Result result) {
        List<Message> totalErrors = null;
        if(this.errors == null && result.getErrors() != null) {
            totalErrors = result.getErrors();
        } else if (this.errors != null && result.getErrors() == null) {
            totalErrors = this.errors;
        } else if(this.errors != null && result.getErrors() != null) {
            totalErrors = new ArrayList<>();
            totalErrors.addAll(this.errors);
            totalErrors.addAll(result.getErrors());
        }

        return  new Result(this.successCount + result.getSuccessCount(), this.getAllCount() + result.getAllCount(), totalErrors);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Result result = (Result) o;

        if (successCount != result.successCount) return false;
        if (allCount != result.allCount) return false;
        return errors != null ? errors.equals(result.errors) : result.errors == null;
    }

    @Override
    public int hashCode() {
        int result = successCount;
        result = 31 * result + allCount;
        result = 31 * result + (errors != null ? errors.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Result{");
        sb.append("successCount=").append(successCount);
        sb.append(", allCount=").append(allCount);
        sb.append(", errors=").append(errors);
        sb.append('}');
        return sb.toString();
    }
}
