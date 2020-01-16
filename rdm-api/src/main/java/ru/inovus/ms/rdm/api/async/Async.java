package ru.inovus.ms.rdm.api.async;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public final class Async {

    private Async() {throw new UnsupportedOperationException();}

    public enum Operation {

        PUBLICATION(Void.TYPE);

        private final Class<?> resultClass;
        Operation(Class<?> resultClass) {
            this.resultClass = resultClass;
        }

        public Class<?> getResultClass() {
            return resultClass;
        }

        public enum Status {
            QUEUED, IN_PROGRESS, ERROR, DONE
        }

        public static class LogEntry {

            private UUID uuid;
            private Async.Operation operation;
            private Async.Operation.Status status;
            private String error;
            private Map<String, Object> payload;
            private Object result;
            private LocalDateTime tsStartUTC;
            private LocalDateTime tsEndUTC;

            public UUID getUuid() {
                return uuid;
            }

            public void setUuid(UUID uuid) {
                this.uuid = uuid;
            }

            public Operation getOperation() {
                return operation;
            }

            public void setOperation(Operation operation) {
                this.operation = operation;
            }

            public Status getStatus() {
                return status;
            }

            public void setStatus(Status status) {
                this.status = status;
            }

            public String getError() {
                return error;
            }

            public void setError(String error) {
                this.error = error;
            }

            public LocalDateTime getTsStartUTC() {
                return tsStartUTC;
            }

            public void setTsStartUTC(LocalDateTime tsStartUTC) {
                this.tsStartUTC = tsStartUTC;
            }

            public LocalDateTime getTsEndUTC() {
                return tsEndUTC;
            }

            public void setTsEndUTC(LocalDateTime tsEndUTC) {
                this.tsEndUTC = tsEndUTC;
            }

            public Map<String, Object> getPayload() {
                return payload;
            }

            public void setPayload(Map<String, Object> payload) {
                this.payload = payload;
            }

            public Object getResult() {
                return result;
            }

            public void setResult(Object result) {
                this.result = result;
            }

        }

    }

    public static final class PayloadConstants {

        public static final String ARGS_KEY = "args";

        private PayloadConstants() {throw new UnsupportedOperationException();}

    }

}
