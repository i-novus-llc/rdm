package ru.inovus.ms.rdm.api.model;

import net.n2oapp.platform.i18n.Message;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

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
        LinkedHashSet<Message> totalErrors = new LinkedHashSet<>();
        if(this.errors == null && result.getErrors() != null) {
            totalErrors.addAll(result.getErrors());
        } else if (this.errors != null && result.getErrors() == null) {
            totalErrors.addAll(this.errors);
        } else if(this.errors != null && result.getErrors() != null) {
            totalErrors.addAll(this.errors);
            totalErrors.addAll(result.getErrors());
        }

        return new Result(this.successCount + result.getSuccessCount(), this.getAllCount() + result.getAllCount(), new ArrayList<>(totalErrors));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Result result = (Result) o;

        if (successCount != result.successCount) return false;
        if (allCount != result.allCount) return false;
        return Objects.equals(errors, result.errors);
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
