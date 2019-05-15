package ru.inovus.ms.rdm.file.process;

import java.util.Map;

public interface RefBookProcessor {

    void process(ProcessRequest request);

    public static class ProcessRequest {

        private String code;

        private String category;

        private Map<String, String> passport;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public Map<String, String> getPassport() {
            return passport;
        }

        public void setPassport(Map<String, String> passport) {
            this.passport = passport;
        }
    }

}
